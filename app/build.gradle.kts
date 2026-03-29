plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.shadowcontacts.app"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = file("../release-keystore.jks")
            storePassword = "dtt1992shnk"
            keyAlias = "shadow-contacts"
            keyPassword = "dtt1992shnk"
        }
    }

    defaultConfig {
        applicationId = "com.shadowcontacts.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // ── Build Flavors ──
    flavorDimensions += "distribution"
    productFlavors {
        create("full") {
            dimension = "distribution"
            // Full version: includes Caller ID popup, overlay, notifications
            // For GitHub, F-Droid, sideloading
            buildConfigField("boolean", "HAS_CALLER_ID", "true")
        }
        create("playstore") {
            dimension = "distribution"
            // Play Store version: no telephony permissions, no caller ID
            // Safe for Google Play review
            buildConfigField("boolean", "HAS_CALLER_ID", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Material 3
    implementation("com.google.android.material:material:1.11.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Gson for import/export
    implementation("com.google.code.gson:gson:2.10.1")
}
