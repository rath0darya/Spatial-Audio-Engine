plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.spatialaudio.engine"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.spatialaudio.engine"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    // Keep Java and Kotlin bytecode targets aligned; Gradle 8.9 rejects a mismatch.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}