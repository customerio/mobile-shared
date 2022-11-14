package io.customer.shared.di

import io.customer.shared.Platform
import io.customer.shared.database.DatabaseManager
import io.customer.shared.database.DatabaseManagerImpl
import io.customer.shared.sdk.SDKCallback
import io.customer.shared.sdk.config.CustomerIOConfig
import io.customer.shared.sdk.config.basicAuthHeaderSting
import io.customer.shared.sdk.config.trackingApiHostname
import io.customer.shared.tracking.api.TrackingHttpClient
import io.customer.shared.tracking.api.TrackingHttpClientImpl
import io.customer.shared.tracking.queue.BackgroundQueue
import io.customer.shared.tracking.queue.BackgroundQueueImpl
import io.customer.shared.tracking.queue.QueueWorker
import io.customer.shared.tracking.queue.QueueWorkerImpl
import io.customer.shared.util.LogLevel
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*

/**
 * Workspace component dependency graph to satisfy workspace based dependencies from single place.
 *
 * The class should only contain dependencies matching the following criteria:
 * - dependencies that requires workspace information to be initialized.
 * - dependencies that should be reinitialized whenever workspace is switched.
 * - or any dependency that cannot fit in DIGraphs above this graph :)
 *
 * @see [KMMStaticComponent] for complete graph hierarchy.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class KMMComponent(
    private val staticComponent: KMMStaticComponent = KMMStaticComponent(),
    val platform: Platform,
    val sdkConfig: CustomerIOConfig,
    val sdkCallback: SDKCallback,
) : DIGraph() {
    private val platformComponent: KMMPlatformComponent = KMMPlatformComponent(
        staticComponent = staticComponent,
        platform = platform,
    )
    val backgroundQueue: BackgroundQueue
        get() = getSingletonInstance {
            BackgroundQueueImpl(
                logger = staticComponent.logger,
                platform = platform,
                dateTimeUtil = staticComponent.dateTimeUtil,
                databaseManager = databaseManager,
                queueWorker = queueWorker,
            )
        }

    internal val databaseManager: DatabaseManager
        get() = getSingletonInstance {
            DatabaseManagerImpl(
                logger = staticComponent.logger,
                dateTimeUtil = staticComponent.dateTimeUtil,
                jsonAdapter = staticComponent.jsonAdapter,
                backgroundQueueConfig = sdkConfig.backgroundQueue,
                workspace = sdkConfig.workspace,
                trackingTaskQueries = platformComponent.databaseHelper.trackingTaskQueries,
                workDispatcher = staticComponent.workDispatcher,
            )
        }

    internal val queueWorker: QueueWorker
        get() = getSingletonInstance {
            QueueWorkerImpl(
                logger = staticComponent.logger,
                dateTimeUtil = staticComponent.dateTimeUtil,
                jsonAdapter = staticComponent.jsonAdapter,
                backgroundQueueConfig = sdkConfig.backgroundQueue,
                databaseManager = databaseManager,
                trackingHttpClient = trackingHttpClient,
                workDispatcher = staticComponent.workDispatcher,
            )
        }

    internal val trackingHttpClient: TrackingHttpClient
        get() = getSingletonInstance {
            TrackingHttpClientImpl(
                logger = staticComponent.logger,
                httpClient = httpClient,
            )
        }

    private val httpClient: HttpClient
        get() = getNewInstance {
            HttpClient {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            staticComponent.logger.debug(message = message)
                        }
                    }
                    level = when (sdkConfig.logLevel) {
                        LogLevel.NONE -> io.ktor.client.plugins.logging.LogLevel.NONE
                        LogLevel.FATAL -> io.ktor.client.plugins.logging.LogLevel.INFO
                        LogLevel.ERROR -> io.ktor.client.plugins.logging.LogLevel.INFO
                        LogLevel.WARN -> io.ktor.client.plugins.logging.LogLevel.INFO
                        LogLevel.INFO -> io.ktor.client.plugins.logging.LogLevel.INFO
                        LogLevel.DEBUG -> io.ktor.client.plugins.logging.LogLevel.ALL
                    }
                }
                install(ContentNegotiation) {
                    staticComponent.jsonAdapter.configureKtorJson(configuration = this)
                }
                defaultRequest {
                    url(urlString = sdkConfig.trackingApiHostname)
                    header("Content-Type", "application/json; charset=utf-8")
                    header("Authorization", sdkConfig.basicAuthHeaderSting)
                    header("User-Agent", sdkCallback.buildUserAgent())
                }
                install(HttpTimeout) {
//                connectTimeoutMillis = configWrapper.tracking.requestTimeoutMillis
                    requestTimeoutMillis = sdkConfig.network.requestTimeoutMillis
//                socketTimeoutMillis = configWrapper.tracking.requestTimeoutMillis
                }
            }
        }
}
