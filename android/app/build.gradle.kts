plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.shopar.demos"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.shopar.demos.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-demo"
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.mobinova.shopar:agency-android:0.1.0-SNAPSHOT")
    implementation("com.mobinova.shopar:discovery-android:0.1.0-SNAPSHOT")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
