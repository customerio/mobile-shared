package io.customer.shared.database

import com.squareup.sqldelight.TransactionWithReturn
import io.customer.shared.common.QueueTaskResult
import io.customer.shared.sdk.config.BackgroundQueueConfig
import io.customer.shared.sdk.meta.Workspace
import io.customer.shared.tracking.constant.Priority
import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.tracking.model.*
import io.customer.shared.tracking.queue.TaskResultListener
import io.customer.shared.tracking.queue.failure
import io.customer.shared.tracking.queue.success
import io.customer.shared.util.*
import io.customer.shared.work.*
import local.TrackingTask
import local.TrackingTaskQueries

/**
 * The class works as a bridge for SQL queries. All queries to database should be made using this
 * class to keep some abstraction from the database layer.
 */
internal interface QueryHelper {
    @MainDispatcher
    fun insertTask(
        task: Task,
        listener: TaskResultListener<QueueTaskResult>? = null,
    )

    @BackgroundDispatcher
    fun updateTasksStatus(
        status: QueueTaskStatus,
        tasks: List<TrackingTask>,
    ): QueueTaskResult

    @BackgroundDispatcher
    fun updateTasksResponseStatus(
        responses: List<TaskResponse>,
    ): QueueTaskResult

    @BackgroundDispatcher
    fun selectAllPendingTasks(): List<TrackingTask>?

    @MainDispatcher
    fun clearAllExpiredTasks()
}

internal class QueryHelperImpl(
    private val logger: Logger,
    private val dateTimeUtil: DateTimeUtil,
    private val jsonAdapter: JsonAdapter,
    override val executor: CoroutineExecutor,
    private val workspace: Workspace,
    private val backgroundQueueConfig: BackgroundQueueConfig,
    private val trackingTaskQueries: TrackingTaskQueries,
) : QueryHelper, CoroutineExecutable {
    private val selectAllPendingQuery = trackingTaskQueries.selectAllPendingTasks(
        status = listOf(QueueTaskStatus.PENDING, QueueTaskStatus.FAILED),
        limit = backgroundQueueConfig.batchTasksMax.toLong(),
        siteId = workspace.siteId,
    )

    @Throws(Exception::class)
    @WithinTransaction
    private fun Task.mergeWithSimilarPending(): Activity {
        if (!activity.isUnique()) return activity

        val newActivity = activity
        val oldActivityTask = trackingTaskQueries.selectByType(
            type = newActivity.type,
            siteId = workspace.siteId,
        ).executeAsOneOrNull()

        var mergedActivity: Activity? = null
        if (oldActivityTask != null && oldActivityTask.queueTaskStatus != QueueTaskStatus.SENT) {
            val result = kotlin.runCatching {
                newActivity.merge(other = jsonAdapter.parseToActivity(oldActivityTask.activityJson))
            }
            result.fold(
                onSuccess = { value -> mergedActivity = value },
                onFailure = { ex ->
                    logger.fatal("Failed to parse activity ${oldActivityTask.type}, model version ${oldActivityTask.activityModelVersion}. Reason: ${ex.message}")
                },
            )
        }
        return mergedActivity ?: newActivity
    }

    @WithinTransaction
    private fun updateStatusInternal(
        status: QueueTaskStatus,
        tasks: List<TrackingTask>,
    ): QueueTaskResult {
        val taskIds = tasks.map { task -> task.id }
        val result = kotlin.runCatching {
            trackingTaskQueries.updateTasksStatus(
                updatedAt = dateTimeUtil.now,
                status = status,
                ids = taskIds,
                siteId = workspace.siteId,
            )
        }
        result.onFailure { ex ->
            logger.error("Unable to update status $status for tasks ${taskIds.joinToString(separator = ",")}. Reason: ${ex.message}")
        }
        return result.isSuccess
    }

    private fun <Result : Any?> runInTransaction(
        block: TransactionWithReturn<Result>.() -> Result,
    ): Result {
        return trackingTaskQueries.transactionWithResult(noEnclosing = false) { block() }
    }

    private fun <Result : Any> runInTransactionAsync(
        block: TransactionWithReturn<Result>.() -> Result,
    ) = runSuspended { runInTransaction(block = block) }

    @MainDispatcher
    override fun insertTask(task: Task, listener: TaskResultListener<QueueTaskResult>?) {
        runInTransactionAsync {
            val activity = task.mergeWithSimilarPending()
            val result = kotlin.runCatching {
                val currentTime = dateTimeUtil.now
                val json = jsonAdapter.parseToString(activity = activity)

                trackingTaskQueries.insertOrReplaceTasks(
                    id = activity.generateUniqueID(),
                    siteId = workspace.siteId,
                    type = activity.type,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    expiresAt = null,
                    stalesAt = null,
                    identity = task.profileIdentifier,
                    identityType = workspace.identityType,
                    activityJson = json,
                    activityModelVersion = activity.modelVersion,
                    queueTaskStatus = QueueTaskStatus.PENDING,
                    priority = Priority.DEFAULT,
                )
                logger.debug("Adding task ${activity.type} to queue successful")

                if (activity is Activity.IdentifyProfile) {
                    trackingTaskQueries.updateAllAnonymousTasks(
                        updatedAt = currentTime,
                        identifier = task.profileIdentifier,
                        identityType = workspace.identityType,
                        siteId = workspace.siteId,
                    )
                    logger.debug("Updating identifier with ${task.profileIdentifier} and identity type ${workspace.identityType}")
                }
            }
            result.fold(
                onSuccess = {
                    runSuspended { listener?.success() }
                },
                onFailure = { ex ->
                    logger.error("Unable to add $activity to queue, skipping task. Reason: ${ex.message}")
                    runSuspended { listener?.failure(exception = ex) }
                },
            )
        }
    }

    @BackgroundDispatcher
    override fun updateTasksStatus(
        status: QueueTaskStatus,
        tasks: List<TrackingTask>,
    ): QueueTaskResult = runInTransaction {
        updateStatusInternal(status = status, tasks = tasks)
    }

    @BackgroundDispatcher
    override fun updateTasksResponseStatus(
        responses: List<TaskResponse>,
    ): QueueTaskResult = runInTransaction {
        val result = kotlin.runCatching {
            val updatedAtTime = dateTimeUtil.now
            responses.forEach { response ->
                trackingTaskQueries.updateFailedTaskStatus(
                    updatedAt = updatedAtTime,
                    status = response.taskStatus,
                    statusCode = response.statusCode,
                    errorReason = response.errorReason,
                    ids = listOf(response.id),
                    siteId = workspace.siteId,
                )
            }
        }
        result.onFailure { ex ->
            logger.error("Unable to updated response status for ${responses.size} pending tasks due to: ${ex.message}")
        }
        return@runInTransaction result.isSuccess
    }

    @BackgroundDispatcher
    override fun selectAllPendingTasks(): List<TrackingTask>? = runInTransaction {
        val pendingTasks = selectAllPendingQuery.executeAsList()
        val result = updateStatusInternal(
            status = QueueTaskStatus.QUEUED,
            tasks = pendingTasks,
        )
        return@runInTransaction pendingTasks.takeIf { result }
    }

    @MainDispatcher
    override fun clearAllExpiredTasks() {
        runInTransaction {
            trackingTaskQueries.clearAllTasksWithStatus(
                status = listOf(QueueTaskStatus.SENT, QueueTaskStatus.INVALID),
                siteId = workspace.siteId,
            )
        }
    }
}
