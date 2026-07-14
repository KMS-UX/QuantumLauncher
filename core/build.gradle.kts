plugins {
    id("org.jetbrains.kotlin.jvm")
}

// Pure-logic module (CLAUDE.md: "Logic lives in com.quantumos.core — no UI deps, unit-tested").
// No Android/Compose dependency here by design, so it builds and tests with a plain JVM — no
// Android SDK required (App Shell Integration Step 1: core extracted alongside the chrome so the
// app-shell module has a non-Android dependency to sit on).
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
