package io.customer.shared.sdk.config

/**
 * Configurations to hold background queue settings.
 */
class BackgroundQueueConfig private constructor(
    val batchDelayMinTasks: Int,
    val batchDelayMaxDelayInSeconds: Int,
    val batchTasksMax: Int,
) {
    class Builder {
        private var batchDelayMinTasks = DefaultValue.BATCH_THRESHOLD
        private var batchDelayMaxDelayInSeconds = DefaultValue.BATCH_DELAY_SECONDS
        private var batchTasksMax = DefaultValue.BATCH_LIMIT

        /**
         * Number of tasks in the background queue before the queue begins operating.
         * This is mostly used during development to test configuration is setup. We do not recommend
         * modifying this value because it impacts battery life of mobile device.
         */
        fun setBatchDelayMinTasks(batchDelayMinTasks: Int) = this.apply {
            this.batchDelayMinTasks = batchDelayMinTasks
        }

        /**
         * The number of seconds to delay running queue after a task has been added to it.
         * We do not recommend modifying this value because it impacts battery life of mobile device.
         */
        fun setBatchDelayMaxDelayInSeconds(batchDelayMaxDelayInSeconds: Int) = this.apply {
            this.batchDelayMaxDelayInSeconds = batchDelayMaxDelayInSeconds
        }

        /**
         * Number of maximum tasks in the background queue to batch.
         * This can result in slowing down the tracking API call. We do not recommend
         * modifying this value because the default value used works with out APIs.
         */
        fun setBatchTasksMax(batchTasksMax: Int) = this.apply {
            this.batchTasksMax = batchTasksMax
        }

        fun build() = BackgroundQueueConfig(
            batchDelayMinTasks = batchDelayMinTasks,
            batchDelayMaxDelayInSeconds = batchDelayMaxDelayInSeconds,
            batchTasksMax = batchTasksMax,
        )
    }

    companion object {
        internal fun default() = Builder().build()
    }

    /**
     * Default values make it easier to reuse when providing fallback values in wrapper SDKs or
     * auto initializing the SDK.
     */
    internal object DefaultValue {
        const val BATCH_THRESHOLD = 5
        const val BATCH_LIMIT = 30
        const val BATCH_DELAY_SECONDS = 30
    }
}
