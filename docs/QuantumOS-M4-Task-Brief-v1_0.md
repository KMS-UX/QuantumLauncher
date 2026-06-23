# QuantumOS — M4 Task Brief: Floating QUARK Trigger
*For Claude Code. Companion to the Launcher Build Spec (v1.1), `CLAUDE.md`, and the M1–M3 Task Briefs. Version 1.0.*

> Read `CLAUDE.md` first if this is a new session. M1–M3 are closed and confirmed on hardware. This
> milestone is the **first to request a new system-level permission** — "draw over other apps" —
> so it gets the same explicit-script treatment M1's HOME intent got. **It is already pre-approved**
> in the Launcher Build Spec §5 as a one-time Settings toggle; this brief is how we execute it cleanly.

## Goal
A small, persistent QUARK mark that floats over every app — including apps outside QuantumOS —
sized to a normal app-icon footprint, static at rest, draggable, that snaps to an edge when released
and plays the familiar PLEASE STANDBY beat on tap.

## The scope boundary — read this first
**M4 builds the trigger. M5 builds what's behind it.** Tapping the trigger in this milestone should
play the PLEASE STANDBY beat and open a **placeholder stub screen** — not the real QUARK Assistant
View. The reactive states, conversation log, command rail, text entry, and scripted-brain wiring are
explicitly **M5's** job (Build Bible decision 39). Building them now would be working ahead of spec.

---

## Step 0 — The permission walkthrough
This is the equivalent of M1's rollback rehearsal: a real system capability, handled deliberately.

- Check `Settings.canDrawOverlays(context)`. If **not** granted, send the user to the system
  settings screen via `Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))`
  — this permission **cannot** be requested through a normal runtime-permission dialog; it only
  works through this Settings screen.
- Re-check the permission when the app resumes (the user grants it outside the app, then returns).
- **Note for the Director (put this in your session report):** this is a **one-time toggle** in
  Settings — "Allow display over other apps" — already anticipated in the build spec. Nothing else
  changes; this is the only new permission this milestone touches.

**Verify:** with the permission not yet granted, tapping whatever triggers this flow correctly opens
the system settings screen; after granting and returning, the app detects it without needing a restart.

## Step 1 — The overlay itself
- Implement this as a **foreground Service** that adds a `TYPE_APPLICATION_OVERLAY` view via
  `WindowManager` — not an overlay tied to the Activity's lifecycle, which won't reliably survive
  switching to other apps. A foreground service needs a small persistent notification; keep it
  minimal and unobtrusive for now (polish later).
- **Size:** a standard app-icon footprint — roughly what one of your own APPS-grid icons already
  renders at. Not larger.
- **Visual:** a simple placeholder mark for now — e.g. a circular "iris" shape in the active phosphor
  color, static. This is **not** the final QUARK mascot art; that's a later identity/polish-stage
  item (per decision 60's icon split) — don't block this milestone waiting on final art.
- **Static at rest** — no idle bob, no blink, no ambient animation (decision 41). It reacts only
  when touched.

**Verify:** the mark appears on screen, in the phosphor color, doing nothing until touched.

## Step 2 — Draggable, with edge-snapping
- While dragging, the mark follows the finger directly (1:1, real-time — this is the one place
  "stepped" motion doesn't apply; a draggable element should feel immediate).
- On release, it **snaps to the nearest screen edge** (left or right) — a quick, decisive settle
  rather than slow elastic easing, matching the mechanical motion language.
- **Default park position** (first launch, before the user has ever dragged it): pick a spot that
  avoids the bottom-center system gesture area and the top status bar — e.g. right edge, roughly
  mid-height. Note in `BUILD_LOG.md` that avoiding a *companion app's* primary control (e.g. a future
  camera shutter) is a forward concern noted in the Bible (the per-app companion apps don't exist
  yet, so this isn't testable today — don't block on it, just don't forget it).

**Verify:** drag the mark anywhere on screen, release, and it settles cleanly against the nearest edge.

## Step 3 — Tap behavior (the M4/M5 boundary)
- Tap → play the existing **PLEASE STANDBY** component (reused from M0, not rebuilt).
- That beat resolves into a **placeholder full-screen Activity** — something as simple as the
  phosphor background plus a line of text acknowledging the real QUARK Assistant View is coming at
  M5, with a clear way back (a back press or a tap) to whatever app was in the foreground.
- Do **not** build reactive states, a conversation log, a command rail, or text entry here — that
  content is explicitly M5's scope.

**Verify:** tapping the trigger plays the beat, opens the placeholder, and backing out returns
correctly to whatever app was running underneath.

## Step 4 — Confirm it actually hovers over other apps
- Open a real app other than QuantumOS (anything from the APPS grid, or a stock app like Settings).
- Confirm the trigger is still visible, still draggable, and tapping it still works from inside that app's context.

**Verify:** this is the milestone's headline check — a launcher feature is table stakes; an overlay
that survives leaving your own app is the actual new capability being proven.

## Step 5 — Session close
Update `BUILD_LOG.md`: mark items done/not-done, note the default park position and the
companion-app-control forward concern, and write the "resume here" line pointing at **M5 — QUARK
Assistant View** (which will replace this milestone's placeholder stub with the real content).

---

## Hard stops — do not do these in M4
- **Do not build the real QUARK Assistant View** (reactive states, conversation log, command rail,
  text entry, scripted-brain wiring) — that's M5. A placeholder stub is correct here.
- **Do not finalize QUARK's mascot art** — a simple placeholder mark is correct; full art is a later
  identity/polish-stage item.
- **Do not request any permission beyond the overlay permission** this milestone.
- **Do not invent QUARK dialogue** — the placeholder stub needs at most one line acknowledging M5 is
  coming; it isn't a QUARK-voice surface yet.

## What "M4 done" looks like to the Director
A small static phosphor mark floats over the launcher *and* over at least one other real app you
opened to check. It's draggable and settles to an edge on release. Tapping it plays the familiar
PLEASE STANDBY beat and opens a simple placeholder screen, and backing out returns you cleanly to
whatever you were doing. The overlay permission was granted through the expected one-time Settings toggle.

---
*End of M4 Task Brief v1.0. Report back to Clara per the Step 0/4 checkpoints — particularly the
"hovers over a real other app" check — before the Bible is bumped to mark M4 closed.*
