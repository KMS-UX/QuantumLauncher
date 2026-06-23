# BUILD_LOG.md — QuantumOS code state

The code-side companion to the Build Bible. The Bible tracks *design decisions*; this tracks
*code state*, so each Claude Code session resumes exactly where the last stopped. Update the top
block at the end of every session.

---

## ▶ RESUME HERE
**Current milestone:** M1 — Launcher core — ✅ DONE, confirmed on Fold 6 (2026-06-23).
APK installs, launches, APPS grid shows real installed apps, tap-to-launch opens them.
**Goal of next session:** Start **M2 — STATUS + LOG channels** (real battery/uptime/storage
telemetry feeding the STATUS channel; real event log feeding the LOG channel). The two channels
currently render an "OFFLINE — M2" placeholder; wire them to live data via the existing
`incomingTelemetryUpdate(...)` seam and `systemLogs` flow in `QuantumStateEngine`. Remove the
`runDevSimulation()` harness as real telemetry comes online.

## Status
- [x] First green `gradle assembleDebug`  ← achieved during M1 (see note below)
- [x] `gradle test` — 4/4 passing
- [ ] M0 confirmed on Fold 6 (phosphor screen + hue switch live)  ← Director action
- [ ] Chakra Petch actually bundled (currently Monospace placeholder)  ← deferred to M2/polish

> **Note on the green build:** the project had NEVER actually compiled before M1, despite M0
> being described as done. Two latent blockers were masking each other: (1) `settings.gradle.kts`
> had no `pluginManagement` repos so AGP could not resolve, and (2) there was no `res/` directory
> at all, yet the manifest referenced `@string/app_name` and `@style/Theme.QuantumOS`. Both are
> now fixed (repos declared; `res/values/{strings,colors,themes}.xml` created). The Kotlin sources
> compiled clean once it reached that stage.
- [x] HOME intent-filter added (M1 Step 1) — will offer QuantumOS as Home picker option on install
- [x] `<queries>` package-visibility block added (M1 Step 2)
- [x] APPS channel: queries real installed apps, renders grid, tap-to-launch (M1 Step 3)
- [x] HOME ⇄ APPS navigation wired via `transitionNavigation` (M1 Step 4)
- [x] App icon added (adaptive phosphor-Q) + `android:icon` wired — required for Samsung install
- [x] **M1 confirmed on Fold 6 (2026-06-23)** — installs, runs, lists real apps, launches them

## Rollback confirmed (Step 0 — M1 Task Brief)
**Director: before setting QuantumOS as default Home, verify the return path:**
> Settings → Apps → Default apps → Home app → select the stock/other launcher.
>
> This is a manual step on the device. Installing this build only makes Android *offer*
> QuantumOS as a Home option (via the picker). It does NOT silently take over. Nothing changes
> until the Director explicitly switches in the system prompt or Settings.

HOME category was confirmed NOT declared before M1 work began (manifest verified clean).

## Known issues / TODOs
- Typography: replace `FontFamily.Monospace` with Chakra Petch (`res/font/chakra_petch.ttf`).
- CRT: current overlay is the cheap non-shader fallback; real AGSL shader is M6 polish.
- `runDevSimulation()` in the ViewModel is a dev-only harness — remove before M7.
- App icons (in the grid): loaded from PackageManager via Drawable→Bitmap conversion. No custom
  icon styling yet; icons render at system defaults. Deferred to polish milestone.
- **Samsung install gotcha (resolved):** One UI's package installer silently aborts if the APK
  has no `android:icon` (now fixed) AND can choke on odd download filenames — keep the artifact
  named `app-debug.apk`. Director confirmed install worked once the file was renamed cleanly.
- Empty-state for APPS grid ("SCANNING...") shows if app list is empty at query time.
- STATUS and LOG channels show an "OFFLINE" body in M1; they wire up in M2.
- Action rail (bottom chrome strip) is not yet rendered — M2/M3 item.
- `loadApps` is called once at launch; no refresh on app install/uninstall (M2+ concern).

## Decisions pending (Director / Clara — do not lock in code)
- **Fixed container vs fill-and-adapt** (the 9:19.5 letterbox). Default is fill-and-adapt; Director
  to judge on-device on the Fold before we lock it.
- **APPS grid column count**: currently `GridCells.Adaptive(72.dp)`. On Fold 6 inner display this
  will give ~5–6 columns. Director to judge whether that's right or we should fix to 4.

## Session history
- **M0 session:** Design-system foundation — phosphor screen, hue switch, QuantumState engine,
  4 unit tests, cheap non-shader CRT overlay. Font placeholder in place.
- **M1 session (2026-06-23):** HOME intent declared, `<queries>` added, App Shell chrome
  (nameplate + channel strip), APPS grid wired to real PackageManager, HOME ⇄ APPS nav,
  back gesture routing. All Steps 0–5 complete. Also unblocked the build for the first time:
  fixed `settings.gradle.kts` plugin repos, created the missing `res/values/*` (strings, colors,
  CRT-ground theme), and repaired the CI workflow (valid YAML + Gradle wrapper). Added adaptive
  app icon to fix Samsung's silent install rejection. **Confirmed on Fold 6 by the Director** —
  M1 closed. Tracked in PR #1.
