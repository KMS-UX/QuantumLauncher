# QuantumOS — Build Log

Tracks *code state* the way the Build Bible tracks design decisions. Update at the end of every session. See `docs/Verification-Infrastructure-Addendum-v1.0.md` §3 for rationale.

---

## Current milestone: Pre-M0 Cloud Spike

**Branch:** `claude/epic-lamport-je0hiz`
**Resume here:** CI pipeline green on that branch — next task is M0 design-system completion (see M0 checklist below).

---

## Session log

### Session 1 — CI pipeline stabilisation (2026-06-23)

**What was fixed:**

| Fix | File(s) |
|---|---|
| `working-directory: launcher` was concatenated into step `name:` — fixed YAML structure | `.github/workflows/android-build.yml` |
| `./gradlew` → `gradle` (no wrapper in repo; `setup-gradle` action installs Gradle 8.9) | `.github/workflows/android-build.yml` |
| Artifact paths had spurious `launcher/` prefix | `.github/workflows/android-build.yml` |
| `settings.gradle.kts` contained `build.gradle.kts` plugin content instead of `rootProject.name` + `include(":app")` | `settings.gradle.kts` |
| Added `pluginManagement { google() }` + `dependencyResolutionManagement` — AGP is on Google Maven, not Gradle Central | `settings.gradle.kts` |
| Created missing `res/values/strings.xml` (`app_name`) and `res/values/themes.xml` (`Theme.QuantumOS`) | `app/src/main/res/values/` |
| Added `--rerun-tasks` to `gradle test` so test reports are always written (not skipped by Gradle's build cache) | `.github/workflows/android-build.yml` |
| Added `ui-text-google-fonts` dep + Chakra Petch font infrastructure | `app/build.gradle.kts`, `LauncherUi.kt`, `res/values/font_certs.xml` |
| Deleted spurious root `LauncherUi.kt` (contained `app/build.gradle.kts` content) | — |
| Added `CLAUDE.md` (session quick-reference) and `BUILD_LOG.md` (this file) | repo root |
| Committed `docs/` and `.claude/skills/` from context bundle | repo root |

**CI status after session:** Green on `cba576f` (resource files fix). Subsequent commits on this branch pending next run.

---

## M0 checklist — Design-system foundation

Verify milestone: *the app launches to a phosphor CRT screen with working font + hue switch.*

- [x] Phosphor palette tokens (`Phosphor` object, `PhosphorHue` enum) — `LauncherUi.kt`
- [x] CRT treatment overlay (non-shader stub, correct for cloud/emulator) — `LauncherUi.kt:crtOverlay()`
- [x] Hue switch live (`vm.setHue()`, engine `updateEnvironmentProfile`) — `LauncherUi.kt`, `QuantumState.kt`
- [ ] **Typography — Chakra Petch.** Infrastructure added (font dep + `ChakraPetchFamily` declaration). Font certs XML is a stub — populate via Android Studio "Add Downloadable Font" (Chakra Petch) to get real provider certs, or bundle the TTF at `res/font/chakra_petch.ttf` and switch to `Font(R.font.chakra_petch)`.
- [ ] Stepped-motion specs — not yet implemented (smooth for now; add discrete easing at M6 polish)
- [ ] PLEASE STANDBY component — not yet implemented; placeholder in boot sequence
- [ ] Sound stubs — not yet implemented (M6)

## M1 checklist — Launcher core
*(not started)*
- [ ] Declare `CATEGORY_HOME` + `CATEGORY_DEFAULT` intent-filter in `AndroidManifest.xml`
- [ ] Add `QUERY_ALL_PACKAGES` permission
- [ ] Home channel — channel selector (HOME / APPS / STATUS / LOG) wired to navigation
- [ ] APPS grid — enumerate installed packages, launch on tap, phosphor/CRT styled
- [ ] Verify: set QuantumOS as default launcher on Fold 6; confirm stock-launcher rollback (Settings → Apps → Default apps → Home)

---

## Known issues / decisions pending

- **Compose BOM** is pinned at `2024.10.01`. The Verification Addendum notes "current line ~Compose 1.11 / BOM 2026.04" — update once on a machine that can resolve the exact Maven version.
- **Chakra Petch font certs** (`res/values/font_certs.xml`) are stubs. The font will silently fall back to Monospace on device until real certs are added. See M0 checklist above.
- **compileSdk / targetSdk** are at 35. Android 16 (API 36) is likely current — update to latest when the BOM is bumped.
- **Non-shader CRT stub** is correct for cloud builds; AGSL phosphor glow is deferred to device tuning (M6).

---

## Signing key note

The signing key for release/sideload builds **must be kept backed up** separately (decision 63). If lost, clean updates to the Fold 6 break. Generate with `keytool` when preparing M7.
