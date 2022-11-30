package io.customer.shared.di

import io.customer.shared.database.*
import io.customer.shared.tracking.api.HttpClientBuilder
import io.customer.shared.tracking.api.HttpClientBuilderImpl
import io.customer.shared.tracking.api.TrackingHttpClient
import io.customer.shared.tracking.api.TrackingHttpClientImpl
import io.customer.shared.tracking.queue.BackgroundQueue
import io.customer.shared.tracking.queue.BackgroundQueueImpl
import io.customer.shared.tracking.queue.QueueWorker
import io.customer.shared.tracking.queue.QueueWorkerImpl
import io.customer.shared.util.JsonAdapter
import io.customer.shared.util.JsonAdapterImpl
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
    val sdkComponent: SDKComponent,
) : DIGraph() {
    constructor(sdkComponent: SDKComponent) : this(
        staticComponent = KMMStaticComponent(),
        sdkComponent = sdkComponent,
    )

    init {
        staticComponent.attachSDKConfig(config = sdkComponent.customerIOConfig)
    }

    val backgroundQueue: BackgroundQueue
        get() = getSingletonInstance {
            BackgroundQueueImpl(
                logger = staticComponent.logger,
                dateTimeUtil = staticComponent.dateTimeUtil,
                workspace = sdkComponent.customerIOConfig.workspace,
                platform = sdkComponent.platform,
                trackingTaskQueryHelper = trackingTaskQueryHelper,
                queueWorker = queueWorker,
            )
        }

    private val databaseDriverFactory: DatabaseDriverFactory
        get() = getSingletonInstance { getDatabaseDriverFactory(platform = sdkComponent.platform) }

    private val databaseHelper: DatabaseHelper
        get() = getSingletonInstance { DatabaseHelper(databaseDriverFactory = databaseDriverFactory) }

    internal val jsonAdapter: JsonAdapter
        get() = getSingletonInstance {
            JsonAdapterImpl(
                logger = staticComponent.logger,
                sdkComponent = sdkComponent,
            )
        }

    internal val trackingTaskQueryHelper: TrackingTaskQueryHelper
        get() = getSingletonInstance {
            TrackingTaskQueryHelperImpl(
                logger = staticComponent.logger,
                dateTimeUtil = staticComponent.dateTimeUtil,
                jsonAdapter = jsonAdapter,
                platformUtil = staticComponent.platformUtil,
                executor = staticComponent.coroutineExecutor,
                workspace = sdkComponent.customerIOConfig.workspace,
                backgroundQueueConfig = sdkComponent.customerIOConfig.backgroundQueue,
                trackingTaskQueries = databaseHelper.trackingTaskQueries,
            )
        }

    internal val queueWorker: QueueWorker
        get() = getSingletonInstance {
            QueueWorkerImpl(
                logger = staticComponent.logger,
                dateTimeUtil = staticComponent.dateTimeUtil,
                jsonAdapter = jsonAdapter,
                executor = staticComponent.coroutineExecutor,
                workspace = sdkComponent.customerIOConfig.workspace,
                backgroundQueueConfig = sdkComponent.customerIOConfig.backgroundQueue,
                trackingTaskQueryHelper = trackingTaskQueryHelper,
                trackingHttpClient = trackingHttpClient,
            )
        }

    internal val trackingHttpClient: TrackingHttpClient
        get() = getSingletonInstance {
            TrackingHttpClientImpl(
                logger = staticComponent.logger,
                httpClient = httpClient,
            )
        }

    private val httpClientBuilder: HttpClientBuilder
        get() = getNewInstance {
            HttpClientBuilderImpl(
                logger = staticComponent.logger,
                sdkLogLevel = sdkComponent.customerIOConfig.sdkLogLevel,
                workspace = sdkComponent.customerIOConfig.workspace,
                networkConfig = sdkComponent.customerIOConfig.network,
                userAgentStore = sdkComponent.userAgentStore,
            )
        }

    private val httpClient: HttpClient
        get() = getNewInstance {
            val requestBuilder = httpClientBuilder
            return@getNewInstance HttpClient {
                install(Logging) {
                    logger = requestBuilder.clientLogger
                    level = requestBuilder.clientLogLevel
                }
                install(ContentNegotiation) {
                    jsonAdapter.configureKtorJson(configuration = this)
                }
                defaultRequest {
                    url(urlString = requestBuilder.baseURL)
                    requestBuilder.headers.forEach { (key, value) -> header(key, value) }
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = requestBuilder.requestTimeoutMillis
                }
            }
        }
}
