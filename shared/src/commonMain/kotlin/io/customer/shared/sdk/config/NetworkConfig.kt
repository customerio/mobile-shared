package io.customer.shared.sdk.config

import io.customer.shared.sdk.meta.Region

/**
 * Configurations class to hold network settings.
 */
class NetworkConfig private constructor(
    val requestTimeoutMillis: Long,
    val trackingApiUrl: String?,
) {
    class Builder {
        private var requestTimeoutMillis: Long = DefaultValue.API_REQUEST_TIMEOUT
        private var trackingApiUrl: String? = null

        /**
         * The request timeout used when calling Customer.io API. Modifying this value may not be
         * required most often as the default value works ideal in most cases.
         */
        fun setRequestTimeoutMillis(requestTimeoutMillis: Long) {
            this.requestTimeoutMillis = requestTimeoutMillis
        }

        /**
         * Base URL to use for the Customer.io track API. You will more then likely not modify this value.
         * If you override this value, [Region] set when initializing the SDK will be ignored.
         */
        fun setTrackingApiUrl(trackingApiUrl: String) {
            this.trackingApiUrl = trackingApiUrl
        }

        fun build() = NetworkConfig(
            requestTimeoutMillis = requestTimeoutMillis,
            trackingApiUrl = trackingApiUrl,
        )
    }

    companion object {
        internal fun default() = Builder().build()
    }

    /**
     * Constant class to hold default values.
     */
    internal object DefaultValue {
        const val API_REQUEST_TIMEOUT = 30_000L
    }
}
