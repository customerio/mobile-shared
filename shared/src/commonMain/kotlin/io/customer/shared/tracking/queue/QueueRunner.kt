package io.customer.shared.tracking.queue

import io.customer.shared.database.TrackingTask
import io.customer.shared.database.TrackingTaskQueryHelper
import io.customer.shared.sdk.meta.Workspace
import io.customer.shared.tracking.api.*
import io.customer.shared.tracking.api.model.*
import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.tracking.model.Task
import io.customer.shared.tracking.model.TaskResponse
import io.customer.shared.util.JsonAdapter
import io.customer.shared.util.Logger
import io.customer.shared.util.fromJSON

/**
 * Class responsible for sending tasks to server by communicating with network layer and updating
 * tasks status and responses in database.
 */
internal interface QueueRunner {
    suspend fun runQueueForTasks(pendingTasks: List<TrackingTask>): Result<Boolean>
}

internal class QueueRunnerImpl(
    private val logger: Logger,
    private val jsonAdapter: JsonAdapter,
    private val workspace: Workspace,
    private val trackingTaskQueryHelper: TrackingTaskQueryHelper,
    private val trackingHttpClient: TrackingHttpClient,
) : QueueRunner {
    override suspend fun runQueueForTasks(
        pendingTasks: List<TrackingTask>,
    ): Result<Boolean> = kotlin.runCatching {
        trackingTaskQueryHelper.updateTasksStatus(
            status = QueueTaskStatus.SENDING,
            tasks = pendingTasks,
        )

        val batchResult = processBatchTasks(pendingTasks = pendingTasks)
        val batchResponse = batchResult.getOrNull()
        if (batchResponse != null) {
            val (response, tasksResponse) = batchResponse
            trackingTaskQueryHelper.updateTasksStatusFromResponse(responses = tasksResponse)
            return@runCatching !response.isServerUnavailable
        } else {
            logger.error("Failed to run queue for ${pendingTasks.size} tasks with error: ${batchResult.exceptionOrNull()?.message}")
            trackingTaskQueryHelper.updateTasksStatus(
                status = QueueTaskStatus.FAILED,
                tasks = pendingTasks,
            )
            return@runCatching false
        }
    }

    private suspend fun processBatchTasks(
        pendingTasks: List<TrackingTask>,
    ): Result<Pair<BatchTrackingResponse, List<TaskResponse>>> = kotlin.runCatching {
        val result = trackingHttpClient.track(
            tasks = pendingTasks.map { task ->
                Task(
                    identityType = workspace.identityType,
                    profileIdentifier = task.identity,
                    activity = jsonAdapter.fromJSON(task.activityJson),
                )
            },
        )
        val response = result.getOrThrow()
        val tasksResponse = processBatchResponse(pendingTasks = pendingTasks, response = response)
        return@runCatching response to tasksResponse
    }

    @Throws(Exception::class)
    private fun processBatchResponse(
        pendingTasks: List<TrackingTask>,
        response: BatchTrackingResponse?,
    ): List<TaskResponse> {
        val responseStatusCode = response?.statusCode?.toLong()
        if (response?.isSuccessful == true) {
            val errorMap = response.errors.associateBy { it.batchIndex ?: 0 }
            return pendingTasks.mapIndexed { index, task ->
                val trackingError = errorMap[index]
                TaskResponse(
                    uuid = task.uuid,
                    taskStatus = trackingError?.taskStatus ?: QueueTaskStatus.SENT,
                    statusCode = responseStatusCode,
                    errorReason = trackingError?.reason,
                )
            }
        } else {
            return pendingTasks.mapIndexed { _, task ->
                TaskResponse(
                    uuid = task.uuid,
                    taskStatus = QueueTaskStatus.FAILED,
                    statusCode = responseStatusCode,
                    errorReason = null,
                )
            }
        }
    }
}
