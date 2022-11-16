package io.customer.shared.util

import io.customer.shared.sdk.config.CustomerIOConfig

/**
 * Since there is no way at runtime to determine if a build is debuggable or not in iOS, it always
 * starts with default log level.
 */
internal actual val logLevelDefault: LogLevel
    get() = CustomerIOConfig.DefaultValue.LOG_LEVEL

actual fun writeLogMessage(logLevel: LogLevel, tag: String, message: String) {
    when (logLevel) {
        LogLevel.NONE -> {}
        LogLevel.FATAL -> println(message)
        LogLevel.ERROR -> println(message)
        LogLevel.WARN -> println(message)
        LogLevel.INFO -> println(message)
        LogLevel.DEBUG -> println(message)
    }
}
