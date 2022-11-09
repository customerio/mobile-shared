package io.customer.shared.tracking.model

/**
 * Data class to hold tracking event attributes to track.
 *
 * @property profileIdentifier the id used for identifying user profile/
 * @property activity tracking activity details.
 */
internal data class Task(
    val profileIdentifier: String?,
    val activity: Activity,
)
