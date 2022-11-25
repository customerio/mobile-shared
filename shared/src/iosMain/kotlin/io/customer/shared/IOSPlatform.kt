package io.customer.shared

import platform.UIKit.UIDevice

class IOSPlatform : NativePlatform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual typealias Platform = IOSPlatform
