plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.truthmodeshots.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.truthmodeshots.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}


dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}
