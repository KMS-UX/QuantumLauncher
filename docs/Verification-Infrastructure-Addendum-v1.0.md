# QuantumOS — Verification & Infrastructure Addendum
*Companion to the Launcher Build Spec (v1.1) and Build Bible (v0.20→v0.21). Version 1.0.*

> **Purpose.** A pre-build "prep pass" run before opening the Launcher App build. It does three things: (1) **verifies** the time-sensitive Android claims the spec and the outside-specialist brief rely on, against the live platform (checked **June 2026**); (2) stands up the **build infrastructure** the project under-planned (version control, code-state handoff, rollback safety); and (3) records the **governance rule** for using outside AI help. Nothing here re-litigates a locked design decision — it hardens the ground under the build.

---

## 1. API verification — what held, what was corrected

Checked against current Android developer documentation (Android 17 era, June 2026).

| Claim under review | Verdict | Note for the build |
|---|---|---|
| Full-canvas / edge-to-edge via `setDecorFitsSystemWindows(window, false)` | ✅ Works, **modern call substituted** | Use `enableEdgeToEdge()`; the old call is the manual fallback. Edge-to-edge is **enforced** at targetSdk 35+, so we draw behind the system bars and **own inset handling** — the CRT-falloff container must account for status/nav/cutout insets itself. |
| Intercept the system **back** gesture inside our shell | ✅ Valid, with nuance | Predictive back is **on by default** (Android 16/17). Consuming back at the root is correct for a locked shell; it also suppresses the system "preview home" animation — which is what we want. Add `android:enableOnBackInvokedCallback="true"`; handle via `BackHandler`. |
| CRT effects as **AGSL shaders** (`RuntimeShader`), not CPU loops | ✅ Confirmed (API 33+) | Keep all phosphor/scanline/vignette/flicker math on the GPU. minSdk 33 unlocks this. |
| Compose recomposition discipline via `@Immutable` / `@Stable` | ✅ Confirmed, good practice | This is also how we honor "static at rest / zero idle redraw." |
| Tech stack = native **Kotlin + Jetpack Compose** (decision 55) | ✅ **Validated harder than at lock time** | Android 17's official stance is **"Compose-first"** — all new APIs are Compose-only, legacy Views in maintenance mode. Our bet is now the only forward-looking option. Current line ~Compose 1.11 / BOM 2026.04; minSdk 33, target latest. |
| "Drawing #000000 **hides** the camera punch-hole / notch" | ⚠️ **Overstated — corrected** | Black pixels power off on OLED (true, good for battery), so black *de-emphasizes* a cutout — it does **not** make the physical hole disappear. Design the fixed container to **live around** the cutout; treat it as "blend," not "mask." |

**Net:** the outside brief was mostly sound and its pure-logic module suggestions (`QuarkParser`, `MatrixBackground`, `QuantumState`) remain good "build-now, zero-hardware" work. Only the OLED-masking mental model needed correcting, and one API call modernized.

## 2. New platform capability worth banking (not for now)

**Android 17 introduced "AppFunctions" — an on-device equivalent of MCP.** An app can register its capabilities as callable "tools" that an on-device AI agent (including Gemini, or a future QUARK brain) can discover and execute on the user's behalf. This sits directly on the path of **QUARK-executes-commands (Trident Pillar ③)** and the **hospitality-API future (Tree 3)**. It is **alpha**, and the launcher build does not need it — but it is a real, new option that did not exist when those parts of the plan were written. **Forward note only; revisit at Chat 04 / Tree 3.**

## 3. Build infrastructure — stand up before M0

The project versioned its *design* meticulously and under-built the *code* side. Three fixes:

1. **Version control — Git + a private GitHub repo.** The Build Bible's equivalent for code: every change saved, reversible, backed up off the laptop, and the mechanism by which one Claude Code session resumes exactly where the last stopped. *Director action: a free GitHub account. Claude Code wires up the rest.*
2. **A "Build Log" companion doc.** The Bible tracks *design decisions*; the Build Log tracks *code state* — what's built, current milestone (M0–M7), known bugs, and the exact "resume here" pointer for the next session. Same end-of-session cadence we already run, applied to code.
3. **Rollback escape hatch — the literal first step of M1.** Before QuantumOS is ever set as the Fold 6's home screen, confirm the way back: **Settings → Apps → Default apps → Home app → switch to stock**. The Fold is also the daily comms phone; this escape hatch is non-negotiable and belongs in the spec ahead of everything.
   - *Related:* the app needs a one-time **signing key** to install (a small file generated once). If it's lost, clean updates break — Claude Code generates it; keep it backed up.

## 4. Governance rule for outside AI help (Gemini & others)

Outside models are valuable as a **second engineer and adversarial reviewer** — especially Gemini, which is current on Android and integrated into Android Studio. Recommended division of labor:

- **Clara (Claude)** — vision-translation, architecture, design system, persona, PM, and Bible coherence. The *what and why*.
- **Claude Code** — hands-on building on the real machine.
- **Gemini** — adversarial review ("what breaks on a real Pixel?"), platform-currency checks, a second pair of hands in the build.
- **The compiler + the device** — the final judges.

**The rule (learned from this very pass):** *anything an outside model produces is **verified before it is banked**.* The specialist brief was useful and mostly right, but it should not have entered Project knowledge as truth without this check. Outside output is a strong **draft and stress-test**, not gospel. With that discipline, more inputs only make the project stronger.

---

*End of Verification & Infrastructure Addendum v1.0. Pairs with Launcher Build Spec v1.1; folded into Build Bible v0.21 (decisions 61–63).*
