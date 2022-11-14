package io.customer.shared.util

import android.util.Log

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
