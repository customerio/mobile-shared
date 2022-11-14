package io.customer.shared.util

import io.customer.shared.tracking.api.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

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
internal class JsonAdapterImpl : JsonAdapter {
    private val parser = Json {
        explicitNulls = false
//        serializersModule = SerializersModule {
//            polymorphic(TrackingRequest::class) {
//                subclass(ActivityRequest::class)
//                subclass(DeviceRequest::class)
//                subclass(MetricRequest::class)
//                subclass(NamedActivityRequest::class)
//            }
//        }
    }

    override fun configureKtorJson(configuration: Configuration, contentType: ContentType) {
        configuration.serialization(contentType, parser)
    }

    @Throws(Exception::class)
    override fun <T : Any> toJSON(kClazz: KClass<T>, content: T): String {
        return parser.encodeToString(kClazz.serializer(), content)
    }

    override fun <T : Any> toJSONOrNull(kClazz: KClass<T>, content: T): String? = try {
        toJSON(kClazz = kClazz, content = content)
    } catch (ex: SerializationException) {
        null
    }

    @Throws(Exception::class)
    override fun <T : Any> fromJSON(kClazz: KClass<T>, json: String): T {
        return parser.decodeFromString(kClazz.serializer(), json)
    }

    override fun <T : Any> fromJSONOrNull(kClazz: KClass<T>, json: String): T? = try {
        fromJSON(kClazz = kClazz, json = json)
    } catch (ex: SerializationException) {
        null
    }
}
