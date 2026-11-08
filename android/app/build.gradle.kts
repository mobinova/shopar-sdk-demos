import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val shoparDemoProperties = Properties().apply {
    val file = rootProject.file("demo.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}

fun demoProperty(name: String, default: String): String =
    shoparDemoProperties.getProperty(name, default).replace("\\", "\\\\").replace("\"", "\\\"")

val shoparAndroidSdkRevision = providers.exec {
    commandLine(
        "git", "-C", rootProject.file("../vendor/shopar-android-sdk").absolutePath,
        "rev-parse", "--short=12", "HEAD",
    )
}.standardOutput.asText.get().trim()

android {
    namespace = "app.shopar.demos"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.shopar.demos.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-demo"
        buildConfigField("String", "SHOPAR_SDK_SOURCE_REVISION", "\"$shoparAndroidSdkRevision\"")
        buildConfigField("String", "SHOPAR_DEMO_AGENCY_APP_ID", "\"${demoProperty("agency.appId", "")}\"")
        buildConfigField("String", "SHOPAR_DEMO_AGENCY_HOST", "\"${demoProperty("agency.host", "grpc.shopar.app")}\"")
        buildConfigField("String", "SHOPAR_DEMO_AGENCY_PORT", "\"${demoProperty("agency.port", "443")}\"")
        buildConfigField("Boolean", "SHOPAR_DEMO_AGENCY_TLS", demoProperty("agency.tls", "true"))
        buildConfigField("String", "SHOPAR_DEMO_DISCOVERY_APP_ID", "\"${demoProperty("discovery.appId", "")}\"")
        buildConfigField("String", "SHOPAR_DEMO_DISCOVERY_HOST", "\"${demoProperty("discovery.host", "grpc.shopar.app")}\"")
        buildConfigField("String", "SHOPAR_DEMO_DISCOVERY_PORT", "\"${demoProperty("discovery.port", "443")}\"")
        buildConfigField("Boolean", "SHOPAR_DEMO_DISCOVERY_TLS", demoProperty("discovery.tls", "true"))
        buildConfigField("String", "SHOPAR_DEMO_DISCOVERY_PLACEMENT_IDS", "\"${demoProperty("discovery.placementIds", "")}\"")
        buildConfigField("String", "SHOPAR_DEMO_DISCOVERY_SELECTED_PLACEMENT_ID", "\"${demoProperty("discovery.selectedPlacementId", "")}\"")
    }

    buildFeatures { compose = true; buildConfig = true }
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
