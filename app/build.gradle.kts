plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.l5rcm.companion"
    compileSdk = 35

    // Reproducible builds (F-Droid): pin build-tools to 34.0.0. apksigner from
    // build-tools >= 35 produces APKs that F-Droid's apksigcopier cannot verify
    // (fdroid issue #3299), and 34.0.0 matches what the F-Droid build server uses
    // for aapt2/zipalign too.
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.l5rcm.companion"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "0.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // AGP embeds a signed "Dependency metadata" block in the APK by default. It is an
    // encrypted, non-reproducible blob that F-Droid's scanner rejects as an extra signing
    // block ("Found extra signing block 'Dependency metadata'"). Strip it from both the
    // APK and the bundle so the FLOSS release stays clean and reproducible.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // Release signing. Secrets come from env vars (CI) or gradle properties (local);
    // they are NEVER committed. When absent (e.g. F-Droid's build-from-source, or a
    // plain debug build) the release stays unsigned — which is exactly what F-Droid
    // compares against the developer-published, signed APK for reproducible builds.
    val releaseStoreFile = System.getenv("SIGNING_KEYSTORE_FILE")
        ?: (project.findProperty("signingKeystoreFile") as String?)
    val hasReleaseSigning = releaseStoreFile != null

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                    ?: project.findProperty("signingKeystorePassword") as String?
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                    ?: project.findProperty("signingKeyAlias") as String?
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                    ?: project.findProperty("signingKeyPassword") as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // Distribution flavors differ only in the QR decoder (the lone proprietary dependency):
    //   floss → zxing-cpp (Apache-2.0), the FOSS build published on F-Droid.
    //   full  → ML Kit, the build shipped on Play Store / as a direct APK.
    flavorDimensions += "dist"
    productFlavors {
        create("floss") { dimension = "dist" }
        create("full") { dimension = "dist" }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // .l5r files used as JVM unit-test fixtures
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        getByName("test") {
            resources.srcDir("src/test/resources")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)

    // QR import: CameraX preview/analysis is shared; the QR decoder is flavor-specific
    // (see src/floss and src/full implementations of qrFrameAnalyzer).
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    "flossImplementation"(libs.zxing.cpp) // Apache-2.0, FOSS — F-Droid build
    "fullImplementation"(libs.mlkit.barcode.scanning) // bundled on-device, no Play Services, but proprietary

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kxml2) // XmlPullParser impl for JVM unit tests

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

// Reproducible builds (F-Droid): AGP embeds a compiled ART baseline profile
// (assets/dexopt/baseline.prof) and reorders dex using the startup profile.
// Neither is bit-reproducible across build environments, so F-Droid's
// build-from-source never matches our published APK. Disabling the ArtProfile
// tasks drops the embedded profile and keeps a stable dex layout.
// Written config-cache-safe (the F-Droid build server uses the configuration cache).
tasks.matching { it.name.contains("ArtProfile") }.configureEach {
    enabled = false
}
