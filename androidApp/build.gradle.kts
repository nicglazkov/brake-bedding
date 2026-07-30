import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * The release signature data comes from a keystore.properties file. That file is not
 * in the repository. The project also builds without the file. Then only the release
 * variant has no signature. Because of this, clones and CI operate without the data
 * that only the maintainer has.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasSigningConfig = keystoreProperties.getProperty("storeFile") != null

android {
    namespace = "com.glazkov.brakebedding"

    // Current AndroidX makes API 37 necessary for the compilation. The targetSdk
    // stays at 36. This is intentional. The targetSdk turns on new system behavior,
    // and 36 is the newest level that an available emulator can test.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.glazkov.brakebedding"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "2.2"
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // The targetSdk stays one level below the compileSdk. Refer to the note at
        // compileSdk above.
        disable += "OldTargetApi"
        // Lint reports mipmap-anydpi-v26 as unnecessary at minSdk 26. It recommends
        // mipmap-anydpi. But AAPT does not find an <adaptive-icon> in a folder
        // without the version, and the build stops.
        disable += "ObsoleteSdkInt"
        warningsAsErrors = true
        abortOnError = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
}
