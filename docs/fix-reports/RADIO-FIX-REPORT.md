# RADIO — Core Apps Fix-Pass Report

*Docked into `:radio` (`com.quantumos.radio`), per Core Apps Fix-Pass Task Brief v1.0 (Decision 86).
Closes the findings in `QuantumRadioReceiver/docs/AUDIT-REPORT.md`. CI-green; confirmed on the Fold 6.*

## What changed

- **Docked into the shared App Shell.** The old app's chrome was entirely dead weight — a
  no-op "◄ APPS" back stub, a non-interactive channel-selector row, no registration marks at all
  beyond a stale comment. All of it is deleted, replaced by `:app-shell`'s `QuantumOSLayoutShell` +
  `NameplateHeader` via a thin local `ui/components/AppShell.kt` wrapper. No `BackHandler`; back is
  `onReturnHome = { finish() }` plus a real, working "◄ HOME" line — the first working
  back-navigation path this app has had.
- **The cryptographic signal-decoder feature is removed entirely — Director ruling.** RADIO stays a
  pure content-receiver; `InterceptControlPanel`, `CryptographicDecoderConsole`,
  `RadioViewModel.startSignalDecoding()`, and `GeminiClient.kt` are not ported. Nothing is lost —
  the standalone `QuantumRadioReceiver` repo preserves the removed code losslessly (never pushed to
  since the audit), and `docs/future-signal/radio-decoder.md` records exactly where (file paths +
  line numbers) for whoever eventually builds SIGNAL. `:radio` now carries no network dependency
  and no `INTERNET` permission at all as a direct result.
- **`SignalStaticCanvas`'s unconditional idle-redraw loop is bounded.** Previously
  `while(true) { delay(100); clockTrigger++ }` ran forever once triggered, regardless of whether
  anything changed again — now a bounded 8-tick (~800ms) settle burst triggered only by real
  `reception` changes. The static-noise visual is preserved; the infinite idle loop is not.
- **Needle and atom-mark motion swapped their bouncy `spring` for a non-bouncy settle**, matching
  the house style's stepped/non-bouncy motion language. The needle's underlying event-gating (only
  retargets on real `selectBand()`/`tuneFrequency()` calls) was already correct and is preserved —
  it never idle-jitters.
- **Dim-hue values are now correct automatically.** The old app hardcoded wrong dim-phosphor hex
  (`#006600` vs. spec `#00AA00` for green, etc.); using `Phosphor.dim(hue)` from `:app-shell`
  fixes this as a side effect of adopting the shared tokens, not a separate patch.
- **`--warn` decorative misuse fixed** — the permanently-visible tuning-pointer line no longer uses
  `Phosphor.Warn`; it uses the active phosphor hue instead, reserving warn-red for actual alerts.
- **Typography/palette**: `FontFamily.Monospace`/raw `Typeface.MONOSPACE` canvas text →
  `Fonts.ChakraPetch` (via `TextMeasurer`/`drawText`); all hardcoded hex → `Phosphor.*`. Rebranded
  `com.example` → `com.quantumos.radio`.
- **FM/AM/WX bands, presets, dial, and reception meter carry over unchanged** — they were already
  correct per the audit.

## Still open (known, flagged gaps — not oversights)

- **No shared icon library exists yet.** Stock Material icons remain in use, per the fix-pass
  brief's own explicit fallback.
- **Docked-module phosphor hue doesn't sync with the launcher's live selection** — same
  pre-existing limitation as the other three new modules and `:optics`/`:nav`.
- **`docs/future-signal/radio-decoder.md` flags two follow-up items** for whoever eventually builds
  SIGNAL: the old reception-meter label ("DECRYPT STABILITY") implied RADIO itself decoded signals,
  which was never accurate — SIGNAL's version should own that framing honestly; and
  `GeminiClient.decodeSignal()`'s API-key story was undocumented in the standalone repo and should
  be resolved before that capability ships anywhere.

## On-device confirmation

**Confirmed on the Fold 6 by the Director** — works fine, alongside COMMS/FILES/AUDIO.
