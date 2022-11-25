package io.customer.shared.util

import android.util.Log
import io.customer.shared.BuildConfig
import io.customer.shared.sdk.config.CustomerIOConfig

/**
 * Android platform starts with debug logs for dev environments; fallbacks to default value for
 * release builds.
 */
internal actual val logLevelDefault: LogLevel
    get() = if (BuildConfig.DEBUG) LogLevel.DEBUG
    else CustomerIOConfig.DefaultValue.LOG_LEVEL

/**
 * Android specific log helper for printing messages to Logcat.
 */
actual fun writeLogMessage(logLevel: LogLevel, tag: String, message: String) {
    when (logLevel) {
        LogLevel.NONE -> {}
        LogLevel.FATAL,
        LogLevel.ERROR,
        -> Log.e(tag, message)
        LogLevel.WARN -> Log.w(tag, message)
        LogLevel.INFO -> Log.i(tag, message)
        LogLevel.DEBUG -> Log.d(tag, message)
    }
}
