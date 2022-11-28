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
import kotlinx.datetime.Instant
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
    fun updateTasksStatusFromResponse(
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
    private val platformUtil: PlatformUtil,
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

    /**
     * Looks for unsent duplicate tasks that can be merged and may not be required to be sent as
     * multiple events e.g. Identify, Add Device, etc.
     */
    @WithinTransaction
    private fun Task.getDuplicateOrNull(): TrackingTask? = kotlin.runCatching {
        return@runCatching if (activity.canBeMerged()) {
            trackingTaskQueries.selectByType(
                type = activity.type,
                siteId = workspace.siteId,
            ).executeAsOneOrNull()?.takeUnless { task ->
                task.queueTaskStatus in listOf(
                    QueueTaskStatus.QUEUED,
                    QueueTaskStatus.SENDING,
                    QueueTaskStatus.SENT,
                )
            }
        } else null
    }.getOrNull()

    /**
     * Merges activities that are merge-able, e.g. attributes, etc.
     */
    @WithinTransaction
    private fun TrackingTask.mergeIncompleteActivities(activity: Activity): Activity? {
        var mergedActivity: Activity? = null
        kotlin.runCatching {
            activity.merge(other = jsonAdapter.parseToActivity(activityJson))
        }.fold(
            onSuccess = { value -> mergedActivity = value },
            onFailure = { ex ->
                logger.fatal("Failed to parse activity ${type}, model version ${activityModelVersion}. Reason: ${ex.message}")
            },
        )
        return mergedActivity
    }

    /**
     * Updates status of tasks with the given values.
     *
     * For private use only, should only be called within database transaction.
     */
    @WithinTransaction
    private fun updateTasksStatusInternal(
        status: QueueTaskStatus,
        tasks: List<TrackingTask>,
    ): QueueTaskResult {
        val taskIds = tasks.map { task -> task.uuid }
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

    /**
     * Runs the given block in database transaction synchronously. The method returns result
     * instantly and should only be called from background dispatcher.
     */
    private fun <Result : Any?> runInTransaction(
        block: TransactionWithReturn<Result>.() -> Result,
    ): Result {
        return trackingTaskQueries.transactionWithResult(noEnclosing = false) { block() }
    }

    /**
     * Runs the given block in database transaction asynchronously. The method does not return
     * any result and should only called where receiving result is not required.
     */
    private fun runInTransactionAsync(
        block: TransactionWithReturn<Unit>.() -> Unit,
    ) = runSuspended { runInTransaction(block = block) }

    @MainDispatcher
    override fun insertTask(task: Task, listener: TaskResultListener<QueueTaskResult>?) {
        runSuspended {
            val result = runInTransaction<Result<QueueTaskResult>> {
                return@runInTransaction kotlin.runCatching {
                    val currentTime = dateTimeUtil.now
                    val duplicateTask = task.getDuplicateOrNull()
                    val mergedActivity =
                        duplicateTask?.mergeIncompleteActivities(activity = task.activity)
                    val activity = mergedActivity ?: task.activity

                    val uuid: String
                    val createdAt: Instant
                    if (mergedActivity != null) {
                        // since we are updating pending task
                        uuid = duplicateTask.uuid
                        createdAt = duplicateTask.createdAt
                    } else {
                        uuid = platformUtil.generateUUID()
                        createdAt = currentTime
                    }

                    val json = jsonAdapter.parseToString(activity = activity)
                    trackingTaskQueries.insertOrReplaceTask(
                        uuid = uuid,
                        siteId = workspace.siteId,
                        type = activity.type,
                        createdAt = createdAt,
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
                    return@runCatching true
                }
            }
            result.fold(
                onSuccess = { listener?.success() },
                onFailure = { ex ->
                    logger.error("Unable to add ${task.activity} to queue, skipping task. Reason: ${ex.message}")
                    listener?.failure(exception = ex)
                },
            )
        }
    }

    @BackgroundDispatcher
    override fun updateTasksStatus(
        status: QueueTaskStatus,
        tasks: List<TrackingTask>,
    ): QueueTaskResult = runInTransaction {
        updateTasksStatusInternal(status = status, tasks = tasks)
    }

    @BackgroundDispatcher
    override fun updateTasksStatusFromResponse(
        responses: List<TaskResponse>,
    ): QueueTaskResult = runInTransaction {
        val result = kotlin.runCatching {
            val updatedAtTime = dateTimeUtil.now
            responses.forEach { response ->
                trackingTaskQueries.updateTaskStatusFromResponse(
                    updatedAt = updatedAtTime,
                    status = response.taskStatus,
                    statusCode = response.statusCode,
                    errorReason = response.errorReason,
                    ids = listOf(response.uuid),
                    siteId = workspace.siteId,
                    retryAttempts = if (response.shouldCountAsRetry) 1 else 0,
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
        val result = updateTasksStatusInternal(
            status = QueueTaskStatus.QUEUED,
            tasks = pendingTasks,
        )
        return@runInTransaction pendingTasks.takeIf { result }
    }

    @MainDispatcher
    override fun clearAllExpiredTasks() {
        runInTransactionAsync {
            trackingTaskQueries.clearAllTasksWithStatus(
                status = listOf(QueueTaskStatus.SENT, QueueTaskStatus.INVALID),
                siteId = workspace.siteId,
            )
        }
    }
}
