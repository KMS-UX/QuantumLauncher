# Core Apps Fix-Pass — Cover Summary

*Companion to the four per-app fix reports in `docs/fix-reports/`. Docks COMMS, FILES, AUDIO, RADIO
into the shared `app-shell` module per Core Apps Fix-Pass Task Brief v1.0 (Decision 86), closing the
findings from the earlier Core Apps Audit Pass. **CI-green, and confirmed on the Fold 6 by the
Director — all four apps work fine.***

## Deliverable #1 — merge-vs-separate check against Optics/Nav Phase 3

**Decision: built on top of Phase 3's branch, not separate, not deferred.** `:core`/`:app-shell`
(and `:optics`/`:nav`) only existed on the still-open, unmerged PR #21
(`claude/dock-optics-nav-appshell-qvvbdz`) — `main` had no shared module at all. This fix-pass's
working branch merged that branch in first, then added the four new modules on top. This is flagged
explicitly, not decided silently: the eventual merged history will contain both Optics/Nav's docking
and this pass's four-app docking together, unless the Director wants PR #21 merged to `main` on its
own first.

## Shared-floor fixes — landed once, in `:app-shell`/`:core`, not four times

Confirmed closed for all four new modules by construction, since they all inherit from the same
shared module rather than each carrying its own copy:
- Toolchain: AGP 8.7.2 / Kotlin 2.2.21 / Compose BOM 2024.10.01 / minSdk 33 (the monorepo's real
  live pin — Kotlin was already at 2.2.21 before this pass, not 2.0.21 as the brief's text says;
  flagged as a stale-CLAUDE.md discrepancy, not a wrong decision made here).
- AGSL CRT shader (scanlines/vignette/flicker) — GPU shader, not a CPU draw-loop, with the
  automatic non-shader fallback — sourced once from `:app-shell`.
- Chakra Petch — bundled once in `:app-shell`, used by all four instead of system Monospace.
- Back-gesture handling — `enableOnBackInvokedCallback` + the Shell-owns-back convention, applied
  once as a pattern (no `BackHandler` in any docked Activity).
- Dead stock-Material theme scaffold (`Color.kt`/`Theme.kt`/`colors.xml`) — not ported into any of
  the four docked modules; `:app-shell`'s `Phosphor`/`Fonts` are the only theme source now.
- Icon policy is the one shared-floor item **not** closed: no line-icon library exists yet anywhere
  in this repo (Optics/Nav draw geometry directly, no SVG asset set). Per the brief's own fallback,
  stock Material icons stay in all four new modules — a known, explicitly-flagged gap for a future
  short design pass, not something worked around with four one-off icon sets.

## Zero-idle-redraw — every confirmed violation closed

- COMMS: whole-screen flicker (closed via `:app-shell` adoption) + `while(true)` telemetry/uptime
  simulator loops (deleted, replaced with no fabricated data).
- AUDIO: the fake idle-drift waveform (deleted outright — the fix-pass's #1 required fix, and the
  session's clearest confirmed instance of the audit's predicted top risk), shell-wide flicker
  (closed via adoption), 7 ungated QuarkMascot animations (now gated per-posture).
- FILES: whole-screen flicker (closed via adoption) + the "breathing" QUARK-trigger pulse (closed
  by deleting the local trigger placeholder entirely, in favor of the real system-wide one).
- RADIO: shell-wide flicker (closed via adoption) + the 10fps static-noise canvas loop (bounded to
  an 8-tick settle burst gated on real tune events).

## Per-app identity rulings

- **RADIO**: cryptographic decoder removed entirely (Director ruling) — RADIO stays a pure
  receiver. Preserved losslessly in the untouched standalone repo; pointer doc at
  `docs/future-signal/radio-decoder.md`.
- **FILES**: taxonomy stays exactly as-is (Director ruling) — four seeded folders, generic
  unrestricted browser, no enforcement added. Stated explicitly in the fix report so it reads as a
  deliberate call, not an oversight.
- **COMMS**: the one real redesign in this pass. Implemented now (not deferred to a separate design
  lab) per direction — the channel list and message thread are rebuilt as a single-column
  transmission log instead of a themed Discord/WhatsApp clone, and the live-pulse dot is bound to a
  real event instead of being a static circle. Grounded in the House Style Skill; the linked
  design-system page returned 403 from this sandbox on every attempt, so this redesign wasn't
  checked against it directly — worth a Director look.

## Scope call beyond the brief's literal text — Gemini backends

COMMS' in-app AI chat persona and FILES' "DECRYPT AI" (plus a second Gemini-backed "talk to QUARK"
feature in FILES the brief didn't separately name) all called a live Gemini API directly. Rewiring
either to QUARK's real on-device brain wasn't realistic this pass — it requires a manually
side-loaded ~2.6GB model, lives in `:app` itself (unreachable from a docked module without circular
dependencies), and is documented as debug-gated only, never production. Both were stripped and
replaced with a shared `:core` placeholder (`AiAssistBridge`/`NotYetWiredAiAssistBridge`) that
renders a clear "not yet wired" state rather than crashing or silently no-op-ing — logged as a
to-do for whenever that brain gets promoted out of debug-gating into a shared module.

## CI round-trips to green

Three, same discipline as Phase 3's own three: (1) `audio/AudioEngine.kt`'s doc comment
accidentally contained a literal `*/` inside a sentence, prematurely closing the block comment and
turning the rest of it into ~40 syntax errors — reworded; (2) two files imported
`androidx.compose.foundation.layout.weight` directly (same class of mistake as Phase 3's own `align`
import bug — it's a scope member, not a top-level function) + three modules were missing
`material-icons-core`/`-extended` — fixed; (3) one missing `height` import. All caught via CI job
logs and fixed directly, matching the reference PR's own fix-push-check loop.

## Status

Definition of done, per the brief's §5: four apps building clean on the pinned toolchain (✅), all
wired to the shared `app-shell` module (✅), §1 acceptance table green (✅), fix reports filed (✅),
**and — beyond what this pass could self-certify — on-device Fold 6 confirmation, which the
Director has now completed: all four apps work fine.**
