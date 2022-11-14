package io.customer.shared.util

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
