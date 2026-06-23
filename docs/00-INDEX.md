# docs/ — QuantumOS deep context for Claude Code

`CLAUDE.md` at the repo root is the quick reference you read every session. **This folder is the
deep source material** behind it. Read in this priority order, and when anything here conflicts with
`CLAUDE.md` or the Build Bible, the **Build Bible wins** (it's the source of truth) — flag the conflict
rather than guessing.

## Reading order
1. **`/CLAUDE.md`** (repo root) — roles, current milestone, design language, hard guardrails. Start here.
2. **`Launcher-Build-Spec-v1.1.md`** — the milestone task list (M0→M7): what to build, in what order, and where the real platform walls are. Your execution map.
3. **House Style skill** (`.claude/skills/quantumos-house-style/`) — the authoritative design language: phosphor palette + tokens, CRT falloff, typography, stepped/static motion, the App Shell, icon and voice rules. Consult before building *any* surface.
4. **`Build-Bible-v0.22.md`** — the master "what & why" + the full numbered decision log. The reasoning behind every locked choice.
5. **`QUARK-Persona-Pack-v1.0.md`** + **`QUARK-Scripted-Line-Library-v1.1.md`** — QUARK's character and her dialogue. Needed from M5. **Her lines come from the Scripted-Line Library — never invent them inline.**
6. **`Verification-Infrastructure-Addendum-v1.0.md`** — the verified (June 2026) platform/API constraints (`enableEdgeToEdge`, AGSL-on-GPU, ViewModel for config survival, back-handling) and the governance rules.

## Rules for these docs
- **Don't edit them.** They're versioned by the Director and Clara. If something needs to change, note it in `BUILD_LOG.md` under "Decisions pending" and surface it — don't silently diverge.
- They're a **point-in-time snapshot.** If a doc looks stale against the live repo, flag it.
- **Verify-before-banking:** code you write gets reviewed against the House Style skill and the spec before it's locked and the Bible is bumped. Surface diffs; don't assume.

## Quick map of where things live in code
- `com.quantumos.core` — pure logic (state engine, parser, scripted-line seam). No UI deps. Unit-tested.
- `com.quantumos.shell.ui` — Compose UI (activity, ViewModel, the shell, surfaces).
