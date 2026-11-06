plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ferzazulic.carmusic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ferzazulic.carmusic"
        minSdk = 24
        targetSdk = 35
        // CI run number makes every build a newer version, so it installs as an update.
        val build = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionCode = 100 + build
        versionName = "1.2.$build"
        vectorDrawables { useSupportLibrary = true }
    }

    // One stable key for every build (prepared by CI, see .github/workflows/build.yml). Previously each
    // CI run signed with a fresh random debug key, so Android refused to install new builds as updates.
    val keystoreFile = file(System.getenv("CARMUSIC_KEYSTORE") ?: "carmusic.keystore")
    signingConfigs {
        create("carmusic") {
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("CARMUSIC_KEYSTORE_PASSWORD") ?: "android"
                keyAlias = "carmusic"
                keyPassword = System.getenv("CARMUSIC_KEYSTORE_PASSWORD") ?: "android"
            }
        }
    }
    val signing = if (keystoreFile.exists()) signingConfigs.getByName("carmusic") else signingConfigs.getByName("debug")

    buildTypes {
        debug {
            signingConfig = signing
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signing
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.palette:palette-ktx:1.0.0")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.media3:media3-datasource:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")

    // USB serial (CANable / SLCAN adapters) for writing to the Astra H CID over MS-CAN.
    implementation("com.github.mik3y:usb-serial-for-android:3.8.1")
}
