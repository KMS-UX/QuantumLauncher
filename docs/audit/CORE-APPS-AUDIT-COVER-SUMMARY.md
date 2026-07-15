# Core Apps Audit — Cover Summary

*Companion to the four per-app `docs/AUDIT-REPORT.md` files (QuantumComms, QuantumFiles, QuantumAudio, QuantumRadioReceiver). Read-only audit pass per Core Apps Audit Task Brief v1.0 — no source files touched in any of the five repos.*

## Environment note affecting deliverable #1

None of the four apps could be built live in this sandbox: `gradle assembleDebug` fails identically on all four at the plugin-resolution step because outbound access to `dl.google.com` (Google's Maven repo) is blocked by this session's egress policy — confirmed via proxy diagnostics as a `403` policy denial, not a repo defect, DNS issue, or dependency problem. Toolchain compliance was instead assessed by statically reading each repo's `gradle/libs.versions.toml` and `app/build.gradle.kts`. **Recommend re-running an actual build on the ROG (or any unrestricted machine) as the first step of the fix-pass**, since a genuinely broken build is still possible and wasn't ruled out here.

## Cross-cutting findings (true of all four apps identically)

These showed up in every single repo, which suggests they came from the same generation pattern rather than four independent judgment calls — worth fixing as one recipe rather than four bespoke efforts:

- **Toolchain float**: all four pin AGP 9.1.1 / Kotlin 2.2.10 / Compose BOM 2024.09.00 / minSdk 24, against the CLAUDE.md baseline of AGP 8.7.2 / Gradle 8.9 / Kotlin 2.0.21 / Compose BOM 2024.10.01 / minSdk 33. None have a committed Gradle wrapper.
- **No AGSL/GPU shaders anywhere, in any of the four apps.** Every CRT effect (scanlines, vignette, flicker) is a CPU `drawBehind`/Compose-animation loop — the exact pattern the platform notes forbid, with no shader path to even treat as the "fallback."
- **Chakra Petch is not bundled in any of the four apps.** All four ship the system Monospace font as their "terminal" look instead — a full-scope typography miss across the board, not a partial one anywhere.
- **No custom line-icon set in any of the four apps.** All four rely 100% on stock `androidx.compose.material.icons`.
- **No app has a working back-gesture handler** (`enableOnBackInvokedCallback` / `BackHandler` both absent everywhere), though all four do correctly call `enableEdgeToEdge()`.
- **All four still carry unrebranded Android-Studio/AI-Studio scaffold residue**: `com.example`/`com.aistudio.*` package IDs, generic project names, dead stock-Material `Color.kt`/`Theme.kt`/`colors.xml` files shipping unused in the APK.
- **None of the four has a shared `AppShell` reused from a common module** — expected at this stage per the brief's own scope note (docking into a shared `app-shell` module is explicitly a later, separate step), but worth naming since it means every app's fix-pass will re-implement shell chrome independently for now.

## Per-app verdicts

| App | Verdict | Standout positive | Standout blocker |
|---|---|---|---|
| **AUDIO** | Minor-to-significant gaps | Only app with an actual shared `AppShell` composable and a correctly recorder-first default screen; real-data polling loops (recording amplitude, playback spectrum) are properly gated on state | Confirmed instance of the brief's predicted top risk: the waveform manufactures a fake sine-wave drift when idle instead of going static, plus a shell-wide flicker loop and 7 ungated QUARK-mascot animations |
| **RADIO** | Significant gaps | Tuning-needle motion is genuinely event-driven and does not idle-jitter (contrary to the brief's top risk prediction for this app) — bands/presets/dial/meter are all structurally present and correct | Chrome is present but non-functional (dead back button, dead channel-selector taps, no registration marks, no QUARK trigger at all); a "cryptographic signal decoder" feature dominates the app and reads as SIGNAL's job, not RADIO's — needs a Director ruling before scoping |
| **FILES** | Significant gaps | The four required categories (FIELD-LOGS/CAPTURES/COMMS-CACHE/MAPS) are correctly named end-to-end with no generic-taxonomy substitution anywhere | The theme actually driving the app is stock Material purple with Material-You dynamic color — the correct phosphor token file exists but is dead code; two unconditional idle-animation loops; taxonomy is only seeded folder names on top of an otherwise generic, unrestricted filesystem browser |
| **COMMS** | Significant gaps | Palette token *file* has the most accurate hex values of the four (though undermined by duplicate/off-palette hardcodes elsewhere) | Worst identity drift of the four — channel list and message thread read as a generic Discord/WhatsApp-style chat app; the module's signature "live-pulse dot" feature is a static, motionless circle with a misleading comment claiming it pulses; no App Shell, no QUARK trigger at all |

## Suggested fix-pass sequencing

Ranking by estimated total fix work, least to most:

1. **AUDIO** — has real chrome and correct navigation defaults to build on; fixes are mostly subtractive (kill idle loops) and additive (shaders, font, icons, QUARK trigger) rather than structural rework.
2. **RADIO** — comparable code-level effort to Audio, but gated behind a product decision: the RADIO/SIGNAL identity question needs Director input before the fix-pass can be scoped, since it determines how much of the app's largest feature (the decoder) survives.
3. **FILES** — needs a live theme swap (not just cleanup, since the wrong theme is what's actually running) plus a product decision on how strictly to enforce the four-category taxonomy against free-form file/folder creation.
4. **COMMS** — the deepest rework: the chat-bubble/channel-list interaction model itself likely needs redesigning to read as "callsign channels + transmission threads" rather than a themed generic messenger, on top of every other cross-cutting gap.

Given the spread — two apps (AUDIO, RADIO) are largely mechanical fix-pass candidates once RADIO's identity question is settled, while the other two (FILES, COMMS) each carry a real product-design question underneath the house-style violations — **recommend splitting into two fix-pass briefs**, same as the Optics & Nav precedent: **Brief A = AUDIO + RADIO** (once the RADIO/SIGNAL ruling lands), **Brief B = FILES + COMMS** (both likely warrant a short design pass from Clara — taxonomy-enforcement UX for FILES, transmission-thread visual language for COMMS — before implementation starts, not just a straight findings-to-fix translation).
