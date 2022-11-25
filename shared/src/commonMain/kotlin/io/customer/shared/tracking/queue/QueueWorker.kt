package io.customer.shared.tracking.queue

import io.customer.shared.database.QueryHelper
import io.customer.shared.sdk.config.BackgroundQueueConfig
import io.customer.shared.tracking.api.*
import io.customer.shared.tracking.api.model.*
import io.customer.shared.tracking.constant.Priority
import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.tracking.model.TaskResponse
import io.customer.shared.util.DateTimeUtil
import io.customer.shared.util.JsonAdapter
import io.customer.shared.util.Logger
import io.customer.shared.util.parseToActivity
import io.customer.shared.work.CoroutineExecutable
import io.customer.shared.work.CoroutineExecutor
import io.customer.shared.work.runSuspended
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Instant
import local.TrackingTask
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
    fun checkForPendingTasks(source: QueueTriggerSource)
    fun sendAllPending()
}

internal class QueueWorkerImpl(
    private val logger: Logger,
    private val dateTimeUtil: DateTimeUtil,
    private val jsonAdapter: JsonAdapter,
    override val executor: CoroutineExecutor,
    private val backgroundQueueConfig: BackgroundQueueConfig,
    private val queryHelper: QueryHelper,
    private val trackingHttpClient: TrackingHttpClient,
) : QueueWorker, CoroutineExecutable {
    private val mutex = Mutex()

    private fun TrackingTask.canTriggerBatch(timestamp: Instant): Boolean {
        val timeInQueue = timestamp.minus(createdAt).toLong(DurationUnit.SECONDS)
        return isCountedAsBatchTrigger() && (priority < Priority.DEFAULT || timeInQueue >= backgroundQueueConfig.batchDelayMaxDelayInSeconds)
    }

    private fun TrackingTask.isCountedAsBatchTrigger(): Boolean {
        return retryCount <= 0
    }

    private suspend fun acquireLock() {
        logger.debug("Acquiring lock for queue")
        val result = kotlin.runCatching {
            mutex.lock(this)
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

    override fun checkForPendingTasks(source: QueueTriggerSource) {
        runSuspended {
            if (mutex.isLocked) return@runSuspended

            // TODO try without mutex using database lock
            acquireLock()
            val pendingTasks = queryHelper.selectAllPendingTasks()
            if (!pendingTasks.isNullOrEmpty()) {
                val result = kotlin.runCatching { runQueueForTasks(pendingTasks) }
                result.onFailure { ex ->
                    logger.error("Validation for pending tasks failed with error: ${ex.message}")
                }
            }
            releaseLock()
        }
    }

    private fun runQueueForTasks(pendingTasks: List<TrackingTask>) {
        runSuspended {
            acquireLock()
            val result = kotlin.runCatching { validateTasksStatus(pendingTasks) }
            result.onFailure { ex ->
                logger.error("Failed to run queue for ${pendingTasks.size} tasks with error: ${ex.message}")
            }
            releaseLock()
        }
    }

    @Throws(Exception::class)
    private suspend fun validateTasksStatus(pendingTasks: List<TrackingTask>) {
        var triggerBatch = false
        var tasksCount = 0
        val currentTime = dateTimeUtil.now
        pendingTasks.forEach { activityLog ->
            triggerBatch = triggerBatch || activityLog.canTriggerBatch(timestamp = currentTime)
            tasksCount += if (activityLog.isCountedAsBatchTrigger()) 1 else 0
        }
        triggerBatch = triggerBatch || tasksCount >= backgroundQueueConfig.batchDelayMinTasks
        logger.debug("Checking tasks: ${pendingTasks.size}, batching :$triggerBatch")
        if (triggerBatch) {
            queryHelper.updateTasksStatus(status = QueueTaskStatus.SENDING, tasks = pendingTasks)
            batchTasks(pendingTasks = pendingTasks)
        } else {
            // queue next
            queryHelper.updateTasksStatus(status = QueueTaskStatus.PENDING, tasks = pendingTasks)
        }
    }

    override fun sendAllPending() {
        runSuspended {

        }
    }

    private suspend fun batchTasks(pendingTasks: List<TrackingTask>) {
        val activities = pendingTasks.map { task -> jsonAdapter.parseToActivity(task.activityJson) }
        val requests = activities.mapIndexed { index, activity ->
            activity.toTrackingRequest(profileIdentifier = pendingTasks[index].identity ?: "N/A")
        }
        val result = trackingHttpClient.track(batch = requests)
        processBatchResponse(
            pendingTasks = pendingTasks,
            response = result.getOrNull(),
        )
    }

    private fun processBatchResponse(
        pendingTasks: List<TrackingTask>,
        response: BatchTrackingResponse?,
    ) {
        if (response != null) {
            val responseStatusCode = response.statusCode.toLong()
            if (response.isSuccessful && !response.isServerUnavailable) {
                val errorMap = response.errors.associateBy { it.batchIndex ?: 0 }

                queryHelper.updateTasksResponseStatus(
                    responses = pendingTasks.mapIndexed { index, task ->
                        val trackingError = errorMap[index]
                        return@mapIndexed TaskResponse(
                            taskStatus = trackingError?.taskStatus ?: QueueTaskStatus.SENT,
                            statusCode = responseStatusCode,
                            errorReason = trackingError?.reason,
                            id = task.id,
                        )
                    },
                )
            } else {
                queryHelper.updateTasksResponseStatus(
                    responses = pendingTasks.map { task ->
                        TaskResponse(
                            taskStatus = QueueTaskStatus.FAILED,
                            statusCode = responseStatusCode,
                            errorReason = null,
                            id = task.id,
                        )
                    },
                )
            }
        } else {
            queryHelper.updateTasksResponseStatus(
                responses = pendingTasks.map { task ->
                    TaskResponse(
                        taskStatus = QueueTaskStatus.FAILED,
                        statusCode = 1001,
                        errorReason = null,
                        id = task.id,
                    )
                },
            )
        }
    }
}
