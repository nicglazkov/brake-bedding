// AGP 9 contains the Kotlin support. Because of this, the project does not apply the
// org.jetbrains.kotlin.android plugin. AGP refuses that plugin. The Compose compiler
// plugin and the serialization plugin stay separate.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
