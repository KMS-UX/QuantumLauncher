# QuantumOS — M3 Task Brief: Vitality Panel
*For Claude Code. Companion to the Launcher Build Spec (v1.1), `CLAUDE.md`, and the M1/M2 Task Briefs. Version 1.0.*

> Read `CLAUDE.md` first if this is a new session. M1 and M2 are closed and confirmed on hardware
> (fill-and-adapt + adaptive grid, real STATUS/LOG). This milestone is **already fully specified** in
> the Build Bible (§ Vitality panel, decisions 34–38, 56) — this brief is that spec translated into
> an execution order, not a new design. Work in order; verify each step.

## Goal
A panel that **rolls down from a tap on a quantum-atom mark** on the Home channel, showing real
vitals at a glance (Zone 1) and four quick actions (Zone 2) — three of them real, one cosmetic.

## Scope boundary — read this first
This build's Vitality panel lives **on the Home channel only**. It is **not** a system-wide
notification shade reachable from every screen — that "flick from anywhere" version is explicitly
deferred to kiosk mode (Build Bible decision 56). Do not add the atom-mark pull to APPS/STATUS/LOG.

---

## Step 0 — The atom mark + roll-down mechanics
- Add the quantum-atom mark to the **Home channel** header (new element this milestone — it
  doesn't exist yet in the M0–M2 surfaces).
- **Static at rest** (no idle animation), **one stepped spin on open** — functional motion, not decoration.
- **Tap to open**: the panel rolls down with **stepped motion** (discrete steps, not a smooth
  interpolated slide — match the stepped language already used for the boot sequence).
- **Tap the mark again, OR a dedicated STOW handle on the open panel, closes it.** Build both.

**Verify:** tap opens with a visible stepped roll-down (not a smooth slide); both close methods work.

## Step 1 — Zone 1: vitals at a gaze (read-only)
Reuse `engine.masterState.vitality` — already populated since M2 (battery, uptime, connectivity).
Don't add a second data path.

- **Readiness (composite headline)** — the engine already computes `SystemReadiness`
  (NOMINAL/DEGRADED/CRITICAL) from power+signal+temp. Just render it: a % feel is fine, plus the
  word. **CRITICAL renders in `--warn` red** — this is the one place warn-red is used here.
- **Build one reusable "segmented gauge" component** (a short row of filled/unfilled segments +
  a numeric value, in-house style — phosphor color/glow, no Material `LinearProgressIndicator`).
  Use it for **Signal**, **Power**, and **Core Temp**.
- **Core Temp** = battery temperature, read from the **same battery broadcast receiver M2 already
  set up** (the `EXTRA_TEMPERATURE` field, in tenths of a °C) — don't add a second receiver. This is
  the locked stand-in until kiosk/ROM grants true SoC thermal (spec §7.3).
- **Signal** — approximate, don't request a precision permission. A coarse tier from
  `ConnectivityManager`'s active transport (e.g. Wi-Fi connected = high tier, cellular only = mid,
  neither = low) is enough for this milestone — precise dBm signal strength is intentionally
  deferred (this matches the build spec's own stated approach, §5).
- **Uptime** — the only vital that ticks continuously (it's a clock); the others update on real
  events, not a polling timer.

**Verify:** open the panel, see Readiness + four gauges with real numbers; force a state change
(e.g. unplug charger, toggle Wi-Fi) and confirm the relevant gauge updates.

## Step 2 — Zone 2: the four quick actions (2×2)
Per Bible decision 36, in this exact order: **Stealth · Phosphor · Beacon · Lock.** (The Comms slot
stays parked — don't fill a 5th action.)

- **Phosphor** — cycle the hue (green → amber → cyan → green…) live across the whole UI. Reuse the
  existing hue-cycling already in `QuantumState.kt`/`QuarkParser` — don't write a second hue mechanism.
- **Stealth** — dim **hard** but keep **full phosphor color saturation** (dim brightness, don't
  desaturate the colors), and mute the app's own sound effects. Implement the dim via this
  window's `screenBrightness` (no special permission needed) — not a system-wide brightness change.
  One tap toggles it; fully reversible.
- **Beacon** — toggle the real torch via `CameraManager.setTorchMode` (no permission required for
  this call — confirmed in the build spec §5), **and** show a blinking warn-red field-flag element
  on the Home channel while active. **Interaction rule (designed default — note it, don't silently
  bury it):** if Stealth is active when Beacon is turned on, auto-disable Stealth — signaling takes
  priority over staying low-emission. Log this rule in `BUILD_LOG.md` so it's visible, not assumed.
- **Lock** — call the **existing** `engine.executeCosmeticLockSequence()` / `unlockDeviceProfile()`.
  This is already implemented from M0/M1 — wire the button to it, don't rebuild it, and don't add
  Device Admin or a real `lockNow()`. Cosmetic only, per decision 56.

**Verify:** each of the four actions does what's described above; Beacon-while-Stealth correctly
drops Stealth; Lock plays the existing PLEASE STANDBY → DEVICE SECURED beat and unlocks correctly.

## Step 3 — Session close
Update `BUILD_LOG.md`: mark items done/not-done, note any platform surprises (e.g. if torch or
window-brightness behaves oddly on the Fold 6 — both are common foldable quirks worth flagging, not
silently working around), note the Beacon/Stealth interaction rule explicitly, and write the
"resume here" line pointing at **M4 — floating QUARK trigger**.

---

## Hard stops — do not do these in M3
- **No system-wide notification-shade reach** — Home-channel-only, per the scope boundary above.
- **No new permissions** — no Device Admin, no `WRITE_SETTINGS`, no camera-permission prompt (torch
  via `setTorchMode` doesn't need one); no precise signal-strength permission.
- **Do not touch the floating QUARK trigger** — that's M4.
- **Do not build the QUARK Assistant View** — that's M5.
- **Do not invent QUARK dialogue** — this milestone has no QUARK-voice surfaces.

## What "M3 done" looks like to the Director
Tap the atom mark on Home → the panel rolls down (stepped, not smooth) showing real Readiness,
Signal, Power, Core Temp, and a ticking Uptime. Stealth dims the screen without washing out the
phosphor color and is fully reversible. Phosphor cycles the hue. Beacon turns on the flashlight and
shows a blinking red flag (and turns Stealth off if it was on). Lock plays the familiar securing
beat. Tapping the mark again or the STOW handle closes the panel.

---
*End of M3 Task Brief v1.0. Report back to Clara per the Step 0/3 checkpoints — particularly the
on-device gauge-accuracy and Beacon/Stealth check — before the Bible is bumped to mark M3 closed.*
