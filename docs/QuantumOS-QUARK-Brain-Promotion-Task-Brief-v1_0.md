# QuantumOS — QUARK Brain Promotion Task Brief v1.0
*For: Claude Code, on the ROG Strix build machine. Authorized by Build Bible v0.38's work-bank entry ("Promote QUARK's on-device brain out of debug-gating"). File to `docs/` once accepted.*
*Mission: wire the real, already-confirmed QUARK brain into the `AiAssistBridge` hook the Core Apps Fix-Pass left for it (decision 88), and retire the debug toggle. Make her a real production feature, not a hidden switch.*

## 0. Read this first — what this is and is not

This is **wiring, not invention.** Every component being connected here already exists and is already confirmed on the Fold 6:
- The model + runtime: Gemma 3n E2B-IT via Google AI Edge/LiteRT, loaded with Persona Pack Part B verbatim as system prompt (decision 73a).
- The voice: Piper TTS pipeline → Kokoro-class synthetic-blend voice, respecting Stealth's mute (decision 73b).
- The hook: `AiAssistBridge` / `NotYetWiredAiAssistBridge` in `:core` (decision 88) — a placeholder interface the Gemini-backend strip left specifically for this.
- The UI it feeds: the QUARK Assistant View (full-screen presence, conversation log, command rail, text entry) and the floating trigger, both already built and present across HOME and all six docked apps.

**Not in scope:** Phase 3 (command execution) — still genuinely Kiosk-gated, untouched by this brief. No new intents, no new UI screens, no changes to SIGNAL or CONFIG (separate thread).

Two project laws apply as always:
- **Zero idle redraw** — inference latency must not introduce any new polling or idle animation beyond the already-designed Scan state.
- **Verify before banking** — this is internal wiring, not externally-generated code, so the audit-pass precedent doesn't apply here, but the acceptance criteria below still require on-hardware confirmation before this is called done.

## 1. Scope at a glance

| # | Deliverable | Acceptance |
|---|---|---|
| 1 | Real `AiAssistBridge` implementation in `:core` | Replaces `NotYetWiredAiAssistBridge`; interface shape unchanged (decision 88) |
| 2 | Debug toggle removed as the gate | Brain is the default production path, no flag required to reach it |
| 3 | Kill-switch retained | The old debug toggle stays wired as a hidden fallback — see §4, this is the rollback path |
| 4 | Voice pipeline connected | Piper → Kokoro-blend voice fires on real replies, Stealth mute honored (decision 38) |
| 5 | First-run model download UX | A minimal, in-house-style consent/progress step — see §3 |
| 6 | Reactive states wired to real inference | Scan holds for actual latency, not scripted timing; settles to result state on completion |
| 7 | Scripted-Line Library demoted to fallback | Stays in the codebase as an offline/error fallback only (decision 58's own intent: scripted lines retire when the real brain arrives) |
| 8 | Confirmed on the Fold 6, in a production (non-debug) build | See §5 acceptance |

## 2. Where this plugs in

- **HOME + all six docked apps** (CAM/MAPS/COMMS/FILES/AUDIO/RADIO) already carry the floating QUARK trigger and the shared Assistant View from the App Shell. No per-app changes needed — wiring `AiAssistBridge` once in `:core` should light all of them up simultaneously, same "fix once in the shared module" pattern as the Core Apps Fix-Pass.
- **SIGNAL/CONFIG are still placeholders** — nothing to wire there yet; they'll inherit this for free once built, same reasoning.

## 3. First-run model download — new surface, needs a stance

The debug toggle never had to handle this gracefully because it was a developer convenience. In production, a real Operator's first tap on QUARK may trigger a model-weight download (decision 73a: weights download on first run, not bundled in the APK).

**Default scoped for this brief** (Director may overrule): a single stepped, in-house-style screen —
- "ACQUIRING QUARK — [progress]" in the boot-log/stepped visual language already established (Ignition Lab precedent), not a generic Android progress bar.
- Fires once, on first invocation of the Assistant View or floating trigger post-install.
- If the download fails or is offline: fall back to the Scripted-Line Library (§1.7) with an honest in-character line acknowledging the limited state, rather than a dead UI.

This is small enough to build inline in this brief rather than spinning up a separate Lab — flag if the Director wants it prototyped first instead.

## 4. Rollback path

The existing debug toggle is **not deleted** — it becomes a hidden emergency kill-switch back to the Scripted-Line Library, reachable the same way debug toggles have been throughout the project. If the real brain misbehaves on hardware post-ship, this is the instant, zero-risk fallback. Consistent with the project's rollback-first discipline (decision 61a and every milestone since).

## 5. Acceptance criteria (must all hold on the Fold 6, production build)

1. Tapping the floating QUARK trigger from HOME and from at least two docked apps produces real LLM replies (not scripted lines) with no debug flag set.
2. Persona holds under the same adversarial jailbreak check that validated Part B in decision 73a — reconfirm end-to-end through the real bridge, not just the isolated debug harness.
3. Voice replies fire correctly and respect Stealth's mute.
4. First-run download flow behaves per §3, and the offline fallback is honest, not broken.
5. No new idle-cost: profile confirms zero unconditional render/poll loops introduced.
6. CI green; the old debug-toggle path still functions as a manual fallback.
7. Latency stays under the confirmed under-5s average under real app-shell conditions (not just the isolated debug harness — this is worth reconfirming, since decision 73's numbers predate docking into the shared shell).

## 6. Process note

Standard handoff: this brief goes to `docs/` in the launcher repo. Report back per-criterion in §5, plus anything unexpected (same pattern as every prior M-series and fix-pass brief) — especially if the first-run download step needs its own Lab pass rather than the inline default in §3.
