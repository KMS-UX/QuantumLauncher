# QuantumOS — M7 Task Brief: Ship + Field-Test
*Version 1.1 — updated at the pre-M7 interim check (Build Bible v0.29, decision 71)*

> **Purpose.** M7 is the last milestone before Checkpoint β. Unlike M0–M6, its center of gravity
> isn't a new feature — it's two things: making the project's most irreversible artifact (the signing
> key) safely, and proving the whole build is stable enough to live on the device. This version adds
> one concrete optimization task (shader intensity) and closes one open question (typography) so
> Claude Code isn't guessing at either.

---

## 0. What changed since v1.0 (read this first)

Two decisions were resolved at the pre-M7 interim check and are now locked scope for this milestone:

1. **Typography — no action needed.** Chakra Petch (shipped real in M6) is confirmed as the
   system's shipping face through Checkpoint β. **Do not swap fonts, do not chase Monofonto in M7.**
   Monofonto remains a deliberately queued future identity-stage item — out of scope here.
2. **Shader intensity — new task added (§2 below).** The Director reviewed the M6 shader on real
   hardware and judged it noticeably more subtle than the earlier demo prototypes. This is **not**
   accepted as a final look. M7 adds an explicit optimization pass before the ship build is cut.

---

## 1. Scope at a glance

| # | Task | Type |
|---|---|---|
| 1 | Signing-key ceremony | New, irreversible — ceremony-level care |
| 2 | Shader intensity optimization | New, hardware-tuning |
| 3 | Full on-device regression pass (M0–M6) | Verification |
| 4 | Build signed release APK | Build |
| 5 | Sideload + live field-test | Hardware |

No new UI, no new features. Everything below either *ships* what M0–M6 already built, or *tunes*
what M6 already shipped.

---

## 2. Shader intensity optimization (new in v1.1)

**Why:** The CRT/phosphor treatment is not decoration — it's the single most identity-defining
surface in the whole OS (Build Bible §2, "the used future"). The Director's call on real hardware
was that it currently reads more subtle than the demo prototypes did. That gap should be closed
deliberately, not left to default.

**What to do:**
- Locate the CRT treatment layer's tunable parameters (the AGSL `RuntimeShader` / `RenderEffect`
  stack built for M6 — scanline opacity/spacing, edge-vignette falloff curve and radius, flicker
  amplitude/frequency, and phosphor glow intensity/spread on text and lines).
- These were built **tunable by design** specifically so intensity could be dialed on real hardware
  (per the Launcher Build Spec's CRT-treatment-layer note) — this task is using that design, not
  fighting it.
- Target: **match or exceed the perceived intensity of the earlier in-Claude demo prototypes**
  (Ignition Lab / App Shell Lab), as judged live on the Fold 6 — not a numeric spec, a Director
  eyeball call.
- Suggested approach: expose the 3–4 key parameters as a temporary on-device debug toggle (or a
  quick recompile-and-flash loop) so the Director can compare 2–3 intensity steps side-by-side on
  hardware in one sitting, rather than judging single static builds one at a time.
- **Constraint:** stay inside the existing zero-idle-redraw rule (decision 41) — turning up
  intensity must not turn a one-shot/triggered shader effect into a continuously-redrawing one.
  A quick GPU-profile or frame-stat check while the boosted shader is running is a reasonable
  sanity check before calling this done.
- **Done when:** the Director confirms, live on the Fold 6, that the shipped intensity reads as
  strong as (or stronger than) the original demos — and that nothing about it introduced new idle
  battery draw.

---

## 3. Signing-key ceremony

This is the one truly irreversible artifact the project has produced — code can always be revised;
a lost or leaked signing key cannot be recovered or swapped without breaking every future update.
Treat it with the same explicit, no-improvising care M1 gave the rollback rehearsal and M4 gave the
new-permission walkthrough.

- Generate the release keystore.
- Confirm the keystore + its credentials are backed up somewhere durable and *off* the build
  machine alone (the Director should know exactly where, and be able to say so without checking).
- Confirm the key is never committed to the GitHub repo (check `.gitignore` covers it before any
  commit in this milestone).
- This step happens **before** anything else in M7 — same pattern as M1's rollback-first step.

---

## 4. Full on-device regression pass

Confirm every milestone still works together, not just individually — M7 is the first time
everything M0–M6 built is exercised in one continuous session.

- **M0** — phosphor screen, hue cycle (green/amber/cyan).
- **M1** — HOME intent, set-as-default, APPS grid lists/launches real apps.
- **M2** — fill-and-adapt layout (unfolded + folded if convenient), adaptive grid, STATUS, LOG.
- **M3** — Vitality atom-mark roll-down, Readiness/Signal/Power/Core-Temp/Uptime, Stealth (no color
  wash-out), Phosphor, Beacon (overrides Stealth correctly), Lock (cosmetic).
- **M4** — floating QUARK trigger survives host-app termination, drag + edge-snap.
- **M5** — QUARK Assistant View: all four reactive states, command rail, free-text entry,
  conversation log, Scripted-Line Library content including the crisis-tier resource text.
- **Deployment Region / Boot Pace patch** — region toggle + persistence across restart; Boot Pace
  Deliberate/Snappy + persistence.
- **M6** — boot ceremony end-to-end on a true cold boot, sound pass, and the optimized shader
  (§2 above) — confirm intensity change didn't regress anything else in the CRT stack.

---

## 5. Build, sideload, field-test

- Build the **signed release APK** (not a debug build) using the keystore from §3.
- Sideload to the **Fold 6**.
- Live field-test session — Director judgment is the final gate, same as every prior milestone.
- On confirmation: **Checkpoint β is reached.**

---

## 6. One honest scope note for the Director

β confirms feel on the **Fold 6** — the dev/test device. The final deploy device is a Pixel with a
different panel and screen geometry; the shader-intensity call made here on the Fold is the best
available signal right now, but it's worth expecting one more look once the actual Pixel is in hand.
