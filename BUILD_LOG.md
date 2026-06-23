# BUILD_LOG.md — QuantumOS code state

The code-side companion to the Build Bible. The Bible tracks *design decisions*; this tracks
*code state*, so each Claude Code session resumes exactly where the last stopped. Update the top
block at the end of every session.

---

## ▶ RESUME HERE
**Current milestone:** M2 — STATUS + LOG channels — ✅ code complete; **awaiting on-device
confirmation on the Fold 6** (the Step 0/4 checkpoints — especially the container fill check).
STATUS shows real battery/charging/uptime/connectivity/temp; LOG shows the live event console;
the surface fills the screen (no letterbox) and the APPS grid is adaptive.
**Goal of next session:** Start **M3 — Vitality panel** (atom roll-down; vitals; Phosphor/Stealth/
Beacon real, Lock cosmetic). Do NOT start M3 until the Director confirms M2 on hardware.

> **Branch consolidation (2026-06-23):** three divergent branches were merged into `main` — the
> M1 line (`gracious-thompson`, verified on Fold 6), the docs/skill/font-infra line
> (`epic-lamport`), and the task-brief line. `main` had never compiled (its `settings.gradle.kts`
> was a broken copy of the root build file and it had no `res/`); it now carries the verified M1
> code. Duplicates resolved to the verified M1 versions; broken `android-build.yml` and the stray
> root `LauncherUi.kt` dropped.

## Status
- [x] First green `gradle assembleDebug`  ← achieved during M1
- [x] `gradle test` — 4/4 passing
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
- Action rail (bottom chrome strip) is not yet rendered — M3 item.
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
