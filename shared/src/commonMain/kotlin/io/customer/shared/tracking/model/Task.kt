package io.customer.shared.tracking.model

import io.customer.shared.sdk.meta.IdentityType

/**
 * Data class to hold tracking event attributes to track.
 *
 * @property profileIdentifier the id used for identifying user profile
 * @property identityType type of user profile identifier
 * @property activity tracking activity details.
 */
internal data class Task(
    val profileIdentifier: String?,
    val identityType: IdentityType,
    val activity: Activity,
)
