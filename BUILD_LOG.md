# BUILD_LOG.md — QuantumOS code state

The code-side companion to the Build Bible. The Bible tracks *design decisions*; this tracks
*code state*, so each Claude Code session resumes exactly where the last stopped. Update the top
block at the end of every session.

---

## ▶ RESUME HERE
**Current milestone:** M1 — Launcher core — complete (pending Director on-device confirm).
**Goal of next session:** Confirm M1 on Fold 6 (APPS grid shows real apps, tap-to-launch works,
HOME ⇄ APPS nav works both directions, QuantumOS offered as Home option in picker). Then start M2
(STATUS + LOG channels: real battery/uptime/storage + event log).

## Status
- [x] First green `gradle assembleDebug`  ← M0
- [x] `gradle test` — 4/4 passing          ← M0
- [ ] M0 confirmed on Fold 6 (phosphor screen + hue switch live)  ← Director action
- [ ] Chakra Petch actually bundled (currently Monospace placeholder)  ← deferred to M2/polish
- [x] HOME intent-filter added (M1 Step 1) — will offer QuantumOS as Home picker option on install
- [x] `<queries>` package-visibility block added (M1 Step 2)
- [x] APPS channel: queries real installed apps, renders grid, tap-to-launch (M1 Step 3)
- [x] HOME ⇄ APPS navigation wired via `transitionNavigation` (M1 Step 4)
- [ ] M1 confirmed on Fold 6  ← Director action

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
- App icons: loaded from PackageManager via Drawable→Bitmap conversion. No custom icon styling
  yet; icons render at system defaults. Deferred to polish milestone.
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
  back gesture routing. All Steps 0–5 complete.
