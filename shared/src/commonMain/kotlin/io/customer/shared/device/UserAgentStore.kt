package io.customer.shared.device

interface UserAgentStore {
    /**
     * buildUserAgent - To get `user-agent` header value. This value depends on SDK version
     * and device detail such as OS version, device model, customer's app name etc
     *
     * If the device and OS information is available, it should return in following format :
     * `Customer.io Android Client/1.0.0-alpha.6 (Google Pixel 6; 30) User App/1.0`
     *
     * Otherwise return SDK info only
     * `Customer.io Android Client/1.0.0-alpha.6`
     */
    fun buildUserAgent(): String
}
