package io.customer.shared.tracking.api.model

import kotlinx.serialization.SerialName

/**
 * To identify modifying operation type by server.
 */
@kotlinx.serialization.Serializable
enum class TrackingRequestType {
    @SerialName("object")
    OBJECT,

    @SerialName("person")
    PERSON,

    @SerialName("delivery")
    DELIVERY,
}
