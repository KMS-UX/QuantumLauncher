# BUILD_LOG.md — QuantumOS code state

The code-side companion to the Build Bible. The Bible tracks *design decisions*; this tracks
*code state*, so each Claude Code session resumes exactly where the last stopped. Update the top
block at the end of every session.

---

## ▶ RESUME HERE
**Current milestone:** M0 — design-system foundation / first green build.
**Goal of this session:** get the project to compile green — phosphor CRT screen, Chakra Petch
font, live `[GREEN] [AMBER] [CYAN]` hue switch, and the four logic unit tests passing.
**Next after green:** confirm M0 on the Fold 6, then resolve the fixed-container-vs-fill question
(see Decisions pending), then start M1.

## Status
- [ ] First green `gradle assembleDebug`
- [ ] `gradle test` — 4/4 passing
- [ ] M0 confirmed on Fold 6 (phosphor screen + hue switch live)
- [ ] Chakra Petch actually bundled (currently a Monospace placeholder — see TODO in LauncherUi.kt)

## Known issues / TODOs
- Typography: replace `FontFamily.Monospace` with Chakra Petch (`res/font/`), per CLAUDE.md.
- CRT: current overlay is the cheap non-shader fallback; real AGSL shader is an M6 polish item.
- `runDevSimulation()` in the ViewModel is a dev-only harness — remove before M7.

## Decisions pending (Director / Clara — do not lock in code)
- **Fixed container vs fill-and-adapt** (the 9:19.5 letterbox). Default is fill-and-adapt; Director
  to judge on-device on the Fold before we lock it.

## Session history
- *(empty — first build session)*
