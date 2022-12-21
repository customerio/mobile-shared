package io.customer.shared.tracking.queue

import io.customer.shared.Platform
import io.customer.shared.common.CustomAttributes
import io.customer.shared.database.TrackingTaskQueryHelper
import io.customer.shared.sdk.meta.Workspace
import io.customer.shared.tracking.api.model.Device
import io.customer.shared.tracking.constant.MetricEvent
import io.customer.shared.tracking.constant.TrackingType
import io.customer.shared.tracking.model.Activity
import io.customer.shared.tracking.model.Task
import io.customer.shared.util.DateTimeUtil
import io.customer.shared.util.Logger
import io.customer.shared.util.nowSeconds
import io.customer.shared.work.JobDispatcher
import io.customer.shared.work.JobExecutor
import io.customer.shared.work.runOnBackground
import io.customer.shared.work.runOnMain
import kotlinx.coroutines.Job

/**
 * Background queue is responsible for queuing tasks to trigger when needed. It works asynchronously
 * and does not blocks the caller for any operation. Caller should provide [TaskResultListener] to
 * listen on results for any request.
 */
interface BackgroundQueue {
    fun queueIdentifyProfile(
        profileIdentifier: String,
        attributes: CustomAttributes,
        listener: TaskResultListener<Unit>? = null,
    ): Job

    fun queueTrack(
        profileIdentifier: String?,
        name: String,
        trackingType: TrackingType,
        attributes: CustomAttributes,
        listener: TaskResultListener<Unit>? = null,
    ): Job

    fun queueRegisterDevice(
        profileIdentifier: String?,
        device: Device,
        listener: TaskResultListener<Unit>? = null,
    ): Job

    fun queueDeletePushToken(
        profileIdentifier: String?,
        deviceToken: String,
        listener: TaskResultListener<Unit>? = null,
    ): Job

    fun queueTrackMetric(
        deliveryId: String,
        deviceToken: String,
        event: MetricEvent,
        listener: TaskResultListener<Unit>? = null,
    ): Job

    fun queueTrackInAppMetric(
        deliveryId: String,
        event: MetricEvent,
        listener: TaskResultListener<Unit>? = null,
    ): Job

    fun sendAllPending(): Job

    fun deleteExpiredTasks(): Job
}

internal class BackgroundQueueImpl(
    private val logger: Logger,
    private val dateTimeUtil: DateTimeUtil,
    private val workspace: Workspace,
    private val platform: Platform,
    override val jobExecutor: JobExecutor,
    private val trackingTaskQueryHelper: TrackingTaskQueryHelper,
    private val queueDispatcher: QueueDispatcher,
) : BackgroundQueue, JobDispatcher {
    override fun queueIdentifyProfile(
        profileIdentifier: String,
        attributes: CustomAttributes,
        listener: TaskResultListener<Unit>?,
    ) = addToQueue(
        profileIdentifier = profileIdentifier,
        activity = Activity.IdentifyProfile(
            timestamp = dateTimeUtil.nowSeconds,
            attributes = attributes,
        ),
        listener = listener,
    )

    override fun queueTrack(
        profileIdentifier: String?,
        name: String,
        trackingType: TrackingType,
        attributes: CustomAttributes,
        listener: TaskResultListener<Unit>?,
    ) = addToQueue(
        profileIdentifier = profileIdentifier,
        activity = when (trackingType) {
            TrackingType.EVENT -> Activity.Event(
                timestamp = dateTimeUtil.nowSeconds,
                attributes = attributes,
                name = name,
            )
            TrackingType.PAGE -> Activity.Page(
                timestamp = dateTimeUtil.nowSeconds,
                attributes = attributes,
                name = name,
            )
            TrackingType.SCREEN -> Activity.Screen(
                timestamp = dateTimeUtil.nowSeconds,
                attributes = attributes,
                name = name,
            )
        },
        listener = listener,
    )

    override fun queueRegisterDevice(
        profileIdentifier: String?,
        device: Device,
        listener: TaskResultListener<Unit>?,
    ) = addToQueue(
        profileIdentifier = profileIdentifier,
        activity = Activity.AddDevice(
            timestamp = dateTimeUtil.nowSeconds,
            device = device.copy(
                platform = platform.name,
            ),
        ),
        listener = listener,
    )

    override fun queueDeletePushToken(
        profileIdentifier: String?,
        deviceToken: String,
        listener: TaskResultListener<Unit>?,
    ) = addToQueue(
        profileIdentifier = profileIdentifier,
        activity = Activity.DeleteDevice(
            device = Device(
                token = deviceToken,
                platform = platform.name,
            ),
        ),
        listener = listener,
    )

    override fun queueTrackMetric(
        deliveryId: String,
        deviceToken: String,
        event: MetricEvent,
        listener: TaskResultListener<Unit>?,
    ) = addToQueue(
        profileIdentifier = null,
        activity = Activity.Metric(
            metricEvent = event,
            deliveryId = deliveryId,
            deviceToken = deviceToken,
            timestamp = dateTimeUtil.nowSeconds,
        ),
        listener = listener,
    )

    override fun queueTrackInAppMetric(
        deliveryId: String,
        event: MetricEvent,
        listener: TaskResultListener<Unit>?,
    ) = addToQueue(
        profileIdentifier = null,
        activity = Activity.Metric(
            metricEvent = event,
            deliveryId = deliveryId,
            deviceToken = null,
            timestamp = dateTimeUtil.nowSeconds,
        ),
        listener = listener,
    )

    private fun addToQueue(
        profileIdentifier: String?,
        activity: Activity,
        listener: TaskResultListener<Unit>? = null,
    ) = runOnBackground {
        val result = trackingTaskQueryHelper.insertTask(
            task = Task(
                profileIdentifier = profileIdentifier,
                identityType = workspace.identityType,
                activity = activity,
            ),
        )
        queueDispatcher.checkForPendingTasks(source = QueueTriggerSource.DATABASE)
        listener?.run { runOnMain { onComplete(result) } }
    }

    override fun sendAllPending() = runOnBackground {
        queueDispatcher.sendAllPending()
    }

    override fun deleteExpiredTasks() = runOnBackground {
        trackingTaskQueryHelper.clearAllExpiredTasks()
    }
}
