package io.customer.shared.tracking.queue

import io.customer.shared.database.DatabaseManager
import io.customer.shared.sdk.config.BackgroundQueueConfig
import io.customer.shared.tracking.api.*
import io.customer.shared.tracking.api.model.*
import io.customer.shared.tracking.constant.Priority
import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.tracking.model.Activity
import io.customer.shared.tracking.model.TaskResponse
import io.customer.shared.util.DateTimeUtil
import io.customer.shared.util.JsonAdapter
import io.customer.shared.util.Logger
import io.customer.shared.work.WorkDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Instant
import local.TrackingTask
import kotlin.time.DurationUnit

internal interface QueueWorker {
    fun checkForPendingTasks(source: QueueTriggerSource)
    fun sendAllPending()
}

internal enum class QueueTriggerSource {
    DATABASE,
    QUEUE,
}

internal class QueueWorkerImpl(
    private val logger: Logger,
    private val dateTimeUtil: DateTimeUtil,
    private val jsonAdapter: JsonAdapter,
    private val backgroundQueueConfig: BackgroundQueueConfig,
    private val databaseManager: DatabaseManager,
    private val trackingHttpClient: TrackingHttpClient,
    private val workDispatcher: WorkDispatcher,
) : QueueWorker {
    private val mutex = Mutex()
//    private var pendingTasksListenerJob: Job? = null
//    private val pendingLogsQueryListener = object : Query.Listener {
//        override fun queryResultsChanged() {
//            pendingTasksListenerJob?.cancelWithoutException()
//            pendingTasksListenerJob = workDispatcher.launchUnique(key = COROUTINE_TAG) {
//                delay(1_000)
//                checkForPendingTasks(source = QueueTriggerSource.DATABASE)
//            }
//        }
//    }

    init {
//        databaseManager.registerPendingTasksListener(pendingLogsQueryListener)
    }

    private fun TrackingTask.canTriggerBatch(timestamp: Instant): Boolean {
        val timeInQueue = timestamp.minus(createdAt).toLong(DurationUnit.SECONDS)
        return isCountedAsBatchTrigger() && (priority > Priority.DEFAULT || timeInQueue >= backgroundQueueConfig.batchDelayMaxDelayInSeconds)
    }

    private fun TrackingTask.isCountedAsBatchTrigger(): Boolean {
        return retryCount <= 0
    }

    override fun checkForPendingTasks(source: QueueTriggerSource) {
        if (mutex.isLocked) return

        workDispatcher.launchShared {
            mutex.lock() // try without lock
            databaseManager.selectAllPending { pendingTasks ->
                try {
                    validateTasksStatus(pendingTasks)
                } catch (ex: Exception) {
                    logger.error("Validation for pending tasks failed with error: ${ex.message}")
                } finally {
//                    pendingTasksListenerJob?.cancelWithoutException()
                    mutex.unlock()
                }
            }
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
            databaseManager.updateStatus(status = QueueTaskStatus.SENDING, tasks = pendingTasks)
            batchTasks(pendingTasks = pendingTasks)
        } else {
            // queue next
            databaseManager.updateStatus(status = QueueTaskStatus.PENDING, tasks = pendingTasks)
        }
    }

    override fun sendAllPending() {
        workDispatcher.launchShared {

        }
    }

    private suspend fun batchTasks(pendingTasks: List<TrackingTask>) {
        val activities =
            pendingTasks.map { task -> jsonAdapter.fromJSON(Activity::class, task.activityJson) }
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

                databaseManager.updateResponseStatus(
                    responses = pendingTasks.mapIndexed { index, task ->
                        val trackingError = errorMap[index]
                        return@mapIndexed TaskResponse(
                            taskStatus = trackingError?.taskStatus ?: QueueTaskStatus.SENT,
                            statusCode = responseStatusCode,
                            errorReason = trackingError?.reason,
                            id = task.identifier,
                        )
                    },
                )
            } else {
                databaseManager.updateResponseStatus(
                    responses = pendingTasks.map { task ->
                        TaskResponse(
                            taskStatus = QueueTaskStatus.FAILED,
                            statusCode = responseStatusCode,
                            errorReason = null,
                            id = task.identifier,
                        )
                    },
                )
            }
        } else {
            databaseManager.updateResponseStatus(
                responses = pendingTasks.map { task ->
                    TaskResponse(
                        taskStatus = QueueTaskStatus.FAILED,
                        statusCode = 1001,
                        errorReason = null,
                        id = task.identifier,
                    )
                },
            )
        }
    }

    companion object {
        private const val COROUTINE_TAG = "QueueWorker"
    }
}
