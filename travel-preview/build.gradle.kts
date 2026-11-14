import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Only the public Maps Client ID is passed to Android. Directions secrets stay on the host.
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.isFile) localFile.inputStream().use(::load)
}
val naverMapClientId = localProperties.getProperty("NAVER_MAP_CLIENT_ID", "").trim()
val naverMapStyleId = localProperties.getProperty("NAVER_MAP_STYLE_ID", "").trim()

android {
    namespace = "com.contest.travel.preview"
    compileSdk = 35
    // Some Pleos images provide these classes on the boot class path.
    useLibrary("android.car", false)

    defaultConfig {
        applicationId = "com.contest.travel.preview"
        manifestPlaceholders["NAVER_MAP_CLIENT_ID"] = naverMapClientId
        manifestPlaceholders["NAVER_MAP_STYLE_ID"] = naverMapStyleId
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-preview"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.vehicle.sdk)
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
    implementation("com.naver.maps:map-sdk:3.24.1")
    implementation("org.maplibre.gl:android-sdk:11.11.0")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
