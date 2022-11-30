package io.customer.shared.util

import io.customer.shared.di.SDKComponent
import io.customer.shared.serializer.CustomAttributeContextualSerializer
import io.customer.shared.tracking.api.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Abstract way to deserialize JSON strings without thinking about the library used.
 */
interface JsonAdapter {
    fun configureKtorJson(
        configuration: Configuration,
        contentType: ContentType = ContentType.Application.Json,
    )

    @Throws(Exception::class)
    fun <T : Any> toJSON(kClazz: KClass<T>, content: T): String
    fun <T : Any> toJSONOrNull(kClazz: KClass<T>, content: T): String?

    @Throws(Exception::class)
    fun <T : Any> fromJSON(kClazz: KClass<T>, json: String): T
    fun <T : Any> fromJSONOrNull(kClazz: KClass<T>, json: String): T?
}

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
internal class JsonAdapterImpl(
    private val logger: Logger,
    private val sdkComponent: SDKComponent,
) : JsonAdapter {
    private val parser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        serializersModule = SerializersModule {
            contextual(
                CustomAttributeContextualSerializer(
                    logger = logger,
                    serializer = sdkComponent.customAttributeSerializer,
                ),
            )
        }
    }

    override fun configureKtorJson(configuration: Configuration, contentType: ContentType) {
        configuration.serialization(contentType, parser)
    }

    @Throws(Exception::class)
    override fun <T : Any> toJSON(kClazz: KClass<T>, content: T): String {
        return parser.encodeToString(kClazz.serializer(), content)
    }

    override fun <T : Any> toJSONOrNull(kClazz: KClass<T>, content: T): String? {
        return kotlin.runCatching { toJSON(kClazz = kClazz, content = content) }.getOrNull()
    }

    @Throws(Exception::class)
    override fun <T : Any> fromJSON(kClazz: KClass<T>, json: String): T {
        return parser.decodeFromString(kClazz.serializer(), json)
    }

    override fun <T : Any> fromJSONOrNull(kClazz: KClass<T>, json: String): T? {
        return kotlin.runCatching { fromJSON(kClazz = kClazz, json = json) }.getOrNull()
    }
}
