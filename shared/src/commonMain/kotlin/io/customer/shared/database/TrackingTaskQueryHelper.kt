package io.customer.shared.database

import com.squareup.sqldelight.TransactionWithReturn
import io.customer.shared.sdk.config.BackgroundQueueConfig
import io.customer.shared.sdk.meta.Workspace
import io.customer.shared.tracking.constant.Priority
import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.tracking.model.*
import io.customer.shared.util.DatabaseUtil
import io.customer.shared.util.DateTimeUtil
import io.customer.shared.util.JsonAdapter
import io.customer.shared.util.Logger

/**
 * The class works as a bridge for SQL queries. All queries to database should be made using this
 * class to provide abstraction and keep communication with database easier.
 */
internal interface TrackingTaskQueryHelper {
    suspend fun insertTask(task: Task): Result<Unit>
    suspend fun updateTasksStatus(status: QueueTaskStatus, tasks: List<TrackingTask>): Result<Unit>
    suspend fun updateTasksStatusFromResponse(responses: List<TaskResponse>): Result<Unit>
    suspend fun selectAllPendingTasks(): Result<List<TrackingTask>?>
    suspend fun clearAllExpiredTasks(): Result<Unit>
}

internal class TrackingTaskQueryHelperImpl(
    private val logger: Logger,
    private val dateTimeUtil: DateTimeUtil,
    private val jsonAdapter: JsonAdapter,
    private val databaseUtil: DatabaseUtil,
    private val workspace: Workspace,
    private val backgroundQueueConfig: BackgroundQueueConfig,
    private val trackingTaskDAO: TrackingTaskQueries,
) : TrackingTaskQueryHelper {
    private val selectAllPendingQuery = trackingTaskDAO.selectAllPendingTasks(
        status = listOf(QueueTaskStatus.PENDING, QueueTaskStatus.FAILED),
        limit = backgroundQueueConfig.batchTasksMax.toLong(),
        siteId = workspace.siteId,
    )

    /**
     * Runs the given block in database transaction synchronously. The method returns result
     * instantly and should only be called from background dispatcher.
     */
    private fun <Result : Any?> runInTransaction(
        block: TransactionWithReturn<Result>.() -> Result,
    ): Result {
        return trackingTaskDAO.transactionWithResult(noEnclosing = false) { block() }
    }

    /**
     * Updates status of tasks with the given values.
     *
     * For private use only, should only be called within database transaction.
     */
    private fun updateTasksStatusInternal(
        status: QueueTaskStatus,
        tasks: List<TrackingTask>,
    ): Result<Unit> {
        val taskIds = tasks.map { task -> task.uuid }
        return kotlin.runCatching {
            trackingTaskDAO.updateTasksStatus(
                updatedAt = dateTimeUtil.now,
                status = status,
                ids = taskIds,
                siteId = workspace.siteId,
            )
        }.onFailure { ex ->
            logger.error("Unable to update status $status for tasks ${taskIds.joinToString(separator = ",")}. Reason: ${ex.message}")
        }
    }

    /**
     * Inserts activity task safely to the database.
     *
     * For private use only, should only be called within database transaction.
     */
    private fun insertTaskInternal(task: Task) = kotlin.runCatching {
        val currentTime = dateTimeUtil.now
        val activity = task.activity

        val json = jsonAdapter.toJSON(kClazz = Activity::class, content = activity)
        trackingTaskDAO.insertOrReplaceTask(
            uuid = databaseUtil.generateUUID(),
            siteId = workspace.siteId,
            type = activity.type,
            createdAt = currentTime,
            updatedAt = currentTime,
            identity = task.profileIdentifier,
            identityType = workspace.identityType,
            activityJson = json,
            activityModelVersion = activity.modelVersion,
            queueTaskStatus = QueueTaskStatus.PENDING,
            priority = Priority.DEFAULT,
        )
        logger.debug("Adding task ${activity.type} to queue successful")

        if (activity is Activity.IdentifyProfile) {
            trackingTaskDAO.updateAllAnonymousTasks(
                updatedAt = currentTime,
                identifier = task.profileIdentifier,
                identityType = workspace.identityType,
                siteId = workspace.siteId,
            )
            logger.debug("Updating identifier with ${task.profileIdentifier} and identity type ${workspace.identityType}")
        }
    }

    override suspend fun insertTask(task: Task): Result<Unit> = runInTransaction<Result<Unit>> {
        insertTaskInternal(task = task)
    }.onFailure { ex ->
        logger.error("Unable to add ${task.activity} to queue, skipping task. Reason: ${ex.message}")
    }

    override suspend fun updateTasksStatus(
        status: QueueTaskStatus,
        tasks: List<TrackingTask>,
    ): Result<Unit> = runInTransaction {
        updateTasksStatusInternal(status = status, tasks = tasks)
    }

    override suspend fun updateTasksStatusFromResponse(
        responses: List<TaskResponse>,
    ): Result<Unit> = runInTransaction {
        kotlin.runCatching {
            val updatedAtTime = dateTimeUtil.now
            responses.forEach { response ->
                trackingTaskDAO.updateTaskStatusFromResponse(
                    updatedAt = updatedAtTime,
                    status = response.taskStatus,
                    statusCode = response.statusCode,
                    errorReason = response.errorReason,
                    ids = listOf(response.uuid),
                    siteId = workspace.siteId,
                    retryAttempts = if (response.shouldCountAsRetry) 1 else 0,
                )
            }
        }.onFailure { ex ->
            logger.error("Unable to updated response status for ${responses.size} pending tasks due to: ${ex.message}")
        }
    }

    override suspend fun selectAllPendingTasks(): Result<List<TrackingTask>?> = runInTransaction {
        val pendingTasks = selectAllPendingQuery.executeAsList()
        val result = updateTasksStatusInternal(
            status = QueueTaskStatus.QUEUED,
            tasks = pendingTasks,
        )
        val exception = result.exceptionOrNull()
        return@runInTransaction if (exception == null) Result.success(pendingTasks)
        else Result.failure(exception)
    }

    override suspend fun clearAllExpiredTasks(): Result<Unit> = runInTransaction<Result<Unit>> {
        kotlin.runCatching {
            trackingTaskDAO.clearAllTasksWithStatus(
                status = listOf(QueueTaskStatus.SENT, QueueTaskStatus.INVALID),
                siteId = workspace.siteId,
            )
        }
    }.onFailure { ex ->
        logger.error("Unable to clear expired tasks from queue. Reason: ${ex.message}")
    }
}
