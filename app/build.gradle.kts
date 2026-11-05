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
        versionCode = 7
        versionName = "0.6.0"
        // Tarjima serveri (backend/worker.js). gradle.properties ichida: ozkeyTranslateUrl=https://...
        val translateUrl = (project.findProperty("ozkeyTranslateUrl") as String?) ?: ""
        buildConfigField("String", "TRANSLATE_URL", "\"$translateUrl\"")
        // Qo'shimcha til paketlari ro'yxati (ixtiyoriy): ozkeyPacksUrl=https://.../index.json
        val packsUrl = (project.findProperty("ozkeyPacksUrl") as String?) ?: ""
        buildConfigField("String", "PACKS_URL", "\"$packsUrl\"")
    }

    // Release imzosi: CI sirlari (secrets) orqali beriladi (tools/make-keystore.sh ga qarang).
    signingConfigs {
        val ks = System.getenv("OZKEY_KEYSTORE")
        if (ks != null) {
            create("release") {
                storeFile = file(ks)
                storePassword = System.getenv("OZKEY_STORE_PASSWORD")
                keyAlias = System.getenv("OZKEY_KEY_ALIAS")
                keyPassword = System.getenv("OZKEY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Kalit berilsa — o'z kalitingiz bilan imzolanadi (Uptodown/Play uchun SHART). Berilmasa — sinov uchun debug kalit.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
