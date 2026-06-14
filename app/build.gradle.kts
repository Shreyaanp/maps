import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

fun configValue(name: String, fallback: String): String =
    localProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: fallback

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "work.shreyaan.dwell"
    compileSdk = 35

    defaultConfig {
        applicationId = "work.shreyaan.dwell"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0"
        buildConfigField(
            "String",
            "DWELL_API_BASE_URL",
            configValue("DWELL_API_BASE_URL", "https://dwell.shreyaan.work")
                .asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "NOMINATIM_BASE_URL",
            configValue("NOMINATIM_BASE_URL", "https://nominatim.openstreetmap.org")
                .asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "MAP_STYLE_URL",
            configValue("MAP_STYLE_URL", "https://tiles.openfreemap.org/styles/liberty")
                .asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "NOMINATIM_USER_AGENT",
            configValue(
                "NOMINATIM_USER_AGENT",
                "Dwell/1.0 (work.shreyaan.dwell; +https://dwell.shreyaan.work) Android",
            ).asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "GOOGLE_SERVER_CLIENT_ID",
            configValue(
                "GOOGLE_SERVER_CLIENT_ID",
                configValue("GOOGLE_WEB_CLIENT_ID", ""),
            ).asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            configValue(
                "GOOGLE_SERVER_CLIENT_ID",
                configValue("GOOGLE_WEB_CLIENT_ID", ""),
            ).asBuildConfigString(),
        )
    }

    signingConfigs {
        create("release") {
            // Provided by CI via env vars; local release builds stay unsigned.
            val ksPath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (!ksPath.isNullOrEmpty()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (!System.getenv("ANDROID_KEYSTORE_PATH").isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        buildConfig = true
        compose = true
    }

}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
    implementation("org.maplibre.gl:android-sdk:11.11.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
