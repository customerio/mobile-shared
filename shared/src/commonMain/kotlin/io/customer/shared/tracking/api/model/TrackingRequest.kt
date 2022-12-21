/**
 * Kotlin serialization does not allow parsing of generic objects without custom/contextual
 * serializers.
 *
 * See CustomAttributeContextualSerializer for more details on this issue.
 */
@file:UseContextualSerialization(Any::class)

package io.customer.shared.tracking.api.model

import io.customer.shared.common.CustomAttributes
import io.customer.shared.sdk.meta.IdentityType
import io.customer.shared.tracking.model.Activity
import io.customer.shared.tracking.model.type
import kotlinx.serialization.SerialName
import kotlinx.serialization.UseContextualSerialization

/**
 * Request class to be used for individual tracking requests.
 */
// TODO: Find ways to break down into smaller classes and overcome kotlin type issue in serialization
// of sealed classes with generic parsing
@kotlinx.serialization.Serializable
internal class TrackingRequest(
    @SerialName("type") val type: String,
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("identifiers") val identifiers: CustomAttributes? = null,
    @SerialName("attributes") val attributes: CustomAttributes? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("device") val device: Device? = null,
    @SerialName("metric") val metric: Metric? = null,
)

internal fun Activity.toTrackingRequest(
    identityType: IdentityType,
    profileIdentifier: String,
) = when (this) {
    is Activity.IdentifyProfile -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
    )
    is Activity.AddDevice -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        device = device,
    )
    is Activity.DeleteDevice -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        device = device,
    )
    is Activity.Metric -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        metric = Metric(
            deliveryID = deliveryId,
            deviceToken = deviceToken,
            event = metricEvent,
            timestamp = timestamp,
        ),
    )
    is Activity.Page -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        name = name,
    )
    is Activity.Screen -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        name = name,
    )
    is Activity.Event -> TrackingRequest(
        identityType = identityType,
        profileIdentifier = profileIdentifier,
        name = name,
    )
}

private fun Activity.TrackingRequest(
    identityType: IdentityType,
    profileIdentifier: String,
    name: String? = null,
    device: Device? = null,
    metric: Metric? = null,
): TrackingRequest {
    val profileIdentifierKey = when (identityType) {
        IdentityType.CIO_ID -> "cio_id"
        IdentityType.ID -> "id"
        IdentityType.EMAIL -> "email"
    }

    return TrackingRequest(
        type = type,
        timestamp = timestamp,
        identifiers = mapOf(profileIdentifierKey to profileIdentifier),
        attributes = attributes,
        name = name,
        device = device,
        metric = metric,
    )
}
