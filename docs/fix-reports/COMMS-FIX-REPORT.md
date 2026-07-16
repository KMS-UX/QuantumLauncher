# COMMS — Core Apps Fix-Pass Report

*Docked into `:comms` (`com.quantumos.comms`), per Core Apps Fix-Pass Task Brief v1.0 (Decision 86).
Closes the findings in `QuantumComms/docs/AUDIT-REPORT.md`. CI-green; confirmed on the Fold 6.*

## What changed

- **Docked into the shared App Shell.** All private chrome (the plain-Material `Scaffold`, hand-drawn
  header, hardcoded scanline/vignette, `Crossfade` tab switch) is gone, replaced by
  `:app-shell`'s `QuantumOSLayoutShell` + `NameplateHeader` via a thin local
  `ui/components/AppShell.kt` wrapper — the same pattern `:optics`/`:nav` established. No
  `BackHandler`; back is `onReturnHome = { finish() }` plus an explicit "◄ HOME" line.
- **Live-pulse dot actually pulses now.** Previously a static, motionless circle with a comment
  falsely claiming it pulsed (`CommsTerminal.kt:888-893` in the standalone repo). Rebuilt as a
  one-shot, event-triggered decay animation (`ui/components/PulseDot.kt`) bound to a real
  new-transmission event — never `infiniteRepeatable`, fully static at rest.
- **Identity redesign — the channel list and message thread no longer read as a themed Discord/
  WhatsApp clone.** Replaced with a single-column transmission-log visual language: each entry is
  a callsign header (bright phosphor, bold) + timestamp + body, no rounded chat bubbles, no
  left/right sender-alignment split — reads as a field radio log, not a messenger. Grounded in the
  `quantumos-house-style` Skill and the Build Bible; **the linked design-system page returned 403
  Forbidden from this sandbox on every attempt (both during the audit and this pass) — worth a
  Director sanity-check against the actual page**, since this redesign wasn't checked against it
  directly.
- **Zero-idle-redraw**: the whole-screen CRT flicker is gone (closed automatically by adopting
  `:app-shell`'s shader-based treatment). The infinite `startUptimeCounter()`/
  `startTelemetrySimulator()` random-drift loops are deleted — no fabricated telemetry.
- **Gemini AI chat persona stripped.** The standalone app called a live Gemini API directly (cloud
  key, `INTERNET` permission) for an in-app chat-reply persona. Removed entirely — `:comms` carries
  no network dependency. Any AI-assist affordance left in the UI calls a new shared `:core`
  placeholder (`AiAssistBridge`/`NotYetWiredAiAssistBridge`), rendering a clearly-styled "AI BRIDGE
  NOT YET WIRED" state rather than a crash or silent no-op.
- **Cipher-decryption terminal preserved as-is** — the audit's one genuinely on-identity, positive
  finding; re-skinned onto `:app-shell` tokens/fonts but otherwise unchanged.
- **Typography/palette**: `FontFamily.Monospace` → `Fonts.ChakraPetch` throughout; all hardcoded
  hex → `Phosphor.bright/dim/Warn/Crt` (hue defaults to GREEN locally, matching Optics/Nav — see
  "Still open" below). Rebranded `com.example` → `com.quantumos.comms`.

## Still open (known, flagged gaps — not oversights)

- **No shared icon library exists yet.** Stock Material icons remain in use, per the fix-pass
  brief's own explicit fallback (don't invent four one-off icon sets). A future short design pass
  should produce the original line-icon set the House Style Skill calls for.
- **Docked-module phosphor hue doesn't sync with the launcher's live selection** — `:comms`
  defaults to GREEN locally on launch, same pre-existing limitation as `:optics`/`:nav`. Not
  introduced by this pass; worth a follow-up across all docked modules together.
- **AI-assist bridge is a placeholder, not a real implementation.** Wiring it to QUARK's real
  on-device brain requires that brain being promoted out of debug-gating and extracted from `:app`
  into a module docked apps can depend on — logged as a to-do, not attempted here.
- The redesigned transmission-log visual language was built against the House Style Skill, not the
  linked Figma-style design page (inaccessible from this sandbox) — flagging for a Director look.

## On-device confirmation

**Confirmed on the Fold 6 by the Director** — works fine, alongside FILES/AUDIO/RADIO.
