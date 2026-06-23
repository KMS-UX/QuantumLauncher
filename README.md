# QuantumOS — Pre-M0 Cloud Spike

First compiling slice of the QuantumOS launcher: the design-system foundation (phosphor
screen + live hue switch), the pure-logic core (`QuantumState`, `QuarkParser`) with unit
tests, and a cheap non-shader CRT overlay. Real AGSL shaders + launcher behaviour arrive on
hardware; this proves the toolchain and the backbone.

## Build it (no local machine)
**Easiest — GitHub Actions:** pushing this repo triggers `.github/workflows/android-build.yml`,
which runs the unit tests and builds `app-debug.apk`. Download it from the run's **Artifacts**.

**Interactive — Codespaces:** open the repo in a Codespace (use a 4-core machine), then:
```
gradle test          # run the logic tests
gradle assembleDebug # build the APK -> app/build/outputs/apk/debug/app-debug.apk
```

## See it run
No good emulator in either environment. Install `app-debug.apk` on a real device (best — real
GPU shows the true look) or upload it to a browser emulator like appetize.io.

Toolchain (pinned, stable): AGP 8.7.2 · Gradle 8.9 · Kotlin 2.0.21 · Compose BOM 2024.10.01 ·
compileSdk 35 · minSdk 33.
