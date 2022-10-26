package com.customerio.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform