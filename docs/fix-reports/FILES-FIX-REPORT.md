# FILES — Core Apps Fix-Pass Report

*Docked into `:files` (`com.quantumos.files`), per Core Apps Fix-Pass Task Brief v1.0 (Decision 86).
Closes the findings in `QuantumFiles/docs/AUDIT-REPORT.md`. CI-green; confirmed on the Fold 6.*

## What changed

- **Docked into the shared App Shell.** The old private chrome (custom header, `EXPLORER/TERMINAL/
  DECRYPT/QUARK` bottom tabs built from scratch, CPU scanline/flicker, the PIN-lock overlay
  misusing "PLEASE STANDBY," the local `FloatingQuarkWidget`) is gone. Chrome now comes from
  `:app-shell`'s `QuantumOSLayoutShell` + `NameplateHeader` via a thin local
  `ui/components/AppShell.kt` wrapper. No `BackHandler`; back is `onReturnHome = { finish() }` plus
  an explicit "◄ HOME" line. The old app's `EXPLORER/TERMINAL/DECRYPT/QUARK` screens are kept as
  FILES's own internal navigation (that's not the launcher's HOME/APPS/STATUS/LOG chrome, which is
  launcher-only — a docked module is free to have its own internal tabs).
- **The local `FloatingQuarkWidget` (with its always-on "breathing" pulse) is deleted outright** —
  the real system-wide `QuarkTriggerService` already floats over this screen like any other
  foreground app; no per-module trigger needed.
- **Zero-idle-redraw**: the whole-screen CRT flicker and the QUARK-trigger breathing pulse are both
  gone (the former closed automatically by adopting `:app-shell`'s treatment; the latter by
  deleting the widget entirely).
- **Taxonomy left exactly as-is — Director ruling, not an oversight.** The four seeded categories
  (FIELD-LOGS/CAPTURES/COMMS-CACHE/MAPS) carry over with their original seed content; the browser
  stays generic and unrestricted (raw path breadcrumb, free-form folder/file creation anywhere). No
  enforcement, folder-locking, or category restriction was added, on purpose.
- **Both Gemini-backed features stripped**, not just the one named in the brief. The standalone app
  had a "DECRYPT AI" feature AND a separate "talk to QUARK" chat feature, both calling a live
  Gemini API (cloud key, `INTERNET` permission). Since `:files` carries no network dependency at
  all, both were rewired onto the same shared `:core` placeholder (`AiAssistBridge`/
  `NotYetWiredAiAssistBridge`) that COMMS uses, rendering a clearly-styled "AI BRIDGE NOT YET
  WIRED" state — **this is a judgment call beyond the brief's literal text (which named only
  DECRYPT AI), flagged for Director confirmation.**
- **Typography/palette**: `FontFamily.Monospace` → `Fonts.ChakraPetch`; the previously-dead correct
  token file and the previously-live stock-Material-purple theme are both gone, replaced by
  `:app-shell`'s `Phosphor` tokens as the only theme source. `CircularProgressIndicator` (used for
  the old AI-decrypt loading state) replaced by `PleaseStandbyCard`. Rebranded `com.example` →
  `com.quantumos.files`.

## Still open (known, flagged gaps — not oversights)

- **No shared icon library exists yet.** Stock Material icons (including the generic Folder/
  Description pair for all file types) remain in use, per the fix-pass brief's own explicit
  fallback — not something to invent a one-off icon set to solve.
- **Docked-module phosphor hue doesn't sync with the launcher's live selection** — same
  pre-existing limitation as `:optics`/`:nav`/the other three new modules; not introduced here.
- **AI-assist bridge is a placeholder, not a real implementation** — see COMMS' fix report for the
  shared reasoning (QUARK's real on-device brain isn't a realistic rewire target this pass).
- Confirm with the Director whether stripping the "talk to QUARK" chat feature (not explicitly
  named in the brief) alongside DECRYPT AI was the right call.

## On-device confirmation

**Confirmed on the Fold 6 by the Director** — works fine, alongside COMMS/AUDIO/RADIO.
