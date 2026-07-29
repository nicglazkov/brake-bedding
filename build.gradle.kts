// AGP 9 has built-in Kotlin support, so the org.jetbrains.kotlin.android plugin is no
// longer applied here; AGP rejects it outright. The Compose and serialization compiler
// plugins are still applied separately.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
