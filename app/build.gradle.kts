plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.partituresfesteres.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.partituresfesteres.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 25
        versionName = "1.6.3"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Compose 1.11.x keeps us on a stable API 36 toolchain instead of requiring API 37.
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.11.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.11.4")
    implementation("androidx.compose.foundation:foundation:1.11.4")
    implementation("androidx.compose.material:material:1.11.4")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling:1.11.4")
}
