package io.customer.shared.sdk.meta

/**
 * Class to hold workspace settings for client.
 *
 * @property siteId the site id to be used as api authentication.
 * @property apiKey the api key to be used as api authentication.
 * @property region the region where Customer.io workspace is located in.
 * @property client source client SDK.
 * @property identityType the type of identifier used by client app.
 */
data class Workspace constructor(
    val siteId: String,
    val apiKey: String,
    val region: Region = DefaultValue.REGION,
    val client: Client,
    val identityType: IdentityType = DefaultValue.IDENTITY_TYPE,
) {
    constructor(
        siteId: String,
        apiKey: String,
        client: Client,
    ) : this(
        siteId = siteId,
        apiKey = apiKey,
        region = DefaultValue.REGION,
        client = client,
        identityType = DefaultValue.IDENTITY_TYPE,
    )

    constructor(
        siteId: String,
        apiKey: String,
        region: Region,
        client: Client,
    ) : this(
        siteId = siteId,
        apiKey = apiKey,
        region = region,
        client = client,
        identityType = DefaultValue.IDENTITY_TYPE,
    )

    /**
     * Default values make it easier to reuse when providing fallback values in wrapper SDKs or
     * auto initializing the SDK.
     */
    internal object DefaultValue {
        val IDENTITY_TYPE = IdentityType.ID
        val REGION = Region.US
    }
}
