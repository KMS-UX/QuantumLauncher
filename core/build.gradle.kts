plugins {
    id("org.jetbrains.kotlin.jvm")
}

// Pure Kotlin/JVM — no Android/Compose deps (state engine, parser, geometry math). Unit-tests with
// zero emulator. Extracted out of :app (App Shell Integration, Phase 3) so :app-shell / :optics /
// :nav can all depend on NavigationChannel/PhosphorHue/etc. without depending on the launcher app.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation(kotlin("test"))
}
