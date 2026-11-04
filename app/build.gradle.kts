plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "uz.ozkey.keyboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "uz.ozkey.keyboard"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
