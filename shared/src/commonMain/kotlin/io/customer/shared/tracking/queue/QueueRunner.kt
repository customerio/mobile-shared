package io.customer.shared.tracking.queue

import io.customer.shared.database.TrackingTask
import io.customer.shared.database.TrackingTaskQueryHelper
import io.customer.shared.sdk.meta.Workspace
import io.customer.shared.tracking.api.*
import io.customer.shared.tracking.api.model.*
import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.tracking.model.Activity
import io.customer.shared.tracking.model.TaskResponse
import io.customer.shared.util.JsonAdapter
import io.customer.shared.util.Logger

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
    override suspend fun runQueueForTasks(pendingTasks: List<TrackingTask>): Result<Boolean> {
        val result = kotlin.runCatching {
            trackingTaskQueryHelper.updateTasksStatus(
                status = QueueTaskStatus.SENDING,
                tasks = pendingTasks,
            )
            val response = processBatchTasks(pendingTasks = pendingTasks)
            return@runCatching response != null && !response.isServerUnavailable
        }
        result.onFailure { ex ->
            logger.error("Failed to run queue for ${pendingTasks.size} tasks with error: ${ex.message}")
            trackingTaskQueryHelper.updateTasksStatus(
                status = QueueTaskStatus.FAILED,
                tasks = pendingTasks,
            )
            ex.printStackTrace()
        }
        return result
    }

    @Throws(Exception::class)
    private suspend fun processBatchTasks(pendingTasks: List<TrackingTask>): BatchTrackingResponse? {
        val activities = pendingTasks.map { task ->
            jsonAdapter.fromJSON(Activity::class, task.activityJson)
        }
        val requests = activities.mapIndexed { index, activity ->
            activity.toTrackingRequest(
                identityType = workspace.identityType,
                // QueueRunner should ideally never get tasks with null identity
                profileIdentifier = pendingTasks[index].identity ?: "N/A",
            )
        }
        val result = trackingHttpClient.track(batch = requests)
        val response = result.getOrNull()
        processBatchResponse(pendingTasks = pendingTasks, response = response)
        return response
    }

    @Throws(Exception::class)
    private fun processBatchResponse(
        pendingTasks: List<TrackingTask>,
        response: BatchTrackingResponse?,
    ) {
        val updateTasksStatus = { mapper: (index: Int, task: TrackingTask) -> TaskResponse ->
            trackingTaskQueryHelper.updateTasksStatusFromResponse(
                responses = pendingTasks.mapIndexed(mapper),
            )
        }

        if (response == null) {
            updateTasksStatus { _, task ->
                TaskResponse(uuid = task.uuid, taskStatus = QueueTaskStatus.FAILED)
            }
            return
        }

        val responseStatusCode = response.statusCode.toLong()
        if (response.isSuccessful) {
            val errorMap = response.errors.associateBy { it.batchIndex ?: 0 }
            updateTasksStatus { index, task ->
                val trackingError = errorMap[index]
                TaskResponse(
                    uuid = task.uuid,
                    taskStatus = trackingError?.taskStatus ?: QueueTaskStatus.SENT,
                    statusCode = responseStatusCode,
                    errorReason = trackingError?.reason,
                )
            }
        } else {
            updateTasksStatus { _, task ->
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
