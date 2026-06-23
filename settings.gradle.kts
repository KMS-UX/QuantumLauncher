// Pinned to a proven, internally-consistent toolchain (late-2024 stable line) rather than the
// newest releases — for a no-debugger spike, a known-good build config beats bleeding edge.
// We move to current tooling on the ROG, where we can iterate on errors live.
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
