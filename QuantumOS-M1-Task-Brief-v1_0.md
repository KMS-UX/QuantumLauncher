# QuantumOS — M1 Task Brief: Launcher Core
*For Claude Code. Companion to the Launcher Build Spec (v1.1) and `CLAUDE.md`. Version 1.0.*

> **Why this brief exists.** Every other milestone so far has been a normal app. **M1 is different: it
> is the step where this app can become the Fold 6's actual Home screen** — the device the Director
> uses daily. That earns an explicit, ordered script instead of an open "build M1" instruction.
> **Work the steps in order. Do not skip Step 0. Stop and report after each step's verify line.**

## Goal
By the end of M1: the app declares itself as a Home app, has a working **APPS grid** that lists and
launches real installed apps, and the Director can confidently set it as default **and revert** — on
a phone they use every day.

## Inputs you already have
- M0 is confirmed working on hardware (phosphor screen, hue switch, stable) — build on it; don't restart.
- `CLAUDE.md` (root) — read it again now; the design-language and platform rules apply to every file you touch.
- `docs/Launcher-Build-Spec-v1.1.md` §0 ("the platform envelope") — re-read before writing the HOME intent; it documents exactly what a launcher can/can't do on a locked bootloader.

---

## Step 0 — Rollback rehearsal (do this BEFORE touching the manifest)
This is non-negotiable and comes first, per Build Bible decision 61.

1. In `BUILD_LOG.md`, write a short **"Rollback confirmed"** line once you've verified the Director
   knows the manual path back to a normal home screen: *Settings → Apps → Default apps → Home app →
   select the stock/other launcher.* (You can't test this yourself without a device session — note it
   as a Director action if you can't drive the device directly, and don't proceed past Step 2 until
   it's acknowledged.)
2. Confirm the current manifest does **not yet** declare `category.HOME` — it shouldn't, per the
   guardrail in `CLAUDE.md`. If it somehow does, stop and flag it before continuing.

**Verify:** `BUILD_LOG.md` has the rollback line, and you've confirmed HOME is not yet declared.

## Step 1 — Declare the launcher intent (additive, not destructive)
Add to the existing `LauncherActivity` entry in `AndroidManifest.xml` — **add** the category, don't
replace the existing `MAIN`/`LAUNCHER` intent-filter:
```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.HOME" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```
Note for the Director (put this in your session report, not just the log): installing this build will
make Android **offer** QuantumOS as a Home option — it does not silently take over. The system will
either prompt "Use as Home" or require manually switching it in Settings. Nothing changes until the
Director chooses.

**Verify:** builds and installs without becoming the active Home automatically. Pressing the device's
own Home button still goes to the existing launcher unless the Director has explicitly switched.

## Step 2 — Package visibility (Android 11+ requirement)
To list other apps, declare what you intend to query in `AndroidManifest.xml` (outside `<application>`):
```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```
Without this, `PackageManager` queries for launchable apps will return **empty or partial** results on
modern Android — a likely silent failure if skipped.

**Verify:** a quick log/print of the queried package list at startup shows more than zero entries.

## Step 3 — The APPS grid (real apps, real launch)
In `com.quantumos.shell.ui`, build an **APPS** channel screen (per the existing `NavigationChannel.APPS`
enum already in `QuantumState.kt` — wire into it, don't duplicate it):
- Query installed launchable apps via `PackageManager` (using the `<queries>` filter above): label, icon, package name.
- Render as a grid **inside the existing App Shell** — house-style chrome (opaque nameplate header,
  the established body pattern), **not** a default `LazyVerticalGrid` with Material defaults dropped
  in raw. Pull spacing/type from the House Style skill tokens.
- Tapping an entry launches it via `startActivity` with that app's launch `Intent`.
- Use the existing `transitionNavigation(NavigationChannel.APPS)` call to switch into this channel from Home — don't invent a second navigation mechanism.

**Verify:** from the Home channel, navigate to APPS, see a real list of installed apps (icons may be
default for now — that's fine), tap one, and it opens.

## Step 4 — Home channel pass
Confirm the Home channel (the existing M0 phosphor surface) is reachable from APPS and back via the
existing channel selector — this is the "rough-but-real home screen" bar from the spec, not a finished
UI. Don't add new visual elements here; M1 is about wiring, not polish.

**Verify:** Home ⇄ APPS navigation works both directions.

## Step 5 — Session close
Update `BUILD_LOG.md`:
- Mark M1's checklist items done/not-done.
- Note anything you deferred (icon styling, empty-state handling, etc.) as "known issue," not silently dropped.
- Write the exact **"resume here"** line for the next session (should point at M2 — STATUS + LOG channels — once M1 is confirmed on-device).

---

## Hard stops — do not do these in M1
- **Do not** request `QUERY_ALL_PACKAGES` permission — the `<queries>` declaration in Step 2 is the
  correct, Play-policy-safe mechanism for "list launchable apps." `QUERY_ALL_PACKAGES` is a heavier,
  reviewed permission we don't need here.
- **Do not** implement Device Admin / kiosk pinning here — that's Trident Pillar ②, a separate track, not M1.
- **Do not** touch the floating QUARK trigger (overlay) — that's M4.
- **Do not** silently invent QUARK dialogue if you add any status text — M1 is launcher mechanics, not QUARK surfaces; if QUARK needs to say anything here, pull it from the Scripted-Line Library seam already in `QuantumState.kt`, don't write new lines inline.

## What "M1 done" looks like to the Director
A debug APK that: can be selected as Home (and un-selected back to stock, confirmed by hand), shows
a real grid of the Director's installed apps, and lets them tap into any app and back to the
QuantumOS Home channel. Rough is fine. Broken-home-screen is not.

---
*End of M1 Task Brief v1.0. Hand this to Claude Code as the working script for the milestone; report back to Clara per the Step 0/5 checkpoints for review before the Bible is bumped to mark M1 closed.*
