plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.glazkov.brakebedding.shared"
        compileSdk = 37
        minSdk = 26

        // The common tests run on the JVM host with this, in addition to the iOS
        // simulator test task.
        withHostTest {}
    }

    // The two device targets for the iOS app. Each makes a framework that the Xcode
    // project consumes.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    // The tests run on a simulator with this exact name. Other agents on this machine
    // use their own simulators; a pinned device prevents a collision.
    iosSimulatorArm64().testRuns.configureEach {
        deviceId = "BrakeBedding-Sim"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)

            implementation(libs.jb.lifecycle.runtime.compose)
            implementation(libs.jb.lifecycle.viewmodel.compose)
            implementation(libs.jb.navigation.compose)

            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
        }
    }
}
