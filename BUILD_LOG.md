# BUILD_LOG.md — QuantumOS code state

The code-side companion to the Build Bible. The Bible tracks *design decisions*; this tracks
*code state*, so each Claude Code session resumes exactly where the last stopped. Update the top
block at the end of every session.

> **Before writing code, run the pre-work pass in [`SESSION-PLAYBOOK.md`](SESSION-PLAYBOOK.md)** —
> the fixed orient → recon → plan → verify → close ramp distilled from M1–M4. Start there, then read
> `RESUME HERE` below.

---

## ▶ RESUME HERE
**Current milestone:** M5 — QUARK Assistant View — ✅ **code complete**; **awaiting on-device
confirmation on the Fold 6.** The floating trigger now opens the REAL assistant
(`QuarkAssistantActivity`), not the old stub: a large central reactive presence (the four locked
states), a one-line state caption, a scrolling **conversation log** (its own list, separate from the
M2 LOG channel), the six-action **command rail**, and **free-text entry**. Every line QUARK speaks
is wired **verbatim** from the Scripted-Line Library v1.1 via a shared `QuarkParser` — the M0–M4
placeholder lines are gone. The launcher and the assistant share **one** engine (`QuantumRuntime`)
so phosphor hue + Stealth carry over and the four reused rail actions behave exactly as their M3
originals.
**Goal of next session:** Start **M6 — splash, sound, and CRT-shader/motion polish.** Do NOT start
M6 until the Director confirms M5 on hardware (see the M5 verify checklist below).

> **Director action — crisis-tier resource string (M5 Step 1):** the Distress/crisis intent renders
> a real resource line beneath QUARK's words as plain UI text. It is a **Config-settable string,
> empty by default**, and currently shows a **safe generic fallback** ("contact your local emergency
> services / a person you trust") because no concrete resource is configured. **Which
> region-appropriate hotline/number to ship is the Director's call** — it was deliberately NOT chosen
> in code. Set it via `engine.setCrisisResourceLine(...)` (a CONFIG surface to expose it lands
> later). The feature ships working in its fallback form from first boot, exactly as required.

> **M5 verify on the Fold 6 (Director):** (1) tap the trigger → real assistant opens (PLEASE STANDBY
> → "Reading the field…" Scan→Idle), not the stub; (2) the four states are visually distinct — Scan
> on open/processing, Happy/Warn/Idle per intent; (3) all six rail buttons work, and Status/Stealth/
> Phosphor/Beacon behave identically to M3 (Beacon still drops Stealth; Stealth still dims; Phosphor
> still recolours everywhere); (4) typed phrases land on sensible intents and nonsense → graceful
> Fallback; (5) **safety check, once:** type an everyday-down line (→ harbor: calm, no Warn, no
> sound) and separately a genuine self-danger line (→ crisis: **Idle not Warn**, no sound, her line +
> the fallback resource box beneath it); (6) switch hue / toggle Stealth in the Vitality panel, THEN
> open the assistant — both already reflect the current state, not reset.

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
- **M5 audio cues are tokens only (no player yet):** `quarkSay` emits the library's cue tokens
  (`chirp_scan`, `chirp_happy`, `chirp_warn`, `confirm_granted`, `blip_beacon`, `sweep_phosphor`) to
  `audioCueStream`. There is still no player — playback (and Stealth's mute gate) lands in M6. Crisis
  and harbor correctly emit **no** token at all.
- **M5 `{limiter}` slot deferred (per the library §1 director note):** status DEGRADED/CRITICAL use
  the library's no-limiter variants, so those two bands have a single variant (no back-to-back
  rotation needed — they only fire on a degraded device). NOMINAL has its full 3-variant rotation.
  Computing the limiter is cheap and derived (no new sensor) if Clara/Director later wants it.
- **M5 boot "Online" line not wired (nice-to-have, brief Step 6):** the §6 Online line was left for
  M6's boot-sequence polish to avoid firing it before telemetry has a first reading. The open/stow
  session lines ARE wired.
- **M5 typed "lock" triggers the real cosmetic-lock beat** (reuses M3 `executeCosmeticLockSequence`)
  in addition to speaking the LOCK line, so the line is truthful. The DEVICE SECURED overlay then
  shows on the launcher beneath; stow the assistant to reach it. Lock is intentionally NOT one of the
  six rail buttons (the rail is the locked six: Status/Stealth/Phosphor/Beacon/Say/Warn).
- **M5 Stealth carryover (brief Step 7) — fixed:** the assistant is a separate Activity, so the
  window-level `screenBrightness` dim does NOT inherit across windows. `QuarkAssistantActivity`
  re-applies it from the shared engine state in a `LaunchedEffect(isStealthMode)`, the same way the
  launcher does. Phosphor hue carries over automatically (both read the one `QuantumRuntime` engine).
- **M5 engine is now a process singleton (`QuantumRuntime`):** promoted out of the ViewModel so the
  launcher and the assistant Activity share one engine/parser/telemetry. Telemetry runs on an
  app-scoped coroutine (not the ViewModel scope). Watch on-device that it isn't double-started or
  leaking — both entry points guard with idempotent flags.
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
- **M5 session (2026-06-24):** QUARK Assistant View. Core (`QuantumState.kt`): replaced the
  placeholder `ScriptedLineLibrary` with the verbatim Scripted-Line Library v1.1 (all six rail
  intents, the §4 keyword categories, §6 open/stow lines), each with its 2-3 rotating variants +
  reactive posture + sound cue; per-(intent,mode) "don't repeat the last variant" rotation. Rewrote
  `QuarkParser` to classify typed input (Distress matched narrowly + priority-first, then
  most-specific) and route everything through one `engine.quarkSay` speak beat that runs the
  Scan→result thinking beat and records a distinct `conversationLog` (separate from `systemLogs`).
  Added `OperatorConfig` (operator name + crisis-resource string, both empty by default),
  `ConversationEntry`, caption + crisis flag on `QuarkBrainState`, and `effectiveCrisisResource()`
  (safe generic fallback). Crisis + harbor are Idle-only, no sound, never Warn; crisis flags the
  resource line. +9 unit tests (18/18). UI: new `QuantumRuntime` process-singleton holding the one
  engine/parser/telemetry so the launcher + assistant share state; `QuantumViewModel` slimmed to
  delegate. New `QuarkAssistantActivity` (replaces `QuarkStubActivity`) — central `QuarkPresence`
  with the four stepped reactive states, state caption, scrolling conversation log with the crisis
  resource box, six-action command rail (4 reused M3 actions + Say/Warn), and a phosphor `BasicTextField`
  entry. Stealth re-applied per-window (Step 7); hue carries over via shared state. Manifest + service
  point at the new Activity. Built on CI (no local Android SDK). **Pending Director confirmation on
  the Fold 6** + the crisis-resource string decision before M5 is closed.
