plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// QUARK Brain Promotion (decision 88) — QuarkOnDeviceBrain, previously debug-gated inside :app's
// own com.quantumos.shell.ai package (QUARK Phase 1/2a/2b), extracted into its own library module
// so it can be a dependency of :app AND the docked modules (:comms/:files) without a circular
// dependency (:app already depends on :comms/:files to launch their Activities by class reference,
// so the reverse direction is impossible). This is exactly the extraction the Core Apps Fix-Pass's
// own AiAssistBridge placeholder comment named as the eventual fix. Depends on :core only — no
// UI/App Shell dependency, since a docked module's ViewModel needs the bridge, not any chrome.
android {
    namespace = "com.quantumos.quarkbrain"
    compileSdk = 35

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core"))

    // QUARK on-device brain — Google AI Edge LiteRT-LM (Engine / Conversation API), the
    // Kotlin/Android binding for on-device Gemma 4 E2B-IT inference with .litertlm files.
    // Moved here verbatim from :app's build.gradle.kts (QUARK Phase 1) — same dependency, new home.
    implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
