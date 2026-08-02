plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

// Release signing material. Never commit it: keystore.properties and *.jks/*.keystore
// are git-ignored. Local builds read keystore.properties; CI reads environment variables.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun signingValue(propertyKey: String, environmentKey: String): String? =
    (keystoreProperties.getProperty(propertyKey) ?: System.getenv(environmentKey))
        ?.takeIf { it.isNotBlank() }

android {
    namespace = "com.niranjan.englisharticle"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.niranjan.englisharticle"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            (localProperties.getProperty("openrouter.api.key") ?: "").asBuildConfigString()
        )
        buildConfigField(
            "String",
            "OPENROUTER_MODEL",
            (localProperties.getProperty("openrouter.model") ?: "google/gemini-2.5-flash").asBuildConfigString()
        )
        buildConfigField(
            "String",
            "DEEPGRAM_API_KEY",
            (localProperties.getProperty("deepgram.api.key") ?: "").asBuildConfigString()
        )
        buildConfigField(
            "String",
            "DEEPGRAM_VOICE",
            (localProperties.getProperty("deepgram.voice") ?: "aura-asteria-en").asBuildConfigString()
        )
        buildConfigField(
            "int",
            "DEEPGRAM_AUDIO_OFFSET_MS",
            (localProperties.getProperty("deepgram.audio.offset.ms") ?: "0")
        )
    }

    signingConfigs {
        // Locals are deliberately prefixed: inside create("release") { } an unprefixed
        // `storePassword` would resolve to the SigningConfig property, not the local,
        // silently self-assigning.
        val ksStoreFile = signingValue("storeFile", "ANDROID_KEYSTORE_FILE")
        val ksStorePassword = signingValue("storePassword", "ANDROID_KEYSTORE_PASSWORD")
        val ksKeyAlias = signingValue("keyAlias", "ANDROID_KEY_ALIAS")
        val ksKeyPassword = signingValue("keyPassword", "ANDROID_KEY_PASSWORD")

        // Only declared when every value is present, so `assembleRelease` still works
        // unsigned (on CI, or before a keystore exists) instead of failing to configure.
        if (ksStoreFile != null && ksStorePassword != null &&
            ksKeyAlias != null && ksKeyPassword != null
        ) {
            create("release") {
                storeFile = file(ksStoreFile)
                storePassword = ksStorePassword
                keyAlias = ksKeyAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Null when no keystore is configured; AGP then emits an unsigned APK.
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
