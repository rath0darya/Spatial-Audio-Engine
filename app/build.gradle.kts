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
        versionCode = 1
        versionName = "0.1.0"
    }
}
