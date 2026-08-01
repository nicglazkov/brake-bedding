package com.glazkov.brakebedding.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.QuartzCore.CACurrentMediaTime
import platform.UIKit.UIApplication

actual fun monotonicMillis(): Long = (CACurrentMediaTime() * 1000.0).toLong()

actual fun randomUuid(): String = NSUUID().UUIDString

actual fun localizedDecimal(value: Double, decimals: Int): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        minimumFractionDigits = decimals.toULong()
        maximumFractionDigits = decimals.toULong()
        usesGroupingSeparator = false
    }
    return formatter.stringFromNumber(NSNumber(double = value)) ?: fixedDecimal(value, decimals)
}

actual fun appVersionName(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "?"

actual fun openWebPage(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
}
