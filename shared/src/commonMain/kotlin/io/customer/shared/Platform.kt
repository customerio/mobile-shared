package io.customer.shared

/**
 * Class to hold platform wide properties.
 */
interface NativePlatform {
    val name: String
}

/**
 * This simplifies usage of [NativePlatform] in platforms by eliminating the need of unnecessary
 * casting.
 */
expect class Platform : NativePlatform
