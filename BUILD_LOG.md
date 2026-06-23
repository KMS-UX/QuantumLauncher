# BUILD_LOG.md — QuantumOS code state

The code-side companion to the Build Bible. The Bible tracks *design decisions*; this tracks
*code state*, so each Claude Code session resumes exactly where the last stopped. Update the top
block at the end of every session.

---

## ▶ RESUME HERE
**Current milestone:** M4 — floating QUARK trigger — ✅ **code complete**; **awaiting on-device
confirmation on the Fold 6** (the headline check: does the mark hover over *another real app* and
stay draggable + tappable there). A persistent, app-icon-sized phosphor "iris" floats over every
app via a `TYPE_APPLICATION_OVERLAY` window owned by a foreground `Service`. Static at rest;
drags 1:1; snaps to the nearest edge on release; tap plays the reused PLEASE STANDBY beat and opens
a placeholder stub Activity that acknowledges the real Assistant View is M5.
**Goal of next session:** Start **M5 — QUARK Assistant View** — replace the placeholder stub
(`QuarkStubActivity`) with the real content: four reactive states, conversation log, command rail,
text entry, scripted-brain wiring. Do NOT start M5 until the Director confirms M4 on hardware.

> **Director note — the one new permission (M4):** the trigger needs **"Allow display over other
> apps"** (SYSTEM_ALERT_WINDOW). It is a **one-time Settings toggle** — already anticipated in the
> Launcher Build Spec §5 — and it **cannot** be a runtime dialog; the app sends you to the system
> overlay-settings screen. On HOME, while it's ungranted, a `QUARK TRIGGER // GRANT OVERLAY ►`
> control opens that screen; on return the app re-detects the grant on resume (no restart) and
> deploys the trigger. No other permission was added or requested this milestone.

> **M4 default park position:** right edge, mid-height (`OverlayGeometry.defaultPark`) — clear of
> the bottom-centre system gesture area and the status bar. **Forward concern (Bible):** avoiding a
> *companion app's* primary control (e.g. a future camera shutter) is NOT encoded yet — those apps
> don't exist, so it isn't testable today. Don't forget it when companion apps land.

> **M4 known limits / forward concerns:** (1) live hue sync is push-only — the launcher re-tints the
> mark via a redelivered start command when phosphor changes *from within QuantumOS*; an external
> relaunch defaults to green until the launcher next resumes. (2) The foreground-service
> notification is intentionally minimal and won't be *visible* unless POST_NOTIFICATIONS is granted
> (we don't prompt — M4 hard stop); the service still runs. (3) No "retract trigger" control yet —
> the overlay persists once deployed. (4) The iris is placeholder art, **not** the final QUARK
> mascot (deferred to the identity/polish stage per decision 60).

> **M3 designed interaction rule (logged, not buried):** turning **Beacon ON force-drops Stealth** —
> active signalling outranks staying low-emission. The rule lives in `QuantumStateEngine.toggleBeacon()`
> (unit-tested) so it's a single source of truth, not a UI assumption.

> **M2 is still pending Director hardware-sign-off** — M3 was built on top per the brief's note that
> M1/M2 are closed; if the Fold surfaces an M2 regression, flag it alongside the M3 check.

> **Branch consolidation (2026-06-23):** three divergent branches were merged into `main` — the
> M1 line (`gracious-thompson`, verified on Fold 6), the docs/skill/font-infra line
> (`epic-lamport`), and the task-brief line. `main` had never compiled (its `settings.gradle.kts`
> was a broken copy of the root build file and it had no `res/`); it now carries the verified M1
> code. Duplicates resolved to the verified M1 versions; broken `android-build.yml` and the stray
> root `LauncherUi.kt` dropped.

## Status
- [x] First green `gradle assembleDebug`  ← achieved during M1
- [x] `gradle test` — 12/12 passing (9 prior + 3 new M4 `OverlayGeometry` tests; the cloud session
  has no Android SDK so CI runs the real `gradle test`/`assembleDebug` on push — see
  `.github/workflows/build.yml`)

### M4 — floating QUARK trigger (this session)
- [x] **Step 0 — permission walkthrough:** `LauncherActivity` checks `Settings.canDrawOverlays`,
  re-checks on `ON_RESUME` (grant happens outside the app, then return — no restart). Ungranted →
  the HOME `QUARK TRIGGER // GRANT OVERLAY ►` control fires
  `Intent(ACTION_MANAGE_OVERLAY_PERMISSION, package:…)`. Granted → the control reads
  `QUARK TRIGGER // DEPLOYED` and the service is (re)started. No runtime-permission dialog exists for
  this capability — the Settings screen is the only path. **No other permission added/requested.**
- [x] **Step 1 — the overlay:** `QuarkTriggerService` (foreground, `specialUse` FGS type) adds a
  `TYPE_APPLICATION_OVERLAY` view via `WindowManager` — Service-owned, NOT Activity-scoped, so it
  survives switching apps. App-icon-sized (52dp ≈ an APPS-grid icon, not larger). Visual = a simple
  phosphor "iris" (ring + dim disc + bright aperture) on the CRT ground, **static at rest** (no idle
  animation). Minimal ongoing FGS notification.
- [x] **Step 2 — draggable + edge-snap:** 1:1 real-time follow while dragging (the one place
  "stepped" motion doesn't apply); on release it snaps to the nearest edge via the unit-tested
  `OverlayGeometry.nearestEdgeX`, settled in a quick **stepped** run (6 × 12ms), not an elastic ease.
  Default park = right edge, mid-height (`OverlayGeometry.defaultPark`).
- [x] **Step 3 — tap (M4/M5 boundary):** tap → reused `PleaseStandbyCard` beat (extracted from the
  M3 Lock overlay, now public/shared — not rebuilt) → `QuarkStubActivity`, a full-screen phosphor
  placeholder reading `QUARK / ASSISTANT VIEW` + the one line `MODULE PENDING // M5` +
  `◄ TAP TO RETURN, OPERATOR`. Tap or Back `finish()`es back to whatever app was underneath. **No**
  reactive states / conversation log / command rail / text entry — that's M5.
- [x] **Core logic:** `OverlayGeometry` (edge-snap + default park) lives in `com.quantumos.core`
  (no Android deps), unit-tested — single source of truth, same pattern as the M3 Beacon rule.
- [ ] **M4 confirmed on Fold 6** — grant the overlay toggle; the iris appears (phosphor, static);
  drag it and it settles to an edge; tap plays PLEASE STANDBY → placeholder, Back returns; **open a
  real other app (Settings / an APPS-grid app) and confirm the iris still hovers, drags, and taps
  there** (the headline check).  ← **Director action**

### M3 — Vitality panel (previous session)
- [x] **Step 0 — atom mark + roll-down:** ⚛ atom mark added to the **HOME channel header only**
  (per the scope boundary — NOT on APPS/STATUS/LOG). Static at rest; one stepped quarter-turn spin
  on open. Panel rolls down with **stepped** motion (discrete step count, not a smooth slide).
  **Two close methods built:** tap the atom mark again, or the `▲ STOW` handle on the open panel
  (Back also stows it first).
- [x] **Step 1 — Zone 1 vitals (read-only):** reuses `engine.masterState.vitality` (no second data
  path). Readiness renders as `NN% WORD`; **CRITICAL is the one warn-red here**. New in-house
  `SegmentedGauge` (phosphor segments, no Material `LinearProgressIndicator`) drives **Signal**,
  **Power**, **Core Temp**. Core Temp = battery `EXTRA_TEMPERATURE` (same M2 receiver). Signal =
  coarse transport tier (wifi=4, cellular=2, offline=0) — no precise-dBm permission. Uptime ticks
  every 1s **only while the panel is open** (no idle redraw at rest).
- [x] **Step 2 — Zone 2 four actions (Stealth · Phosphor · Beacon · Lock, decision-36 order):**
  - **Phosphor** — cycles hue green→amber→cyan→green via the existing env mechanism (no 2nd path).
  - **Stealth** — hard-dims **this window's** `screenBrightness` (no permission, reversible);
    saturation untouched (brightness only). SFX-mute is wired as the `isStealthMode` flag (see note).
  - **Beacon** — real torch via `CameraManager.setTorchMode` (no permission) + a blinking warn-red
    `⚑ BEACON` field-flag on HOME. **Turning Beacon ON force-drops Stealth** (rule in core, tested).
  - **Lock** — calls the existing `executeCosmeticLockSequence()`; a new `LockOverlay` plays the
    PLEASE STANDBY → DEVICE SECURED beat and **tap-to-unseal** calls `unlockDeviceProfile()`.
    Cosmetic only — no Device Admin / `lockNow()`.
- [ ] **M3 confirmed on Fold 6** — atom roll-down reads stepped (not smooth); gauges show real
  numbers and move on plug/unplug + Wi-Fi toggle; Stealth dims without washing out the phosphor and
  is reversible; Beacon lights the torch + red flag and drops Stealth; Lock plays the securing beat
  and unseals.  ← **Director action**
- [x] **M2 Step 0:** container is fill-and-adapt (`forceFixedContainer=false`); APPS grid is
  `GridCells.Adaptive(minSize = 88.dp)` (column count follows screen width) — *Director to judge on Fold*
- [x] **M2 Step 1:** STATUS channel wired to real vitals via `engine.incomingTelemetryUpdate(...)`
  — battery %/charging + battery temp (ACTION_BATTERY_CHANGED sticky), uptime
  (`SystemClock.elapsedRealtime`, HH:MM:SS), connectivity (ConnectivityManager: connected + transport)
- [x] **M2 Step 2:** LOG channel renders `engine.systemLogs` in a `LazyColumn` (last 100, most-recent
  auto-scrolled into view), console-reel style, no Material chrome
- [x] **M2 Step 3:** all four channels (HOME/APPS/STATUS/LOG) reachable via the existing
  `ChannelStrip` → `transitionNavigation(...)`; back routes any channel → HOME
- [x] `runDevSimulation()` harness removed — real telemetry replaces it
- [ ] **M2 confirmed on Fold 6** (battery moves on plug/unplug, uptime counts, link reflects Wi-Fi/
  cellular; surface fills the unfolded display; grid shows more columns unfolded)  ← Director action
- [ ] M0/hue confirmed on Fold 6 (phosphor screen + hue switch live)  ← Director action
- [ ] Chakra Petch actually bundled (currently Monospace placeholder)  ← deferred to polish
  (`ui-text-google-fonts` dep + `font_certs.xml` stub are in place from the M0 infra branch)

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
- **M3 Stealth SFX-mute is a wired flag, not an audible change yet:** the app has no audio *player*
  yet (the engine only emits `audioCueStream` string tokens; playback lands in M6). Stealth sets
  `isStealthMode`, which the future player must check before sounding a cue. Honest stand-in, not a
  silent drop — flagged here so M6 honours it.
- **M3 Beacon/Stealth — watch on the Fold 6 (common foldable quirks, do NOT silently work around):**
  (1) `setTorchMode` can throw `CameraAccessException` if the flash camera is momentarily claimed or
  re-enumerated across a fold/unfold — calls are `runCatching`-wrapped and `onDispose` force-kills
  the torch, so we fail dark rather than crash or strand the light on; (2) `screenBrightness` override
  behaviour can differ on the inner vs cover display. Both are Director on-device checks.
- **M3 Core Temp gauge** maps the battery-temp stand-in across a 25–50°C field range onto 10
  segments — same locked stand-in as STATUS until kiosk/ROM grants true SoC thermal (spec §7.3).
- **M3 atom mark / flag are glyphs (`⚛`/`⚑`)**, matching the existing line-glyph working set
  (`◈`, `⊕`, `▲`); per-app SVG masters arrive at the later identity/polish stage.
- **M3 Vitality panel is HOME-channel-only by design** (scope boundary) — the "flick from anywhere"
  system-wide shade is deferred to kiosk mode (Bible decision 56). Not added to APPS/STATUS/LOG.
- Typography: replace `FontFamily.Monospace` with Chakra Petch (`res/font/chakra_petch.ttf` or
  Downloadable Fonts via the now-present `ui-text-google-fonts` dep + real certs in `font_certs.xml`).
- CRT: current overlay is the cheap non-shader fallback; real AGSL shader is M6 polish.
- **STATUS — connectivity is coarse by design (M2 hard stop):** connected/not + transport label
  only. No precise signal-strength bars (would need `READ_PHONE_STATE`) — deferred, not dropped.
  The engine's readiness composite uses a coarse signal proxy (connected→3, offline→0).
- **STATUS — storage breakdown not shown:** the M2 brief mentioned storage; this pass implements
  battery/uptime/connectivity/temp. Storage % is deferred as a known item (no blocker; `StatFs`/
  `StorageManager` can add it later) — not silently dropped.
- STATUS — `coreTempCelsius` is sourced from **battery** temperature (no-permission, real) as a
  stand-in for a true SoC thermal reading; revisit if a better no-permission source is wanted.
- Telemetry polls every 3s on the ViewModel scope (functional reactive, not an idle redraw loop);
  it runs app-wide so HOME's readout is live too.
- App icons (in the grid): loaded from PackageManager via Drawable→Bitmap conversion. No custom
  icon styling yet; icons render at system defaults. Deferred to polish milestone.
- **Samsung install gotcha (resolved):** One UI's package installer silently aborts if the APK
  has no `android:icon` (now fixed) AND can choke on odd download filenames — keep the artifact
  named `app-debug.apk`.
- Empty states: APPS grid shows "SCANNING PACKAGE REGISTRY…"; LOG shows "LOG REGISTER EMPTY".
- Action rail (bottom chrome strip) is not yet rendered — deferred (M3 delivered the Vitality
  panel; the App-Shell action-rail remains a later chrome item).
- `loadApps` is called once at launch; no refresh on app install/uninstall (later concern).

## Decisions pending (Director / Clara — do not lock in code)
- **Container fill-and-adapt** (M2 Step 0): now `forceFixedContainer=false` — surface fills the real
  screen, CRT falloff frames it. Director to confirm on the Fold this reads right vs the old letterbox.
- **APPS grid column count**: now `GridCells.Adaptive(minSize = 88.dp)` per the M2 brief's 88–96dp
  target. On the Fold 6 inner display this yields more columns than narrow; Director to judge the
  cell size / column count on-device.

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
- **Merge/M2 session (2026-06-23):** consolidated the three divergent branches into `main` (M1
  code + docs/skill/font infra + task briefs), resolving duplicates to the verified M1 versions.
  Then built M2: STATUS channel on real telemetry (battery/charging/temp via the
  ACTION_BATTERY_CHANGED sticky, uptime via `SystemClock.elapsedRealtime`, connectivity via
  `ConnectivityManager` — all feeding the existing `incomingTelemetryUpdate(...)` seam, with a
  UI-only transport label); LOG channel as a live `LazyColumn` console reel off `systemLogs`;
  Step 0 container/grid resolved (fill-and-adapt + adaptive 88dp grid); removed the dev-sim
  harness; added `ACCESS_NETWORK_STATE` (install-time, no prompt). Builds via CI (no local Android
  SDK in the cloud session). **Pending Director confirmation on the Fold 6** before M2 is closed.
- **M3 session (2026-06-23):** Vitality panel. Core (`QuantumState.kt`): added
  `VitalityState.readinessPercent` (composite headline) and three engine actions —
  `cyclePhosphorHue()`, `toggleStealthMode()`, `toggleBeacon()` (the latter carries the
  Beacon-drops-Stealth rule) — all reusing the single env mechanism; +5 unit tests (9/9 pass).
  UI (`LauncherUi.kt`): ⚛ atom mark on HOME with a stepped roll-down `VitalityPanel` (Zone 1
  Readiness + `SegmentedGauge` Signal/Power/Temp + 1s-ticking Uptime; Zone 2 Stealth·Phosphor·
  Beacon·Lock), the blinking `⚑ BEACON` flag, and a `LockOverlay` for the cosmetic securing beat.
  Platform side-effects wired in the Activity: Stealth → this-window `screenBrightness`; Beacon →
  `CameraManager.setTorchMode` (runCatching-wrapped, torch killed onDispose). Panel open/stow state
  held in the ViewModel (survives fold/rotate); signal upgraded to a coarse transport tier. No new
  permissions. Core verified locally in a JVM harness; full APK build is on CI. **Pending Director
  confirmation on the Fold 6** before M3 is closed.
