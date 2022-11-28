package io.customer.shared.util

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
