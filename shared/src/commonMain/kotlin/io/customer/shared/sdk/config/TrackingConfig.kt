package io.customer.shared.sdk.config

/**
 * Configurations to be used by tracking module.
 */
class TrackingConfig private constructor(
    val autoTrackScreenViews: Boolean,
    val autoTrackDeviceAttributes: Boolean,
) : ModuleConfig {
    override val key: String = NAME

    class Builder : ModuleConfig.Builder<TrackingConfig> {
        private var autoTrackScreenViews: Boolean = DefaultValue.AUTO_TRACK_SCREEN_VIEWS
        private var autoTrackDeviceAttributes: Boolean = DefaultValue.AUTO_TRACK_DEVICE_ATTRIBUTES

        /**
         * Indicates whether screens views should be tracked automatically or not
         */
        fun setAutoTrackScreenViews(autoTrackScreenViews: Boolean) {
            this.autoTrackScreenViews = autoTrackScreenViews
        }

        /**
         * Indicates whether device attributes should be tracked automatically or not
         */
        fun setAutoTrackDeviceAttributes(autoTrackDeviceAttributes: Boolean) {
            this.autoTrackDeviceAttributes = autoTrackDeviceAttributes
        }

        override fun build() = TrackingConfig(
            autoTrackScreenViews = autoTrackScreenViews,
            autoTrackDeviceAttributes = autoTrackDeviceAttributes,
        )
    }

    companion object {
        internal const val NAME = "TRACKING"
        internal fun default() = Builder().build()
    }

    /**
     * Constant class to hold default values.
     */
    internal object DefaultValue {
        const val AUTO_TRACK_SCREEN_VIEWS = true
        const val AUTO_TRACK_DEVICE_ATTRIBUTES = true
    }
}
