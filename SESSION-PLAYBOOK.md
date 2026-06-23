# SESSION-PLAYBOOK.md — Claude Code pre-work / review pass

The repeatable **ramp-up every milestone session runs before writing code**, plus the close-out
after. This is *execution methodology* (Claude Code's side of the house), not design canon — the
design canon lives in `docs/` and `CLAUDE.md` and is **read-only** to us. Distilled from M1–M4,
which all shipped clean by following this shape.

> **Why this exists:** every session starts cold and re-derives context. A fixed pre-work pass means
> the first stretch is *orienting against the live repo*, not guessing — and that's exactly what kept
> M1–M4 on-spec and first-try-green on CI.

---

## Stage 0 — Orient (always, in this order)
1. **`CLAUDE.md`** (auto-loaded) — roles, design non-negotiables, platform rules, **hard guardrails**, build/run.
2. **`BUILD_LOG.md` ▶ RESUME HERE** — the single source of *where the last session stopped, the goal of
   this one, and pending hardware sign-offs.* Confirm the milestone before anything else.
3. **The milestone Task Brief** (`docs/QuantumOS-M<n>-Task-Brief-*.md`) — read end-to-end **before
   touching code.** Note four things explicitly: the **Goal**, the **scope boundary**, every per-step
   **`Verify:`** line, and the **Hard stops**.
4. **Invoke the `quantumos-house-style` skill** — mandatory before building, styling, or writing *any*
   QuantumOS surface. Don't default to Material. (From M5 also re-read the QUARK Persona Pack +
   Scripted-Line Library — her lines come from the library, never invented inline.)

## Stage 1 — Map the ground (recon before design)
- `git log --oneline` — recent milestones + merges; what's actually on `main`.
- Layout: `com.quantumos.core` (pure logic, no UI deps, unit-tested) vs `com.quantumos.shell.ui` (Compose).
- Read the files you'll touch **and their neighbours**, then list the **reuse seams** before writing anything new:
  - **`Phosphor`** token source — never hardcode a hue per-screen.
  - **`QuantumStateEngine`** seams (`masterState`, `dispatch*`, `incomingTelemetryUpdate`, …) — logic lives here, not in the UI.
  - **Shared composables** already built — e.g. `PleaseStandbyCard`, `SegmentedGauge`, the App Shell chrome. Reuse, don't rebuild.
  - **`ScriptedLineLibrary`** — the only source of QUARK voice (critical from M5).
- **Know the build reality:** the cloud session has **no Android SDK** → CI (`.github/workflows/build.yml`)
  is the real compiler on push. Pure-logic can be sanity-checked in a JVM harness; everything else is proven by CI + the on-device pass.

## Stage 2 — Plan to spec
- Restate the **scope boundary** and **hard stops** in your own words; write down what is explicitly *out of scope* (e.g. M4: no real Assistant View).
- Split the work into **LOGIC vs UI**:
  - Logic → `com.quantumos.core`, all-`val` data classes/enums (Compose-stable), **unit-tested**, single source of truth.
  - UI → Compose, dressed inside the App Shell, house-style motion/voice.
- Pick the **testable seam** and write/extend its unit tests (e.g. M4's `OverlayGeometry` — edge-snap + park math out of the View and into core).
- For any **aesthetic/UX call**, implement the recommended default and **flag it for the Director** — never lock it silently.

## Stage 3 — Implement
- Match the surrounding code's idiom and comment density.
- House style is non-negotiable: **stepped motion, static at rest, phosphor-only, CRT falloff, terse status microcopy, "Operator" voice, GPU shaders (with a cheap fallback).** No Material chrome, no off-palette colour, no idle redraw.
- One source of truth: reuse seams; never fork a second state path or rebuild an existing component.

## Stage 4 — Verify
- Map **every brief `Verify:` line** to a concrete check.
- `gradle test` / `assembleDebug` locally if an SDK is present; otherwise **push and watch CI to green** before claiming done.
- The **headline check is on-device** (the Director's pass) — call it out specifically (e.g. M4: "does it hover over a *real other app*").

## Stage 5 — Close
- Update **`BUILD_LOG.md`**: items done/not-done, **judgment calls + forward concerns**, and the
  **`resume here → M<n+1>`** line.
- Commit to the **designated branch**; open a PR; **merge only when asked.**

---

## 60-second pre-flight checklist
- [ ] `RESUME HERE` read — correct milestone, hardware sign-offs noted
- [ ] Task brief read — **Goal · scope boundary · every `Verify:` · Hard stops**
- [ ] `quantumos-house-style` skill invoked (+ Scripted-Line Library from M5)
- [ ] Reuse seams identified — no hardcoded hue, no rebuilt component, no second state path
- [ ] LOGIC-vs-UI split decided; **testable core seam** chosen
- [ ] Scope restated; out-of-scope + hard stops written down
- [ ] Build reality clear: pure logic = JVM harness; everything else = CI + on-device

## Anti-patterns this prevents (seen-and-avoided in M1–M4)
- Working ahead of the milestone (building M5 content during M4).
- Re-implementing a beat/gauge/chrome that already exists instead of extracting a shared one.
- Putting testable logic inside a Composable/View where it can't be unit-tested.
- Hardcoding a phosphor hue or reaching for a Material default / generic spinner.
- Inventing QUARK dialogue inline instead of routing through the Scripted-Line Library.
- Declaring "done" off a local edit when only CI + the Fold 6 actually prove it.
