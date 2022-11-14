package io.customer.shared.internal

/**
 * Marks declarations that should be used by only Customer.io classes. Client apps should avoid
 * using them as they may change without any announcement.
 */
@RequiresOptIn(
    message = "This is internal API for CustomerIO. Do not depend on this API in your own client code.",
    level = RequiresOptIn.Level.ERROR
)
annotation class InternalCustomerIOApi
