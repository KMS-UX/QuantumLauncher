# CLAUDE.md — QuantumOS build context

**Read this before writing any code.** QuantumOS is a retro-futuristic "used-future" Android
field multi-tool (launcher + companion apps) with a phosphor-CRT aesthetic. This file keeps the
build on-spec; default Android/Material styling is **wrong** here.

**Start every milestone session with the pre-work pass in [`SESSION-PLAYBOOK.md`](SESSION-PLAYBOOK.md)** —
the orient → recon → plan → verify → close ramp (distilled from M1–M4) that gets you oriented against
the live repo before touching code.

## Roles
- **Director** (repo owner, non-coder) — sets direction, approves, judges the look on-device.
- **Clara** (planning Claude) — owns architecture, the design system, specs, and the Build Bible.
- **You (Claude Code)** — implement to spec on the real repo; compile, read errors, fix, iterate.
- **The compiler + the Fold 6** — the final judges of correctness and of the look.

When a choice is *aesthetic or UX* (e.g. the fixed-container vs fill question), do **not** lock it
silently — implement the recommended default, flag it, and let the Director judge it on-device.

## Where we are
Critical path: **Tree 1.5 — Launcher App**, building toward **Checkpoint β**. Work the milestones
in order; each is one session and ends with an on-device check on the Fold 6 + a `BUILD_LOG.md`
update. Current target: **M0 — first green build** (phosphor screen, font, live hue switch, logic
tests pass).

- **M0** Design-system foundation → phosphor CRT screen, font + hue switch live.
- **M1** Launcher core → HOME intent, Home channel, set as default launcher, APPS grid. *(See rollback rule below.)*
- **M2** STATUS + LOG channels (real battery/uptime/storage + event log).
- **M3** Vitality panel (atom roll-down; vitals; Phosphor/Stealth/Beacon real, Lock cosmetic).
- **M4** Floating QUARK trigger (overlay, static + draggable, PLEASE STANDBY beat).
- **M5** QUARK Assistant View (4 reactive states, log, command rail, scripted brain).
- **M6** Splash + sound + CRT-shader/motion polish.
- **M7** Signed APK → sideload to Fold 6 → Checkpoint β.

## Design language — non-negotiable (full detail in the House Style skill / docs)
- **Color:** phosphor only. Active hue is GREEN `#00FF00`/dim `#00AA00` (default), AMBER
  `#FFB000`/`#A86F00`, CYAN `#00E5FF`/`#0090A8`. `--warn` `#FF3B1F` = alerts/access-denied ONLY.
  `--crt` `#020402` = screen ground. **One token source; never hardcode hues per-screen; no off-palette colors.**
- **Screen:** CRT falloff — content fades to black at the edges; scanlines + vignette + glow.
  Effects are **AGSL/GPU shaders, never CPU draw-loops.** Keep a cheap non-shader fallback so
  layout renders on software emulators.
- **Type:** Chakra Petch (substitute for Monofonto). Bundle it; don't ship system Monospace.
- **Motion:** stepped, not interpolated. **Static at rest** (zero idle redraw) — life only through
  functional reactive states. Loading = a **PLEASE STANDBY** card, never a generic spinner.
- **Chrome:** the App Shell (opaque nameplate header, registration marks, strip→content→action-rail,
  persistent floating QUARK trigger). No Material-default app bars/FABs/dialogs.
- **Voice:** address the user as **Operator**. QUARK's lines come from the **Scripted-Line Library**
  — do not invent her dialogue inline.

## Platform rules (verified June 2026)
- Native Kotlin + Jetpack Compose. minSdk 33. Logic lives in `com.quantumos.core` (no UI deps, unit-tested).
- Edge-to-edge via `enableEdgeToEdge()` (not `setDecorFitsSystemWindows`); we own inset handling.
- State that must survive fold/unfold/rotate lives in a **ViewModel** (not in composition).
- Consume the back gesture inside the shell; manifest `android:enableOnBackInvokedCallback="true"`.
- State data classes = all-`val` primitives/enums (Compose infers them stable).

## Hard guardrails
- **Do NOT add `<category android:name="android.intent.category.HOME">` until M1.** For now this is a
  normal tappable app, so it can't take over the Fold's home screen by accident.
- **M1 rollback rule:** before setting QuantumOS as the *default* launcher, confirm the way back —
  *Settings → Apps → Default apps → Home → stock launcher* — and put that in the verify step. The Fold
  is also the daily phone; never leave it without a working home screen.
- **One objective per session.** Don't run ahead into later milestones.
- End every session by updating `BUILD_LOG.md`: what's done, known issues, and the exact "resume here".
- Back-and-brand review: surface anything that diverges from this file rather than guessing.

## Build / run
```
gradle test            # logic tests, no emulator
gradle assembleDebug   # -> app/build/outputs/apk/debug/app-debug.apk
```
Toolchain is pinned for reliability (AGP 8.7.2 / Gradle 8.9 / Kotlin 2.2.21 / Compose BOM 2024.10.01).
If you upgrade it, do so deliberately and note it in `BUILD_LOG.md`.
