package io.customer.shared

import android.content.Context

/**
 * Android platform class to hold Android specific properties e.g. [Context], etc.
 */
class AndroidPlatform(
    val applicationContext: Context,
) : NativePlatform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

actual typealias Platform = AndroidPlatform
