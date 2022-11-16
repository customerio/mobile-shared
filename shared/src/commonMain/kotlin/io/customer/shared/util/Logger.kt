package io.customer.shared.util

/**
 * Basic logging interface to print desired messages.
 */
interface Logger {
    // Log level to filter unwanted messages in debug and release environments
    var logLevel: LogLevel

    fun fatal(message: String)
    fun error(message: String)
    fun warn(message: String)
    fun info(message: String)
    fun debug(message: String)
}

/**
 * Expects the default log level to start with, should use default value where no changes are required.
 */
internal expect val logLevelDefault: LogLevel

/**
 * Platform specific helper method used by default [ConsoleLogger] implementation to allow printing
 * using native apis without providing complete [Logger] implementation.
 */
expect fun writeLogMessage(logLevel: LogLevel, tag: String, message: String)

/**
 * Default basic [Logger] implementation to route logs to native loggers conveniently.
 */
internal class ConsoleLogger : Logger {
    // By default starts with debug logs for dev environments; fallbacks to default value for release builds
    override var logLevel: LogLevel = logLevelDefault

    override fun fatal(message: String) = writeLogMessage(LogLevel.FATAL, message)
    override fun error(message: String) = writeLogMessage(LogLevel.ERROR, message)
    override fun warn(message: String) = writeLogMessage(LogLevel.WARN, message)
    override fun info(message: String) = writeLogMessage(LogLevel.INFO, message)
    override fun debug(message: String) = writeLogMessage(LogLevel.DEBUG, message)

    private fun writeLogMessage(levelForMessage: LogLevel, message: String) {
        if (logLevel.shouldLog(levelForMessage)) {
            writeLogMessage(logLevel = levelForMessage, tag = TAG, message = message)
        }
    }

    companion object {
        const val TAG = "[CIO]-[KMM]"
    }
}
