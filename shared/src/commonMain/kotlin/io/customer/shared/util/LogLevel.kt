package io.customer.shared.util

/**
 * Enum class to differentiate between log types and making it easier to filter.
 */
enum class LogLevel {
    NONE,

    /**
     * This is same as error, but these are the issues that we don't expect to happen in any scenario.
     * We only catch and log them to prevent crashes. In near future, we'll find ways to extract them
     * automatically to help us identify bugs in the SDK.
     */
    FATAL,
    ERROR,
    WARN,
    INFO,
    DEBUG,
}

/**
 * Determines whether should we log the given log level or skip it
 *
 * @param levelForMessage log level of the message to log
 * @return true if we should log the message; false otherwise
 */
internal fun LogLevel.shouldLog(levelForMessage: LogLevel): Boolean {
    return this.ordinal >= levelForMessage.ordinal
}
