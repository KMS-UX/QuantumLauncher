# AUDIO — Core Apps Fix-Pass Report

*Docked into `:audio` (`com.quantumos.audio`), per Core Apps Fix-Pass Task Brief v1.0 (Decision 86).
Closes the findings in `QuantumAudio/docs/AUDIT-REPORT.md`. CI-green; confirmed on the Fold 6.*

## What changed

- **Docked into the shared App Shell.** AUDIO already had the best-organized private chrome of the
  four apps (its own local `AppShell.kt`), but it was still a private reimplementation — deleted
  entirely and replaced by `:app-shell`'s `QuantumOSLayoutShell` + `NameplateHeader` via a thin
  local `ui/components/AppShell.kt` wrapper, matching `:optics`/`:nav`. No `BackHandler`; back is
  `onReturnHome = { finish() }` plus an explicit "◄ HOME" line. The old PIN-lock screen (which
  misused the "PLEASE STANDBY" card as a lock-screen widget, hardcoded PIN `"1950"`) is dropped —
  the launcher already owns a real Vitality/Stealth/Lock system at the OS level.
- **The fake idle-drift waveform is deleted outright — the fix-pass's #1 required fix, and a
  confirmed instance of the audit's predicted top risk.** The old `RecorderScreen.kt` kept a
  `while (!isRecording) { val restingValue = sin(t)*0.08f + 0.12f; ...; delay(80) }` loop running
  forever whenever not recording, continuously injecting fake sine-wave data into the oscilloscope.
  The ported version's `LaunchedEffect(isRecordingState)` has no idle branch at all: when recording
  stops, the effect's key flips, the collecting coroutine cancels, and the trace freezes exactly
  where it is. Confirmed static at rest, both via source review and the Fold 6 pass.
- **All 7 `QuarkMascot` animations now gate on real reactive state.** Previously one
  `rememberInfiniteTransition` drove idle-bob/happy-hop/happy-tilt/warn-shake-x/warn-shake-y/
  scan-line/expanding-ring continuously regardless of `quarkState`, with only the rendered output
  branched. Each posture now gets its own `rememberInfiniteTransition`, created only while that
  posture is actually active — nothing animates when the mascot is idle.
- **Player screen's vinyl-spin transition now only exists while `isPlaying`** — previously the
  transition object ticked in the background even when frozen visually at rest; now it isn't
  created at all unless playback is active.
- **REC-pulse indicator** is likewise gated on `isRecordingState` — no pulse animation object
  exists while not recording.
- **Typography/palette**: `FontFamily.Monospace` → `Fonts.ChakraPetch`; the ~25+ scattered
  `--warn`/CRT-ground hex redefinitions are gone, replaced by `Phosphor.bright/dim/Warn/Crt`.
  Rebranded `com.example` → `com.quantumos.audio`.
- **Recorder-first navigation preserved** — the app still defaults to and launches on the Recorder
  screen, matching the standalone app's already-correct behavior.

## Still open (known, flagged gaps — not oversights)

- **No shared icon library exists yet.** Stock Material icons (including all transport controls)
  remain in use, per the fix-pass brief's own explicit fallback.
- **Docked-module phosphor hue doesn't sync with the launcher's live selection** — same
  pre-existing limitation as the other three new modules and `:optics`/`:nav`.
- The Player screen's full turntable/DJ-deck experience (vinyl visualizer, pitch slider) was kept
  as a co-equal top-level tab, unchanged from the standalone app — the audit flagged this as
  competing for surface area with the recorder-first identity, but the fix-pass brief only called
  for the idle-loop fix, not a scope reduction of Player itself; flagging for Director judgment on
  whether Player deserves a follow-up pass.

## On-device confirmation

**Confirmed on the Fold 6 by the Director — the waveform is static at rest, not just in code, per
the fix-pass brief's own acceptance bar.** Works fine, alongside COMMS/FILES/RADIO.
