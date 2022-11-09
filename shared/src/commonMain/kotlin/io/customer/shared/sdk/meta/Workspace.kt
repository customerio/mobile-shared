package io.customer.shared.sdk.meta

/**
 * Class to hold workspace settings for client.
 *
 * @property client source client SDK.
 * @property siteId the site id to be used as api authentication.
 * @property apiKey the api key to be used as api authentication.
 * @property region the region where Customer.io workspace is located in.
 */
data class Workspace(
    val client: Client,
    val siteId: String,
    val apiKey: String,
    val region: Region,
)
