plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "uz.ozkey.keyboard"
    compileSdk = 36   // Play: 2026-08-31 dan yangi ilova va yangilanishlar targetSdk 36 (Android 16) bo'lishi shart

    defaultConfig {
        applicationId = "uz.ozkey.keyboard"
        minSdk = 23
        targetSdk = 36
        versionCode = 18
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Tarjima serveri (backend/worker.js). gradle.properties ichida: ozkeyTranslateUrl=https://...
        val translateUrl = ((project.findProperty("ozkeyTranslateUrl") as String?) ?: "").trim()
        buildConfigField("String", "TRANSLATE_URL", "\"$translateUrl\"")
        // Qo'shimcha til paketlari ro'yxati (ixtiyoriy): ozkeyPacksUrl=https://.../index.json
        val packsUrl = ((project.findProperty("ozkeyPacksUrl") as String?) ?: "").trim()
        buildConfigField("String", "PACKS_URL", "\"$packsUrl\"")
        // Do'kon havolasi (baho/ulashish) va qo'llab-quvvatlash havolasi (Click/Payme/Buy Me a Coffee). Bo'sh bo'lsa tugma ko'rinmaydi.
        val storeUrl = (project.findProperty("ozkeyStoreUrl") as String?) ?: ""
        buildConfigField("String", "STORE_URL", "\"$storeUrl\"")
        val supportUrl = (project.findProperty("ozkeySupportUrl") as String?) ?: ""
        buildConfigField("String", "SUPPORT_URL", "\"$supportUrl\"")
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
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Kalit berilsa — o'z kalitingiz bilan imzolanadi (Uptodown/Play uchun SHART). Berilmasa — sinov uchun debug kalit.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures { buildConfig = true }

    // JVM unit testlarida android.* stub'lari xato bermasin (testlar toza mantiqni tekshiradi)
    testOptions { unitTests.isReturnDefaultValues = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}

// Play Market uchun release faqat o'z kalitingiz bilan yig'iladi. Kalitsiz `bundleRelease`/`assembleRelease`
// jimgina debug kalit bilan imzolanib, keyin Play'da rad etilmasligi uchun darhol to'xtatiladi.
// (Android Studio "Generate Signed Bundle" kalitni o'zi beradi — u ham ruxsat etiladi.)
val hasReleaseKey = System.getenv("OZKEY_KEYSTORE") != null ||
    project.hasProperty("android.injected.signing.store.file")
gradle.taskGraph.whenReady {
    if (!hasReleaseKey && allTasks.any { it.name == "bundleRelease" || it.name == "assembleRelease" }) {
        throw GradleException("Release imzo kaliti berilmagan (OZKEY_KEYSTORE, OZKEY_STORE_PASSWORD, OZKEY_KEY_ALIAS, OZKEY_KEY_PASSWORD). tools/make-keystore.sh ga qarang.")
    }
}
