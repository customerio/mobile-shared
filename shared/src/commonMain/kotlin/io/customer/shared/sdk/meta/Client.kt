package io.customer.shared.sdk.meta

/**
 * Class to hold information about the source SDK being used by the client app.
 *
 * @property source name of the client to append with user-agent.
 * @property sdkVersion version of the SDK used.
 */
sealed class Client(
    val source: String,
    val sdkVersion: String,
) {
    override fun toString(): String = "$source Client/$sdkVersion"

    /**
     * Simpler class for Android clients.
     */
    class Android(sdkVersion: String) : Client(source = SOURCE_ANDROID, sdkVersion = sdkVersion)

    /**
     * Simpler class for iOS clients.
     */
    class IOS(sdkVersion: String) : Client(source = SOURCE_IOS, sdkVersion = sdkVersion)

    /**
     * Simpler class for ReactNative clients.
     */
    class ReactNative(sdkVersion: String) : Client(
        source = SOURCE_REACT_NATIVE,
        sdkVersion = sdkVersion
    )

    /**
     * Simpler class for Expo clients.
     */
    class Expo(sdkVersion: String) : Client(source = SOURCE_EXPO, sdkVersion = sdkVersion)

    /**
     * Simpler class for Flutter clients.
     */
    class Flutter(sdkVersion: String) : Client(source = SOURCE_FLUTTER, sdkVersion = sdkVersion)

    /**
     * Other class allows adding custom sources for clients that are not supported above.
     *
     * Use this only if the client platform is not available in the above list.
     */
    class Other internal constructor(
        source: String,
        sdkVersion: String,
    ) : Client(source = source, sdkVersion = sdkVersion)

    companion object {
        internal const val SOURCE_ANDROID = "Android"
        internal const val SOURCE_IOS = "iOS"
        internal const val SOURCE_REACT_NATIVE = "ReactNative"
        internal const val SOURCE_EXPO = "Expo"
        internal const val SOURCE_FLUTTER = "Flutter"

        /**
         * Helper method to create [Client] from raw values.
         *
         * @param source raw string of client source (case insensitive).
         * @param sdkVersion version of the SDK used.
         * @return [Client] created from provided values.
         */
        fun fromRawValue(source: String, sdkVersion: String) = when (source.lowercase()) {
            SOURCE_ANDROID.lowercase() -> Android(sdkVersion = sdkVersion)
            SOURCE_IOS.lowercase() -> IOS(sdkVersion = sdkVersion)
            SOURCE_REACT_NATIVE.lowercase() -> ReactNative(sdkVersion = sdkVersion)
            SOURCE_EXPO.lowercase() -> Expo(sdkVersion = sdkVersion)
            SOURCE_FLUTTER.lowercase() -> Flutter(sdkVersion = sdkVersion)
            else -> Other(source = source, sdkVersion = sdkVersion)
        }
    }
}
