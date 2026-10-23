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
        versionCode = 5
        versionName = "0.3.2"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
}