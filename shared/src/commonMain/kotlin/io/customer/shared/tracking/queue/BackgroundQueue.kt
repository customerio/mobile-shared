package io.customer.shared.tracking.queue

import io.customer.shared.Platform
import io.customer.shared.common.CustomAttributesCompat
import io.customer.shared.common.QueueTaskResult
import io.customer.shared.common.fix
import io.customer.shared.database.QueryHelper
import io.customer.shared.sdk.meta.Workspace
import io.customer.shared.tracking.api.model.Device
import io.customer.shared.tracking.constant.MetricEvent
import io.customer.shared.tracking.constant.TrackingType
import io.customer.shared.tracking.model.Activity
import io.customer.shared.tracking.model.Task
import io.customer.shared.util.DateTimeUtil
import io.customer.shared.util.Logger

/**
 * Background queue is responsible for queuing tasks to trigger when needed. It works asynchronously
 * and does not blocks the caller for any operation.
 */
interface BackgroundQueue {
    fun queueIdentifyProfile(
        profileIdentifier: String,
        attributes: CustomAttributesCompat,
        listener: TaskResultListener<QueueTaskResult>? = null,
    )

    fun queueTrack(
        profileIdentifier: String?,
        name: String,
        trackingType: TrackingType,
        attributes: CustomAttributesCompat,
        listener: TaskResultListener<QueueTaskResult>? = null,
    )

    fun queueRegisterDevice(
        profileIdentifier: String?,
        device: Device,
        listener: TaskResultListener<QueueTaskResult>? = null,
    )

    fun queueDeletePushToken(
        profileIdentifier: String?,
        deviceToken: String,
        listener: TaskResultListener<QueueTaskResult>? = null,
    )

    fun queueTrackMetric(
        deliveryId: String,
        deviceToken: String,
        event: MetricEvent,
        listener: TaskResultListener<QueueTaskResult>? = null,
    )

    fun queueTrackInAppMetric(
        deliveryId: String,
        event: MetricEvent,
        listener: TaskResultListener<QueueTaskResult>? = null,
    )

    fun sendAllPending()

    fun deleteExpiredTasks()
}

internal class BackgroundQueueImpl(
    private val logger: Logger,
    private val dateTimeUtil: DateTimeUtil,
    private val workspace: Workspace,
    private val platform: Platform,
    private val queryHelper: QueryHelper,
    private val queueWorker: QueueWorker,
) : BackgroundQueue {
    override fun queueIdentifyProfile(
        profileIdentifier: String,
        attributes: CustomAttributesCompat,
        listener: TaskResultListener<QueueTaskResult>?,
    ) = addToQueue(
        profileIdentifier = profileIdentifier,
        activity = Activity.IdentifyProfile(
            timestamp = dateTimeUtil.nowUnixTimestamp,
        ),
        listener = listener,
    )

    override fun queueTrack(
        profileIdentifier: String?,
        name: String,
        trackingType: TrackingType,
        attributes: CustomAttributesCompat,
        listener: TaskResultListener<QueueTaskResult>?,
    ) = addToQueue(
        profileIdentifier = profileIdentifier,
        activity = when (trackingType) {
            TrackingType.EVENT -> Activity.Event(
                timestamp = dateTimeUtil.nowUnixTimestamp,
                attributes = attributes.fix(),
                name = name,
            )
            TrackingType.PAGE -> Activity.Page(
                timestamp = dateTimeUtil.nowUnixTimestamp,
                attributes = attributes.fix(),
                name = name,
            )
            TrackingType.SCREEN -> Activity.Screen(
                timestamp = dateTimeUtil.nowUnixTimestamp,
                attributes = attributes.fix(),
                name = name,
            )
        },
        listener = listener,
    )

    override fun queueRegisterDevice(
        profileIdentifier: String?,
        device: Device,
        listener: TaskResultListener<QueueTaskResult>?,
    ) = addToQueue(
        profileIdentifier = profileIdentifier,
        activity = Activity.AddDevice(
            timestamp = dateTimeUtil.nowUnixTimestamp,
            device = device.copy(
                platform = platform.name,
            ),
        ),
        listener = listener,
    )

    override fun queueDeletePushToken(
        profileIdentifier: String?,
        deviceToken: String,
        listener: TaskResultListener<QueueTaskResult>?,
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
        listener: TaskResultListener<QueueTaskResult>?,
    ) = addToQueue(
        profileIdentifier = null,
        activity = Activity.Metric(
            metricEvent = event,
            deliveryId = deliveryId,
            deviceToken = deviceToken,
            timestamp = dateTimeUtil.nowUnixTimestamp,
        ),
        listener = listener,
    )

    override fun queueTrackInAppMetric(
        deliveryId: String,
        event: MetricEvent,
        listener: TaskResultListener<QueueTaskResult>?,
    ) = addToQueue(
        profileIdentifier = null,
        activity = Activity.Metric(
            metricEvent = event,
            deliveryId = deliveryId,
            deviceToken = null,
            timestamp = dateTimeUtil.nowUnixTimestamp,
        ),
        listener = listener,
    )

    private fun addToQueue(
        profileIdentifier: String?,
        activity: Activity,
        listener: TaskResultListener<QueueTaskResult>? = null,
    ) {
        queryHelper.insertTask(
            task = Task(
                profileIdentifier = profileIdentifier,
                identityType = workspace.identityType,
                activity = activity,
            ),
            listener = { result ->
                queueWorker.checkForPendingTasks(source = QueueTriggerSource.DATABASE)
                listener?.onComplete(result)
            },
        )
    }

    override fun sendAllPending() {
        queueWorker.sendAllPending()
    }

    override fun deleteExpiredTasks() {
        queryHelper.clearAllExpiredTasks()
    }
}
