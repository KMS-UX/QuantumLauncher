import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropsFile = rootProject.file("keystore.properties")

android {
    namespace = "com.quantumos.shell"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quantumos.shell"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-beta"
    }

    if (keystorePropsFile.exists()) {
        val keystoreProps = Properties()
        keystoreProps.load(keystorePropsFile.inputStream())
        signingConfigs {
            create("release") {
                storeFile = file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePropsFile.exists()) {
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
        compose = true
    }
}

dependencies {
    // Compose BOM keeps all Compose library versions aligned with one number.
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Chakra Petch via Downloadable Fonts (Monofonto substitute, M0 typography).
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // QUARK on-device brain — Phase 1, debug-gated. Google AI Edge LiteRT-LM (Engine / Conversation
    // API), the Kotlin/Android binding for on-device Gemma 4 E2B-IT inference with .litertlm files.
    // Never in the production scripted-brain path; lives entirely behind the triple-tap debug toggle.
    implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")

    // QUARK Phase 2b — custom voice. ONNX Runtime for on-device Kokoro (kokoro-v1.0) inference of
    // the locked QUARK-H2 blend. Debug-gated behind the same // VOICE toggle; falls back to the
    // Phase 2a Android-TTS placeholder when the model/phonemizer aren't present. See KokoroVoiceEngine.
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")

    // Pure-logic unit tests (no emulator) — runbook Step 4.
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation(kotlin("test"))
}
