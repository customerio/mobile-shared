package io.customer.shared.util

import io.customer.shared.tracking.api.*
import io.customer.shared.tracking.model.Activity
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.modules.*

/**
 * File to hold extensions for [JsonAdapter] to make parsing easier.
 */

@Throws(Exception::class)
internal fun JsonAdapter.parseToActivity(json: String): Activity {
    return fromJSON(Activity::class, json)
}

@Throws(Exception::class)
internal fun JsonAdapter.parseToString(activity: Activity): String {
    return toJSON(kClazz = Activity::class, content = activity)
}
