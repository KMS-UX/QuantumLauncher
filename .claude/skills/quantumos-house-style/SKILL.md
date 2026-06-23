---
name: quantumos-house-style
description: The locked visual, motion, sound, and voice design language for QuantumOS — a retro-futuristic "used-future" Android field multi-tool. Use this skill whenever building, styling, or writing ANY QuantumOS surface such as launcher screens, the App Shell, core-app modules (COMMS/FILES/AUDIO/CAM/MAPS/RADIO/SIGNAL/CONFIG), the Vitality panel, the QUARK assistant view, the boot splash, Compose UI code, theme or color tokens, icons, animations, sounds, or on-screen text and microcopy. Trigger this even when the request only mentions "phosphor," "CRT," "the launcher," "the field unit," "QUARK," a phosphor color (green/amber/cyan), or a QuantumOS screen by name — and even if the user does not say "design." If you are generating anything a user of QuantumOS would see, hear, or read, consult this skill first so the output matches the house style instead of defaulting to generic Material styling.
---

# QuantumOS House Style

This skill is the single, fast reference for *how QuantumOS looks, moves, sounds, and speaks*. It is distilled from the Build Bible (§2–§4) and the locked decisions. When any design choice is unclear, the rule is: **does this help the Operator stay vital?** If a flourish doesn't serve operational readiness, cut it.

For the exact hex values, font roles, the full sound palette, and the verified Android-rendering notes, read `references/design-tokens.md`.

## The north star (never violate these)

- **The used future.** Industrial, mechanical, utilitarian, *lived-in* — the future as imagined in the 1950s–80s. Knobs, rings, gauges, CRTs; a tool you maintain, not a glass surface you stroke. Mood lineage only (atomic-age industrial, *Blade Runner* "used future," *Fallout* survival-tech); **every concrete asset is original**.
- **It's a field multi-tool.** The owner is a field operative. The OS must be rugged, legible, and quick under pressure.
- **Vitality = operational readiness.** Judge every surface by: does it keep the Operator alert, equipped, alive? Status at a *gaze* (bars/gauges, not paragraphs); safety actions in a tap or two; nothing decorative on a critical surface.
- **Mechanical over silky.** Motion is stepped, physical, deliberate. A small "please standby" beat reads as a machine doing real work — trustworthy. Silky/instant/sensitive is the *opposite* of the feeling we want. (Latency also lets the system do real work behind a stylish card, so it never appears to hang.)

## Color — the phosphor palette

The screen is a phosphor CRT. One switchable hue recolors the entire OS. Default **green**; alternates **amber** and **cyan**. Everything is drawn in the active phosphor and its dim pair on a near-black CRT ground.

- `--phosphor` / `--phosphor-dim` — the active hue (green `#00FF00`/`#00AA00` default).
- `--warn` `#FF3B1F` — alerts, warnings, **access-denied ONLY**. Never decorative.
- `--crt` `#020402` — the near-black screen background.

Full table (all three hues) in `references/design-tokens.md`. **Do not introduce off-palette colors.** If you need emphasis, use brightness/glow within the active phosphor, not a new color.

## Screen treatment — "CRT falloff"

- **No metal frame, no drawn bezel.** Content **fades to black at the edges** (the Pip-Boy falloff look).
- Layered on top: **scanlines**, edge **vignette**, subtle **flicker**, soft phosphor **glow** on all text and lines.
- On real hardware these are **GPU effects (AGSL shaders), never CPU draw-loops** — see the rendering note in references. Make intensity tunable.
- The space *outside* the active container is pure black (it costs nothing on OLED and reads as chassis), but treat physical camera cutouts as "de-emphasized," not "masked" — the hole is still physically there; design the container to live around it.

## Typography

- **Target face: Monofonto** (squared industrial-technical). **Web/build substitute: Chakra Petch** — slots Monofonto in later with no structural change.
- **Wordmark = a three-role family** (use the right one):
  - **Industrial Nameplate** (Monofonto/Chakra Petch, heavy, wide tracking, glow, stamped between two rules) = the **primary system wordmark**, used anywhere "QuantumOS" appears as *text*.
  - **Atom Lockup** (nameplate + the quantum-atom mark over sub-label **"FIELD OPS"**) = the **logo / app-icon / badge**, used where the name reads as a *mark*.
  - **Monoton** (neon-tube) = a **display accent, used sparingly** (boot flourish, the one ceremonial use). **Never the system text face.**

## Motion language — stepped, physical, reactive

- **Stepped, not interpolated.** Animation advances in discrete clicks (slide-projector / window-blind), not smooth glides.
- **Reactive, not ambient.** Utility marks and QUARK are **static at rest** (zero idle redraw — right for a battery-as-vitality field tool). Life shows only through *functional* motion: QUARK's Scan/Happy/Warn states, stepped roll-downs, scan sweeps, the atom-mark's single spin on open. Functional *status* indicators (e.g. a live "online" pulse) may still blink.
- **Discrete over continuous.** Prefer tap-to-toggle states over finger-tracking gestures (steadier, more predictable).
- **PLEASE STANDBY** is the universal loading card — an original phosphor card (QUARK and/or a film-reel), used for boot and any heavy load/transition. **Never a generic spinner.**
- Motion is **functional**: it should tell the Operator something about system state.

## Sound language — mechanical, stepped, synthesized

The audio twin of the motion language: short, functional, synthesized (relays, mechanical keys, phosphor beeps, CRT-era tones). **No cinematic swells, no orchestral pads.** Signature four: **Boot = power-up sweep · Access Denied = harsh buzz · Access Granted = two-note + sub · Keypad = relay tick.** QUARK's non-verbal chirps (Scan/Happy/Warn) are *wordless* and distinct from her spoken voice. Full palette in references.

## The App Shell (every app inherits this frame)

One universal chrome; modules are *dressed inside* it, never redesigned separately:

- **Opaque nameplate header**: back `◄ APPS` · centered app title · the **Vitality atom pull**. The header is an **opaque chrome layer the Vitality shade tucks behind** when stowed and rolls out from when summoned — the panel must never bleed through.
- **Corner registration marks**.
- **Body pattern: strip → content → action-rail.**
- The persistent **floating QUARK trigger** (app-icon sized, static at rest, draggable with edge-snapping; don't park it over an app's primary control).
- The **channel selector** (HOME / APPS / STATUS / LOG) and the CRT treatment.

**Module identities** (each is a field-tool, not a stock app): **COMMS** = field comms (callsign channels + transmission threads); **FILES** = field file manager; **AUDIO** = field *recorder* first; **CAM** = **Optics** (phosphor viewfinder + reticle); **MAPS** = tactical **Nav**; **RADIO** = broadcast *receiver* (content coming **in**); **SIGNAL** = link *diagnostics* (your link measured **out**); **CONFIG** = the field-unit console. The **RADIO-listens / SIGNAL-measures** split is locked.

## Icons

Original **SVG line-icons** in the house stroke language — consistent weight, themeable with the active phosphor, **no platform-dependent emoji**. (Pixel-level icon masters + per-app Atom-Lockup badges come at the later identity/polish stage; line-icons are the working set until then.)

## Voice & naming (when writing any on-screen text)

- Address the user as **"Operator"** (their name once set). A peer-grade aide — never servile.
- QUARK speaks in the **EDI register**: precise, logical enunciation with subtle emotional inflection; composed, self-aware, wit rationed; **principled loyalty** — honest in the Operator's interest, never flattery. For anything QUARK *says*, match the Scripted-Line Library tone; if a dedicated QUARK-voice skill is present, defer to it.
- Refer to the deploy hardware as **"the device" / "the multi-tool" / "the Field Unit"** — not by model name.
- Microcopy is terse, mechanical, status-reporting. Prefer "ACCESS DENIED" / "LINK NOMINAL" / "PLEASE STANDBY" over chatty app-speak.

## Quick do / don't

- ✅ Stepped motion · static at rest · phosphor-only color · CRT falloff · gauges over text · terse status microcopy · GPU shaders for effects.
- ❌ Smooth/silky easing · ambient idle loops · off-palette accent colors · drawn bezels/metal frames · generic spinners · Material-default chrome · `--warn` as decoration · platform emoji · chatty copy.

When in doubt, re-read the north star and ask the vitality question.
