# QuantumOS — M7 Task Brief: Ship + Field-Test
*For Claude Code. Companion to the Launcher Build Spec (v1.1), `CLAUDE.md`, and the M1–M6 Task
Briefs. Version 1.0. This is the final milestone before Checkpoint β.*

> Read `CLAUDE.md` first if this is a new session. M1–M6 are closed and confirmed on hardware,
> including the Deployment Region patch. **This milestone is shaped differently from the others.**
> Its central artifact is a signing key, not a feature — and unlike code, a lost or leaked signing
> key cannot be fixed in a later session. Treat Step 0 with the same care M1's rollback rehearsal
> and M4's permission walkthrough got.

## Goal
A real, signed **release** build (not debug) installed on the Fold 6, with a full on-device
regression pass confirming every milestone still works correctly in that release build — the
combination of which is **Checkpoint β**.

## Before anything else: read the actual current project state
Earlier scaffolding (package name, Kotlin/Compose/SDK versions) may no longer match what's actually
in the repo after six milestones of real building. **Use whatever is actually in `app/build.gradle.kts`
right now** — applicationId, compileSdk/targetSdk, version line — don't assume an earlier brief's
specifics still hold. This step is about working from the real current file, not re-deriving it.

---

## Step 0 — The signing key ceremony
This key is the one truly irreversible artifact in the project so far.

- Generate a release signing **keystore** (via `keytool`, or Gradle's signing config — either is fine).
- **This file, and its passwords, must be backed up by the Director outside any cloud/ephemeral
  environment** — not left only inside a Codespace or an Actions runner, which can be wiped. Losing
  it means QuantumOS can never receive a clean update again; the only recovery is uninstalling and
  reinstalling under a new key, which **wipes all app data** (including the persisted settings added
  in M6) and forces switching away from QuantumOS as Home first, if it's currently set.
- **Note for the Director (put this plainly in your session report):** once this keystore is
  generated, please copy it and its passwords somewhere durable (a password manager, encrypted
  backup) before going further. This is the one step in the whole build where losing the artifact
  has a real, lasting cost — everything else so far has been recoverable by rebuilding.
- Wire `app/build.gradle.kts`'s `release` build type to use this signing config.

**Verify:** the keystore exists, is backed up outside the ephemeral build environment, and the
release build type references it correctly.

## Step 1 — Build the signed release APK
- Run the release build (e.g. `gradle assembleRelease`).
- Keep `isMinifyEnabled = false` for this first signed release — don't introduce code shrinking/
  obfuscation in the same milestone as a new signing key; that's two new variables at once if
  something goes wrong. Minification can be a deliberate, separate decision later.
- Confirm the output is genuinely signed (a successful install on-device is sufficient proof —
  Android refuses to install an improperly signed APK).

**Verify:** `assembleRelease` completes and produces an APK file.

## Step 2 — Sideload (read this before doing it)
A few practical things that will otherwise look like bugs:

- **Installing the new signed release APK over the existing debug-signed install will fail** —
  Android blocks installing a different signing certificate over the same package name. **Uninstall
  the current debug build first.**
- If QuantumOS is currently set as the Fold's default Home, uninstalling it will need the phone to
  fall back to the stock launcher — this is exactly the rollback path rehearsed back at M1; it's
  expected to work, and this is a real-world use of it, not just a drill this time.
- Uninstalling **wipes the persisted settings from M6** (Deployment Region, Boot Pace) — expected
  and one-time, not a regression. Note it so it isn't alarming when STATUS shows the defaults again
  on first launch of the new build.
- Install the new signed APK, and set it as Home again when ready.

**Verify:** the signed release build installs cleanly and launches.

## Step 3 — The field-test (this is the Director's pass, not Claude Code's)
Claude Code's job here is to prepare the build and this checklist — the actual tapping-through
happens on the Fold 6, by the Director, since that's literally what "field-test" means. Walk through:

- **Boot** — a true cold boot (restart or force-stop + relaunch) plays the full M6 ceremony and resolves to Home.
- **Home / Apps / Status / Log** — all four channels reachable and working (M1–M2).
- **Vitality panel** — atom-mark roll-down; Stealth, Phosphor, Beacon (with its Stealth-override rule), and Lock all behave as before (M3).
- **Floating QUARK trigger** — present, draggable, survives switching to another app (M4).
- **QUARK Assistant View** — opens from the trigger, all four reactive states fire correctly, the
  six-action rail works, free-text entry gets sensible replies, and — once — the crisis-tier
  behavior stays calm and shows the configured resource (M5).
- **Deployment Region + Boot Pace** — both default correctly on this fresh install, both toggle and
  persist correctly across a restart of the new build.
- **Look and feel** — the real CRT shader and synthesized sound are present throughout.

**Verify:** every item above works in the signed release build, not just in the debug builds tested
across M1–M6. A release build can behave subtly differently from debug — this pass is what confirms it didn't.

## Step 4 — Session close
Update `BUILD_LOG.md` with the field-test results. If everything in Step 3 passes, this is the
moment to write **Checkpoint β reached** — note it clearly; it closes out the entire Launcher App
phase of the project, not just one milestone.

---

## Hard stops — do not do these in M7
- **Do not** enable minification/obfuscation in the same pass as the new signing key.
- **Do not** skip the keystore-backup note to the Director — it's the one step this milestone exists to get right.
- **Do not** start any Trident/kiosk-phase work — that begins only after Checkpoint β is confirmed, in a future session.

## What "M7 done" looks like to the Director
A signed release APK, installed fresh on the Fold 6 (after uninstalling the old debug build and
confirming you still know the way back to stock Home if needed), with everything from every prior
milestone working correctly — and the signing keystore safely backed up somewhere that isn't this
build environment. That combination is Checkpoint β.

---
*End of M7 Task Brief v1.0. Report back to Clara with the field-test results — and confirmation the
keystore is backed up — before the Bible is bumped to mark M7 closed and Checkpoint β reached.*
