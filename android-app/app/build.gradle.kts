plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

val uploadKeystore = rootProject.file("keystore/astrotalks-upload.jks")
val uploadStorePassword = System.getenv("ASTROTALKS_STORE_PASSWORD")
val uploadKeyPassword = System.getenv("ASTROTALKS_KEY_PASSWORD")

android { namespace = "com.astrotalks.app"; compileSdk = 36
    defaultConfig { applicationId = "com.astrotalks.app"; minSdk = 26; targetSdk = 36; versionCode = 3; versionName = "1.0.2" }
    signingConfigs { create("release") { if (uploadKeystore.exists() && !uploadStorePassword.isNullOrBlank() && !uploadKeyPassword.isNullOrBlank()) { storeFile = uploadKeystore; storePassword = uploadStorePassword; keyAlias = "astrotalks-upload"; keyPassword = uploadKeyPassword } } }
    buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("release"); proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL", "") }\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", "") }\"")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}
