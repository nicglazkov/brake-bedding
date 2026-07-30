package com.glazkov.brakebedding.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSUUID
import platform.QuartzCore.CACurrentMediaTime

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
