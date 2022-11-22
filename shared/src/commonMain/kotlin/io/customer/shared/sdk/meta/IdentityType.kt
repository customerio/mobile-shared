package io.customer.shared.sdk.meta

/**
 * Specifies the type by which client app identifies its user when calling the identify method from
 * SDK. This property is set sdk wide and passed to server to make sure attributes are attached
 * correctly to user profiles.
 */
enum class IdentityType {
    /**
     * Let the server identify the type automatically. However, this option should be avoided.
     * Prefer specifying IdentityType explicitly.
     */
    AUTO_IDENTIFY,

    /**
     * Sets the user identifier as cio_id in user profile.
     */
    CIO_ID,

    /**
     * Sets the user identifier as id in user profile (this is not cio_id and can be any id from
     * client app).
     */
    ID,

    /**
     * Sets the user identifier as email in user profile.
     */
    EMAIL,
}
