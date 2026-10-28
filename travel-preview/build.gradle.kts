plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.contest.travel.preview"
    compileSdk = 35
    // Some Pleos images provide these classes on the boot class path.
    useLibrary("android.car", false)

    defaultConfig {
        applicationId = "com.contest.travel.preview"
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
    implementation("org.maplibre.gl:android-sdk:11.11.0")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
