package io.customer.shared.database

import com.squareup.sqldelight.Query
import io.customer.shared.common.QueueTaskResult
import io.customer.shared.sdk.config.BackgroundQueueConfig
import io.customer.shared.sdk.meta.Workspace
import io.customer.shared.tracking.constant.Priority
import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.tracking.model.Activity
import io.customer.shared.tracking.model.Task
import io.customer.shared.tracking.model.TaskResponse
import io.customer.shared.tracking.model.type
import io.customer.shared.tracking.queue.TaskResultListener
import io.customer.shared.tracking.queue.failure
import io.customer.shared.tracking.queue.success
import io.customer.shared.util.DateTimeUtil
import io.customer.shared.util.JsonAdapter
import io.customer.shared.util.Logger
import io.customer.shared.work.WorkDispatcher
import local.TrackingTask
import local.TrackingTaskQueries

internal interface DatabaseManager {
    fun identifyProfile(identifier: String)
    fun insert(task: Task, listener: TaskResultListener<QueueTaskResult>? = null)
    suspend fun updateStatus(status: QueueTaskStatus, tasks: List<TrackingTask>)
    fun updateResponseStatus(
        responses: List<TaskResponse>,
        listener: TaskResultListener<QueueTaskResult>? = null,
    )

    fun selectAllPending(callback: suspend (List<TrackingTask>) -> Unit)
    fun registerPendingTasksListener(listener: Query.Listener)
    fun unregisterPendingTasksListener(listener: Query.Listener)
    fun clearAllExpired()
}

internal class DatabaseManagerImpl(
    private val logger: Logger,
    private val dateTimeUtil: DateTimeUtil,
    private val jsonAdapter: JsonAdapter,
    private val backgroundQueueConfig: BackgroundQueueConfig,
    private val workspace: Workspace,
    private val trackingTaskQueries: TrackingTaskQueries,
    private val workDispatcher: WorkDispatcher,
) : DatabaseManager {
    private val selectAllPendingQuery = trackingTaskQueries.selectAllPending(
        status = listOf(QueueTaskStatus.PENDING, QueueTaskStatus.FAILED),
        limit = backgroundQueueConfig.batchTasksMax.toLong(),
    )

    private fun Activity.createID(): String = when (this) {
        is Activity.AddDevice,
        is Activity.DeleteDevice,
        is Activity.IdentifyProfile,
        -> type
        is Activity.Event,
        is Activity.Metric,
        is Activity.Page,
        is Activity.Screen,
        -> "$type-$timestamp"
    }

    private suspend fun updateStatusInternal(
        status: QueueTaskStatus,
        tasks: List<TrackingTask>,
    ) {
        val ids = tasks.map { task -> task.identifier }
        try {
            trackingTaskQueries.updateStatus(
                status = status,
                ids = ids,
            )
        } catch (ex: Exception) {
            logger.error("Unable to update status $status for tasks ${ids.joinToString(",")}. Reason: ${ex.message}")
        }
    }

    override fun identifyProfile(identifier: String) {
        workDispatcher.launchShared {
            trackingTaskQueries.updateAllAnonymous(
                identifier = identifier,
            )
            logger.debug("Updating identifier with $identifier")
        }
    }

    override fun insert(task: Task, listener: TaskResultListener<QueueTaskResult>?) {
        val activity = task.activity
        workDispatcher.launchShared {
            try {
                val currentTime = dateTimeUtil.now
                val json = jsonAdapter.toJSON(kClazz = Activity::class, content = activity)
                trackingTaskQueries.insert(
                    identifier = activity.createID(),
                    siteId = workspace.siteId,
                    type = activity.type,
                    createdAt = currentTime,
                    expiresAt = null,
                    stalesAt = currentTime,
                    activityJson = json,
                    activityModelVersion = activity.modelVersion,
                    identity = task.profileIdentifier,
                    queueTaskStatus = QueueTaskStatus.PENDING,
                    priority = Priority.DEFAULT,
                )
                listener?.success()
            } catch (ex: Exception) {
                logger.error("Unable to add $activity to queue, skipping activity. Reason: ${ex.message}")
                listener?.failure(exception = ex)
            }
        }
    }

    override suspend fun updateStatus(
        status: QueueTaskStatus,
        tasks: List<TrackingTask>,
    ) {
        updateStatusInternal(
            status = status,
            tasks = tasks,
        )
    }

    override fun updateResponseStatus(
        responses: List<TaskResponse>,
        listener: TaskResultListener<QueueTaskResult>?,
    ) {
        trackingTaskQueries.transaction {
            workDispatcher.launchShared {
                try {
                    responses.forEach { response ->
                        trackingTaskQueries.updateWithFailure(
                            status = response.taskStatus,
                            statusCode = response.statusCode,
                            errorReason = response.errorReason,
                            ids = listOf(response.id),
                        )
                    }
                    listener?.success()
                } catch (ex: Exception) {
                    logger.error("Unable to updated response status for ${responses.size} pending tasks due to: ${ex.message}")
                    listener?.failure(exception = ex)
                }
            }
        }
    }

    override fun selectAllPending(callback: suspend (List<TrackingTask>) -> Unit) {
        trackingTaskQueries.transaction {
            workDispatcher.launchShared {
                val pendingTasks = selectAllPendingQuery.executeAsList()
                updateStatusInternal(
                    status = QueueTaskStatus.QUEUED,
                    tasks = pendingTasks,
                )
                callback(pendingTasks)
            }
        }
    }

    override fun registerPendingTasksListener(listener: Query.Listener) {
        selectAllPendingQuery.addListener(listener)
    }

    override fun unregisterPendingTasksListener(listener: Query.Listener) {
        selectAllPendingQuery.removeListener(listener)
    }

    override fun clearAllExpired() {
        workDispatcher.launchShared {
            trackingTaskQueries.clearAllWithStatus(QueueTaskStatus.SENT)
        }
    }
}
