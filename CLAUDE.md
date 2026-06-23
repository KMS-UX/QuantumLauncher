# QuantumOS — Claude Code Quick Reference

Deep source material lives in `docs/`. When anything here conflicts with the Build Bible, **the Build Bible wins** — flag it, don't guess.

---

## Current milestone: Pre-M0 Cloud Spike → M0

**Where we are:** The project skeleton compiles to a debug APK (Pre-M0 Cloud Spike ✅ in progress).
**Next step:** Complete M0 design-system foundation so the app launches to a phosphor CRT screen with working font + hue switch. Then M1 (HOME intent, APPS grid).

Full task sequence: M0 → M1 · Launcher core → M2 · STATUS/LOG → M3 · Vitality panel → M4 · Floating QUARK trigger → M5 · QUARK Assistant View → M6 · Splash/sound/polish → M7 · Ship/field-test → Checkpoint β.

---

## Hard guardrails (never violate without director sign-off)

- **Aesthetic north star:** used-future / atomic-age industrial / lived-in field tool. Every asset original (Fallout/Blade Runner = mood lineage only, zero direct copying).
- **Address the user as "Operator."** QUARK is a peer-grade aide, not servile. Precise, EDI register — honesty in the Operator's interest, never flattery.
- **QUARK's spoken lines come from the Scripted-Line Library** (`docs/QUARK-Scripted-Line-Library-v1.1.md`) — never invented inline.
- **"Verify before banking":** code produced without a compiler running (e.g. by any AI) is treated as a draft, not truth. CI + device are the final judges.
- **Don't re-litigate locked decisions.** If you think a decision is wrong, flag it rather than silently diverging.
- **Docs in `docs/` are read-only.** They are versioned by the Director/Clara. Surface conflicts in `BUILD_LOG.md` under "Decisions pending."

---

## Design language (quick ref — full detail in `.claude/skills/quantumos-house-style/`)

**Color:** phosphor CRT. One switchable hue recolors everything. Green default (`#00FF00` / `#00AA00`), Amber (`#FFB000` / `#A86F00`), Cyan (`#00E5FF` / `#0090A8`). Warn = `#FF3B1F` (alerts / access-denied ONLY — never decorative). CRT ground = `#020402`. No off-palette colors.

**Screen treatment:** content fades to black at the edges (CRT falloff). Layered: scanlines + edge vignette + subtle flicker + phosphor glow. GPU effects (AGSL shaders, API 33+) on hardware; non-shader fallback for cloud/emulator builds.

**Typography:** Chakra Petch (Monofonto substitute, bundled). Monoton = display accent only (boot wordmark stamp — one blessed ceremonial use). Never Monoton as a system text face.

**Motion:** stepped + discrete (slide-projector clicks, not smooth glides). Static at rest — zero idle redraw. PLEASE STANDBY = universal loading card; never a generic spinner. Functional motion only.

**Sound:** mechanical, synthesized. Boot = power-up sweep · Denied = harsh buzz · Granted = two-note + sub · Keypad = relay tick. QUARK's non-verbal chirps are wordless and distinct from her voice.

---

## Codebase map

| Package | Role |
|---|---|
| `com.quantumos.core` | Pure Kotlin logic — state engine, parser, scripted-line seam. **No UI/Android deps.** Unit-tested without emulator. |
| `com.quantumos.shell.ui` | Compose UI — activity, ViewModel, shell, surfaces. |

**Key files:**
- `app/src/main/java/com/quantumos/core/QuantumState.kt` — engine, parser, all enums/state
- `app/src/main/java/com/quantumos/shell/ui/LauncherUi.kt` — activity, Phosphor tokens, CRT overlay, terminal surface
- `app/src/test/java/com/quantumos/core/QuantumStateEngineTest.kt` — pure-logic unit tests (no emulator)

---

## Platform notes (verified June 2026, see Verification Addendum)

- Edge-to-edge is **enforced** at targetSdk 35+. Use `enableEdgeToEdge()` (done). The CRT container owns inset handling.
- Back gesture: add `android:enableOnBackInvokedCallback="true"` in manifest (done) + `BackHandler` in Compose (done).
- AGSL shaders (`RuntimeShader`) confirmed at API 33+. Non-shader CRT stub is correct for cloud/emulator builds.
- Cutout / punch-hole: black *de-emphasizes* on OLED but the hole is still physically there. Design the container to live around it — "blend," not "mask."
- Android 17 "AppFunctions" (on-device MCP equivalent): alpha, not for now. Revisit at Chat 04 / Tree 3.

---

## Scope guard — NOT in this build

Lock screen/keypad, true notification shade, real Android boot animation, deep stock-surface theming. All return free in kiosk (Tree 1.75). Real `lockNow()` arrives in device-owner mode. QUARK's LLM brain = Chat 04.
