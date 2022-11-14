package io.customer.shared.tracking.queue

import io.customer.shared.Platform
import io.customer.shared.common.CustomAttributesCompat
import io.customer.shared.common.QueueTaskResult
import io.customer.shared.common.fix
import io.customer.shared.database.DatabaseManager
import io.customer.shared.tracking.api.model.Device
import io.customer.shared.tracking.constant.MetricEvent
import io.customer.shared.tracking.constant.NamedActivityType
import io.customer.shared.tracking.model.Activity
import io.customer.shared.tracking.model.Task
import io.customer.shared.util.DateTimeUtil
import io.customer.shared.util.Logger

interface BackgroundQueue {
    fun queueIdentifyProfile(
        newIdentifier: String,
        oldIdentifier: String?,
        attributes: CustomAttributesCompat,
        listener: TaskResultListener<QueueTaskResult>? = null,
    )

    fun queueTrack(
        profileIdentifier: String?,
        name: String,
        activityType: NamedActivityType,
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

    suspend fun run()

    fun deleteExpiredTasks()
}

internal class BackgroundQueueImpl(
    private val logger: Logger,
    private val platform: Platform,
    private val dateTimeUtil: DateTimeUtil,
    private val databaseManager: DatabaseManager,
    private val queueWorker: QueueWorker,
) : BackgroundQueue {
    override fun queueIdentifyProfile(
        newIdentifier: String,
        oldIdentifier: String?,
        attributes: CustomAttributesCompat,
        listener: TaskResultListener<QueueTaskResult>?,
    ) {
        // If SDK previously identified profile X and X is being identified again, no use blocking the queue with a queue group.
        // TODO: Handle update attributes
        // If there was a previously identified profile, or, we are just adding attributes to an existing profile, we need to wait for
        // this operation until the previous identify runs successfully.
        // TODO: Handle update attributes

        if (oldIdentifier.isNullOrBlank()) {
            databaseManager.identifyProfile(identifier = newIdentifier)
        }

        return addToQueue(
            profileIdentifier = newIdentifier,
            activity = Activity.IdentifyProfile(
                timestamp = dateTimeUtil.nowUnixTimestamp,
            ),
            listener = listener,
        )
    }

    override fun queueTrack(
        profileIdentifier: String?,
        name: String,
        activityType: NamedActivityType,
        attributes: CustomAttributesCompat,
        listener: TaskResultListener<QueueTaskResult>?,
    ) = addToQueue(
        profileIdentifier = profileIdentifier,
        activity = Activity.Event(
            timestamp = dateTimeUtil.nowUnixTimestamp,
            attributes = attributes.fix(),
            name = name,
        ),
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
            deviceToken = "",
            timestamp = dateTimeUtil.nowUnixTimestamp,
        ),
        listener = listener,
    )

    private fun addToQueue(
        profileIdentifier: String?,
        activity: Activity,
        listener: TaskResultListener<QueueTaskResult>? = null,
    ) {
        databaseManager.insert(
            task = Task(
                profileIdentifier = profileIdentifier,
                activity = activity,
            ),
            listener = listener,
        )
        queueWorker.checkForPendingTasks(source = QueueTriggerSource.QUEUE)
    }

    override suspend fun run() {
        queueWorker.sendAllPending()
    }

    override fun deleteExpiredTasks() {
        databaseManager.clearAllExpired()
    }
}
