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
import io.customer.shared.work.*
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
    override val executor: JobExecutor,
    private val backgroundQueueConfig: BackgroundQueueConfig,
    private val trackingTaskQueryHelper: TrackingTaskQueryHelper,
    private val queueRunner: QueueRunner,
    private val queueTimer: QueueTimer,
) : QueueWorker, JobDispatcher {
    private val mutex = Mutex()

    private suspend fun acquireLock() {
        logger.debug("Acquiring lock for queue")
        kotlin.runCatching {
            mutex.lock(owner = this)
        }.onFailure { ex ->
            logger.error("Mutex locking failed, reason: $ex")
        }
    }

    private fun releaseLock() {
        logger.debug("Releasing lock for queue")
        kotlin.runCatching {
            mutex.unlock()
        }.onFailure { ex ->
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
        processBatchTasks(isRecursive = true) {
            trackingTaskQueryHelper.selectAllPendingTasks().getOrNull()
        }
    }

    private fun processBatchTasks(
        isRecursive: Boolean,
        getTasks: suspend () -> List<TrackingTask>?,
    ) = runOnBackground {
        if (mutex.isLocked) return@runOnBackground

        acquireLock()
        kotlin.runCatching {
            queueTimer.cancel()
        }.onFailure { ex ->
            logger.error("Cannot cancel scheduled timer error: ${ex.message}")
        }
        kotlin.runCatching {
            do {
                val pendingTasks = getTasks()
                val isSuccess: Boolean = if (!pendingTasks.isNullOrEmpty()) {
                    queueRunner.runQueueForTasks(pendingTasks).getOrNull() == true
                } else false
            } while (isRecursive && isSuccess)
        }.onFailure { ex ->
            logger.error("Validation for pending tasks failed with error: ${ex.message}")
        }
        kotlin.runCatching {
            queueTimer.schedule(
                force = true,
                duration = TimeUnit.Seconds(backgroundQueueConfig.batchDelayMaxDelayInSeconds.toDouble()),
            ) { checkForPendingTasks(QueueTriggerSource.TIMER) }
        }.onFailure { ex ->
            logger.error("Cannot schedule timer error: ${ex.message}")
        }
        releaseLock()
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
