package com.glazkov.brakebedding.platform

/**
 * The small set of functions that each platform supplies. All other code is common.
 */

/** A time value in milliseconds from the monotonic clock. It never moves back. */
expect fun monotonicMillis(): Long

/** A new random UUID as a string. */
expect fun randomUuid(): String

/**
 * Formats [value] with exactly [decimals] fraction digits, in the locale of the user.
 * Do not parse the result. For values that the app parses again, use [fixedDecimal].
 */
expect fun localizedDecimal(value: Double, decimals: Int): String

/** The version name of the installed app, for the About section. */
expect fun appVersionName(): String

/** Opens [url] in the system browser. */
expect fun openWebPage(url: String)

/** The addresses of the project pages. One constant object keeps the links correct. */
object Links {
    const val WEBSITE = "https://nicglazkov.github.io/brake-bedding/"
    const val PRIVACY = "https://nicglazkov.github.io/brake-bedding/privacy.html"
    const val SUPPORT = "https://nicglazkov.github.io/brake-bedding/support.html"
    const val SOURCE = "https://github.com/nicglazkov/brake-bedding"
}

/**
 * Formats [value] with [decimals] fraction digits and a dot separator, independent of
 * the locale. The result is safe to parse again.
 */
fun fixedDecimal(value: Double, decimals: Int): String {
    if (decimals <= 0) {
        return kotlin.math.round(value).toLong().toString()
    }
    var scale = 1L
    repeat(decimals) { scale *= 10 }
    val scaled = kotlin.math.round(value * scale).toLong()
    val sign = if (scaled < 0) "-" else ""
    val abs = kotlin.math.abs(scaled)
    val whole = abs / scale
    val fraction = (abs % scale).toString().padStart(decimals, '0')
    return "$sign$whole.$fraction"
}
