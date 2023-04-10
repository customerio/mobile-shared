package io.customer.shared.sdk.meta

/**
 * Class to hold workspace settings for client.
 *
 * @property siteId the site id to be used as api authentication.
 * @property identityType the type of identifier used by client app.
 */
data class Workspace constructor(
    val siteId: String,
    val identityType: IdentityType = DefaultValue.IDENTITY_TYPE,
) {
    constructor(
        siteId: String,
    ) : this(
        siteId = siteId,
        identityType = DefaultValue.IDENTITY_TYPE,
    )

    /**
     * Default values make it easier to reuse when providing fallback values in wrapper SDKs or
     * auto initializing the SDK.
     */
    internal object DefaultValue {
        val IDENTITY_TYPE = IdentityType.ID
    }
}
