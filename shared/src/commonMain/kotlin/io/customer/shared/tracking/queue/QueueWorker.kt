package io.customer.shared.tracking.queue

import io.customer.shared.database.TrackingTask
import io.customer.shared.database.TrackingTaskQueryHelper
import io.customer.shared.sdk.config.BackgroundQueueConfig
import io.customer.shared.tracking.api.*
import io.customer.shared.tracking.api.model.*
import io.customer.shared.tracking.constant.Priority
import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.util.DateTimeUtil
import io.customer.shared.util.Logger
import io.customer.shared.work.CoroutineExecutable
import io.customer.shared.work.CoroutineExecutor
import io.customer.shared.work.runSuspended
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Instant
import kotlin.time.DurationUnit

/**
 * Class responsible for running queue operations. This class only receives requests to run the
 * queue and is responsible for following:
 *
 * - whether to run the queue now or wait (except when specifically asked to run).
 * - maintain lock to avoid sending same tasks multiple times.
 * - validate and update task status related to queue operations.
 */
internal interface QueueWorker {
    suspend fun checkForPendingTasks(source: QueueTriggerSource)
    suspend fun sendAllPending()
}

internal class QueueWorkerImpl(
    private val logger: Logger,
    private val dateTimeUtil: DateTimeUtil,
    override val executor: CoroutineExecutor,
    private val backgroundQueueConfig: BackgroundQueueConfig,
    private val trackingTaskQueryHelper: TrackingTaskQueryHelper,
    private val queueRunner: QueueRunner,
) : QueueWorker, CoroutineExecutable {
    private val mutex = Mutex()

    private suspend fun acquireLock() {
        logger.debug("Acquiring lock for queue")
        val result = kotlin.runCatching {
            mutex.lock(owner = this)
        }
        result.onFailure { ex ->
            logger.error("Mutex locking failed, reason: $ex")
        }
    }

    private fun releaseLock() {
        logger.debug("Releasing lock for queue")
        val result = kotlin.runCatching {
            mutex.unlock()
        }
        result.onFailure { ex ->
            logger.error("Mutex unlocking failed, reason: $ex")
        }
    }

    override fun onCoroutineFailed(exception: Throwable) {
        releaseLock()
    }

    override suspend fun checkForPendingTasks(source: QueueTriggerSource) {
        processBatchTasks(isRecursive = false) { getTasksToBatch() }
    }

    override suspend fun sendAllPending() {
        processBatchTasks(isRecursive = true) { trackingTaskQueryHelper.selectAllPendingTasks().getOrNull() }
    }

    private fun processBatchTasks(
        isRecursive: Boolean,
        getTasks: suspend () -> List<TrackingTask>?,
    ) {
        runSuspended {
            if (mutex.isLocked) return@runSuspended

            acquireLock()
            val result = kotlin.runCatching {
                do {
                    val pendingTasks = getTasks()
                    val isSuccess: Boolean = if (!pendingTasks.isNullOrEmpty()) {
                        queueRunner.runQueueForTasks(pendingTasks).getOrNull() == true
                    } else false
                } while (isRecursive && isSuccess)
            }
            result.onFailure { ex ->
                logger.error("Validation for pending tasks failed with error: ${ex.message}")
            }
            releaseLock()
        }
    }

    private fun canTriggerBatch(task: TrackingTask, timestamp: Instant): Boolean {
        val timeInQueue = timestamp.minus(task.createdAt).toLong(DurationUnit.SECONDS)
        return isCountedAsBatchTrigger(task) && (
                timeInQueue >= backgroundQueueConfig.batchDelayMaxDelayInSeconds
                        || task.priority > Priority.DEFAULT)
    }

    private fun isCountedAsBatchTrigger(task: TrackingTask): Boolean {
        return task.retryCount <= 0 || task.statusCode == null
    }

    @Throws(Exception::class)
    private suspend fun getTasksToBatch(): List<TrackingTask>? {
        val pendingTasks = trackingTaskQueryHelper.selectAllPendingTasks().getOrNull()
        if (pendingTasks.isNullOrEmpty()) return null

        var triggerBatch = false
        var tasksCount = 0
        val currentTime = dateTimeUtil.now
        pendingTasks.forEach { task ->
            triggerBatch = triggerBatch || canTriggerBatch(task = task, timestamp = currentTime)
            tasksCount += if (isCountedAsBatchTrigger(task = task)) 1 else 0
        }
        triggerBatch = triggerBatch || tasksCount >= backgroundQueueConfig.batchDelayMinTasks
        logger.debug("Checking tasks: ${pendingTasks.size}, batching :$triggerBatch")

        if (triggerBatch) {
            trackingTaskQueryHelper.updateTasksStatus(
                status = QueueTaskStatus.SENDING,
                tasks = pendingTasks,
            )
        } else {
            // queue next
            trackingTaskQueryHelper.updateTasksStatus(
                status = QueueTaskStatus.PENDING,
                tasks = pendingTasks,
            )
        }
        return pendingTasks.takeIf { triggerBatch }
    }
}
