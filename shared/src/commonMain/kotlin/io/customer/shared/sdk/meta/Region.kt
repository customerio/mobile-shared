package io.customer.shared.sdk.meta

/**
 * Region that your Customer.io Workspace is located in.
 *
 * The SDK will route traffic to the correct data center location depending on the [Region] that
 * you use.
 */
sealed class Region(val code: String, internal val trackingURL: String) {
    // Note: These URLs are meant to be used specifically by the official
    // mobile SDKs. View our API docs: https://customer.io/docs/api/
    // to find the correct hostname for what you're trying to do.
    object US : Region(code = REGION_CODE_US, trackingURL = "https://track-sdk.customer.io/")
    object EU : Region(code = REGION_CODE_EU, trackingURL = "https://track-sdk-eu.customer.io/")

    companion object {
        private const val REGION_CODE_US = "us"
        private const val REGION_CODE_EU = "eu"

        /**
         * Helper method to create [Region] from raw values.
         *
         * @param code raw string containing region code (case insensitive).
         * @return [Region] created from code.
         */
        fun fromRawValue(code: String): Region = when (code.lowercase()) {
            REGION_CODE_EU.lowercase() -> EU
            else -> US
        }
    }
}
