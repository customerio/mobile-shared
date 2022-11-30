@file:UseContextualSerialization(Any::class)

package io.customer.shared.tracking.model

import io.customer.shared.common.CustomAttributes
import io.customer.shared.tracking.api.model.Device
import io.customer.shared.tracking.constant.ActivityType
import io.customer.shared.tracking.constant.MetricEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.UseContextualSerialization

/**
 * Activity class to hold events for tracking. The class is sealed so only defined events can be
 * tracked. Any new type supported should be added to this class.
 *
 * All inherited class should define their type in [SerialName] annotation to override type in
 * serialization. See polymorphism in serialization for more details.
 *
 * @see [polymorphism in serialization][https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#custom-subclass-serial-name]
 */
@kotlinx.serialization.Serializable
internal sealed interface Activity {
    @SerialName("timestamp")
    val timestamp: Long?

    @SerialName("attributes")
    val attributes: CustomAttributes?

    /**
     * Model version can help parsing json safely when migrating to newer versions of Activity models
     */
    @SerialName("modelVersion")
    val modelVersion: Long

    /**
     * Merges similar activities to a single object. This is helpful when we receive similar events
     * in a shorter span and want to merge them together before sending to the server.
     *
     * The receiver takes higher priority and overrides any value in the passed activity where
     * needed.
     */
    fun merge(other: Activity): Activity

    @kotlinx.serialization.Serializable
    @SerialName(ActivityType.ADD_DEVICE)
    data class AddDevice(
        override val timestamp: Long,
        @SerialName("device") val device: Device,
    ) : Activity {
        override val modelVersion: Long = 1
        override val attributes: CustomAttributes? = null

        override fun merge(other: Activity) = copy()
    }

    @kotlinx.serialization.Serializable
    @SerialName(ActivityType.DELETE_DEVICE)
    data class DeleteDevice(
        val device: Device,
    ) : Activity {
        override val modelVersion: Long = 1
        override val timestamp: Long? = null
        override val attributes: CustomAttributes? = null

        override fun merge(other: Activity) = copy()
    }

    @kotlinx.serialization.Serializable
    @SerialName(ActivityType.IDENTIFY)
    data class IdentifyProfile(
        override val timestamp: Long,
        override val attributes: CustomAttributes = emptyMap(),
    ) : Activity {
        override val modelVersion: Long = 1

        override fun merge(other: Activity) = copy(
            attributes = attributes.merge(other = other.attributes),
        )
    }

    @kotlinx.serialization.Serializable
    @SerialName(ActivityType.EVENT)
    data class Event(
        val name: String,
        override val timestamp: Long,
        override val attributes: CustomAttributes = emptyMap(),
    ) : Activity {
        override val modelVersion: Long = 1

        override fun merge(other: Activity) = copy(
            attributes = attributes.merge(other = other.attributes),
        )
    }

    @kotlinx.serialization.Serializable
    @SerialName(ActivityType.PAGE)
    data class Page(
        val name: String,
        override val timestamp: Long,
        override val attributes: CustomAttributes = emptyMap(),
    ) : Activity {
        override val modelVersion: Long = 1

        override fun merge(other: Activity) = copy(
            attributes = attributes.merge(other = other.attributes),
        )
    }

    @kotlinx.serialization.Serializable
    @SerialName(ActivityType.SCREEN)
    data class Screen(
        val name: String,
        override val timestamp: Long,
        override val attributes: CustomAttributes = emptyMap(),
    ) : Activity {
        override val modelVersion: Long = 1

        override fun merge(other: Activity) = copy(
            attributes = attributes.merge(other = other.attributes),
        )
    }

    @kotlinx.serialization.Serializable
    @SerialName(ActivityType.METRIC)
    data class Metric(
        val metricEvent: MetricEvent,
        val deliveryId: String,
        val deviceToken: String?,
        override val timestamp: Long,
        override val attributes: CustomAttributes = emptyMap(),
    ) : Activity {
        override val modelVersion: Long = 1

        override fun merge(other: Activity) = copy(
            attributes = attributes.merge(other = other.attributes),
        )
    }
}
