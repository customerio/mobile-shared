package io.customer.shared.tracking.api

import io.customer.shared.device.UserAgentStore
import io.customer.shared.sdk.config.NetworkConfig
import io.customer.shared.sdk.meta.Region
import io.customer.shared.sdk.meta.Workspace
import io.customer.shared.util.LogLevel
import io.customer.shared.util.Logger
import io.ktor.util.*

/**
 * Helper class to provide http client params from a single place.
 */
internal interface HttpClientBuilder {
    val requestTimeoutMillis: Long
    val clientLogLevel: io.ktor.client.plugins.logging.LogLevel
    val clientLogger: io.ktor.client.plugins.logging.Logger

    /**
     * Returns trackingApiUrl provided in config; fallbacks to tracking url from [Region].
     */
    val baseURL: String

    /**
     * The headers provided will be automatically included in all HTTP requests made using the
     * client.
     */
    val headers: Map<String, String>
}

internal class HttpClientBuilderImpl(
    private val logger: Logger,
    private val sdkLogLevel: LogLevel,
    private val workspace: Workspace,
    private val networkConfig: NetworkConfig,
    private val userAgentStore: UserAgentStore,
) : HttpClientBuilder {
    override val requestTimeoutMillis: Long
        get() = networkConfig.requestTimeoutMillis

    override val clientLogLevel: io.ktor.client.plugins.logging.LogLevel
        get() = when (sdkLogLevel) {
            LogLevel.NONE -> io.ktor.client.plugins.logging.LogLevel.NONE
            LogLevel.FATAL,
            LogLevel.ERROR,
            LogLevel.WARN,
            LogLevel.INFO,
            -> io.ktor.client.plugins.logging.LogLevel.INFO
            LogLevel.DEBUG -> io.ktor.client.plugins.logging.LogLevel.ALL
        }

    override val clientLogger: io.ktor.client.plugins.logging.Logger
        get() = object : io.ktor.client.plugins.logging.Logger {
            override fun log(message: String) {
                logger.debug(message = message)
            }
        }

    override val baseURL: String
        get() = networkConfig.trackingApiUrl ?: workspace.region.trackingURL

    override val headers: Map<String, String>
        get() = mapOf(
            "Content-Type" to "application/json; charset=utf-8",
            "Authorization" to getAuthHeader(),
            "User-Agent" to userAgentStore.buildUserAgent(),
        )

    private fun getAuthHeader(): String {
        val apiKey = workspace.apiKey
        val siteId = workspace.siteId
        val rawHeader = "$siteId:$apiKey"
        return "Basic ${rawHeader.encodeBase64()}"
    }
}
