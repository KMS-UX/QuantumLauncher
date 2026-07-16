# Future SIGNAL candidate: cryptographic signal decoder (removed from RADIO)

Removed from the docked `:radio` module per the Core Apps Fix-Pass (Decision 86) — RADIO is a
pure content-receiver (FM/AM/WX bands, presets, dial, reception meter); a "CRYPTOGRAPHIC MODULE"
that intercepts and AI-decodes signals is measurement/analysis work that belongs to SIGNAL
(link diagnostics), not RADIO (content listening) — the RADIO-listens / SIGNAL-measures split.

The original implementation is fully preserved, untouched, in the standalone rollback repo
`kms-ux/quantumradioreceiver` (never pushed to since the audit) at:

- `app/src/main/java/com/example/MainActivity.kt`:
  - `InterceptControlPanel` — lines 870-927 (the "CRYPTOGRAPHIC MODULE [WX-9]" card; its only job
    was a button that called `viewModel.setDecoderPaneOpen(true)`)
  - `CryptographicDecoderConsole` — lines 1148-1323 (the "QUARK CRYPTOGRAPHIC SIGNAL DECODER"
    full-screen overlay: custom-input field, "ACTIVATE DECRYPTOR" button, stepped boot-log terminal,
    decoded-result readout)
- `app/src/main/java/com/example/radio/RadioViewModel.kt`:
  - `startSignalDecoding()` — lines 171-203 (the coroutine that streams the stepped boot logs, then
    calls `GeminiClient.decodeSignal(...)`)
  - Supporting state also removed from the ported ViewModel: `isDecoding`, `decodeLog`,
    `decodedResult`, `customInput`, `isDecoderPaneOpen`, `setCustomInput()`, `setDecoderPaneOpen()`
- `app/src/main/java/com/example/radio/GeminiClient.kt` — the full file (125 lines). The live Gemini
  API client (`gemini-3.1-pro-preview`, `generateContent`, HIGH thinking config via OkHttp). This is
  also why the docked `:radio` module carries no `INTERNET` permission and no network dependency —
  deleting this file removed RADIO's only reason to need either.

Nothing here is lost: the standalone repo is a read-only reference that was never pushed to after
the audit, so this is a rollback copy, not a deletion of the only copy.

If a future SIGNAL module wants this capability, port from those exact locations rather than
rebuilding from scratch — it was a working feature, just misplaced. Two things to fix on the way
over, noted during this pass:
- The old `AnalogReceptionMeter` label read "DECRYPT STABILITY," implying RADIO itself decoded
  signals; SIGNAL's version should own that framing honestly (it measures/diagnoses, RADIO never
  did the decoding even before this removal — the label was aspirational, not accurate).
- `GeminiClient.decodeSignal()` reads `BuildConfig.GEMINI_API_KEY` with no key-rotation/secrets
  story documented anywhere in the standalone repo; whoever wires this into SIGNAL should confirm
  where that key is meant to live before shipping it.
