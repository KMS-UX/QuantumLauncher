# QuantumOS — Design Tokens & Rendering Reference

The exact values behind the SKILL.md summary. Read this when you need precise hex codes, the full sound palette, or how the look maps to real Android.

## Color — full phosphor palette

| Token | Green (default) | Amber | Cyan |
|---|---|---|---|
| `--phosphor` | `#00FF00` | `#FFB000` | `#00E5FF` |
| `--phosphor-dim` | `#00AA00` | `#A86F00` | `#0090A8` |

| Token | Value | Use |
|---|---|---|
| `--warn` | `#FF3B1F` | Alerts / warnings / **access denied** ONLY |
| `--crt` | `#020402` | Screen background (near-black) |

The phosphor switch is a real OS feature — a Vitality-panel quick action, mirrored in the QUARK assistant. One token swap recolors everything. Never hard-code a hue; always reference the active `--phosphor` / `--phosphor-dim`.

## Typography roles

- **Monofonto** — target system face (squared industrial-technical). Perfected later.
- **Chakra Petch** — current web/build substitute for Monofonto; bundle it in the app. Swap is structural-change-free.
- **Monoton** — neon-tube display accent ONLY; the boot wordmark stamp is the one blessed ceremonial use. Never body or system text.

Wordmark family: Industrial Nameplate (primary text logo) · Atom Lockup (icon/badge with "FIELD OPS") · Monoton accent (sparingly).

## Sound palette (locked direction; final masters at polish)

Signature four:
- **Boot** = power-up sweep (warm rising tone, "system alive").
- **Access Denied** = harsh buzz (low, tremolo'd; shares QUARK's Warn language).
- **Access Granted** = crisp two-note + soft sub.
- **Keypad key** = tight high relay tick.

Supporting: UI-select "clunk" · Vitality-roll ratchet · PLEASE-STANDBY processing pulse · Phosphor-switch retune sweep · Stealth power-down / release power-up · Beacon warn-blip ×3 · Device-Secured latch.

QUARK non-verbal chirps (wordless, distinct from her spoken voice): Scan (rising interrogative) · Happy (bright two-note + sparkle) · Warn (shares the denial language).

All synthesized, brief, functional. No cinematic swell.

## Verified Android-rendering notes (checked June 2026)

The look is GPU work, not bitmaps, and the platform has specific current rules. These were verified against live Android docs:

- **CRT effects = AGSL shaders** in a `RuntimeShader` / `RenderEffect` layer (API 33+). Do NOT animate phosphor glow, scanlines, vignette, or flicker with CPU-bound Compose loops or continuous canvas redraws — that causes jank and battery drain. Push the per-pixel math to the GPU.
- **Static at rest = real recomposition discipline.** Wrap invariant data (vitals, layout indices, terminal config) in `@Immutable` / `@Stable` so Compose skips redrawing static nodes during live updates. This is also how we honor "zero idle redraw."
- **Edge-to-edge is mandatory** for apps targeting Android 15+ (SDK 35). Use `enableEdgeToEdge()` (the modern call; `setDecorFitsSystemWindows(window, false)` is only the manual fallback). We then own inset handling — the CRT-falloff container must account for the status/navigation/cutout insets itself.
- **Back gesture**: predictive back is on by default (Android 16/17). For the locked shell we *consume* back internally (it must not exit our surface); intercepting at the root also suppresses the system "preview home" animation, which is what we want. Add `android:enableOnBackInvokedCallback="true"` and handle via `BackHandler`.
- **Stack is validated**: as of Android 17, Google's official stance is "Compose-first" — all new APIs are Compose-only and legacy Views are in maintenance mode. Native Kotlin + Compose (minSdk 33, target latest) is the correct and future-proof base.

Heavier code conventions (project structure, full Compose patterns) belong to the separate Build-Conventions skill when it exists; this file covers only the design-to-platform mapping.
