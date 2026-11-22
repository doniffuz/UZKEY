import java.util.Properties

val ksProps = Properties()
val ksPropsFile = rootProject.file("keystore.properties")
if (ksPropsFile.exists()) {
    ksPropsFile.inputStream().use { stream -> ksProps.load(stream) }
}
fun ksVal(prop: String, env: String): String? =
    ksProps.getProperty(prop) ?: System.getenv(env)
val ksFile: String? = ksVal("storeFile", "OZKEY_KEYSTORE")

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
        versionCode = 19
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Do'kon havolasi (baho/ulashish) va qo'llab-quvvatlash havolasi (Click/Payme/Buy Me a Coffee). Bo'sh bo'lsa tugma ko'rinmaydi.
        val storeUrl = (project.findProperty("ozkeyStoreUrl") as String?) ?: ""
        buildConfigField("String", "STORE_URL", "\"$storeUrl\"")
        val supportUrl = (project.findProperty("ozkeySupportUrl") as String?) ?: ""
        buildConfigField("String", "SUPPORT_URL", "\"$supportUrl\"")
    }

    // Release imzosi: avval lokal keystore.properties (git'ga QO'SHILMAYDI), bo'lmasa CI muhit o'zgaruvchilari.
    // Namuna: keystore.properties.example. Kalit yaratish: README_RELEASE.md.

    signingConfigs {
        if (ksFile != null) {
            create("release") {
                storeFile = rootProject.file(ksFile)
                storePassword = ksVal("storePassword", "OZKEY_STORE_PASSWORD")
                keyAlias = ksVal("keyAlias", "OZKEY_KEY_ALIAS")
                keyPassword = ksVal("keyPassword", "OZKEY_KEY_PASSWORD")
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
val hasReleaseKey = ksFile != null ||
    project.hasProperty("android.injected.signing.store.file")
gradle.taskGraph.whenReady {
    if (!hasReleaseKey && allTasks.any { it.name == "bundleRelease" || it.name == "assembleRelease" }) {
        throw GradleException("Release imzo kaliti berilmagan: keystore.properties yarating (keystore.properties.example ga qarang) yoki OZKEY_* muhit o'zgaruvchilarini bering.")
    }
}
