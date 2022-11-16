package io.customer.shared

interface NativePlatform {
    val name: String
}

expect class Platform : NativePlatform
