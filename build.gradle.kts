// Kotlin bumped 2.0.21 → 2.2.21 to match litertlm-android:0.13.1 which requires Kotlin 2.2.x.
// AGP stays at 8.7.2; Compose BOM stays at 2024.10.01 (both remain compatible with Kotlin 2.2).
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("com.android.library") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}
