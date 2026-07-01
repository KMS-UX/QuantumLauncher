# QUARK Phase 2b — Canonical Voice (locked)

**Decision of record for QUARK's spoken-voice identity (closes Build Bible decision 42).**
Director-chosen 2026-07-01 from a real Kokoro-engine audition (6 rounds, candidate **H2**).

## The voice

A **synthetic blend** — original by construction, matching no single shipped voice,
owned outright, offline. `af_nicole` leads for closeness/breath, `af_bella` carries
the richness, a whisper of `bf_emma`/`af_aoede` for clarity.

| Param | Value |
|---|---|
| Engine | `kokoro-onnx`, model `kokoro-v1.0` (StyleTTS2 lineage) |
| Sample rate | 24 000 Hz |
| Language / G2P | `en-us` (espeak-ng) |
| **Speed** | **1.02** |
| **Blend** | `af_bella 0.40 + af_nicole 0.56 + bf_emma 0.035 + af_aoede 0.0025 + af_heart 0.0025` |

Weights are relative (normalized at blend time). See `quark_voice_recipe.json`.

## What's in here (owned, reproducible)

| File | What it is |
|---|---|
| `quark_voice_recipe.json` | Machine-readable recipe (engine, model, speed, blend weights) |
| `quark_voice_H2.npy` / `.f32.bin` | The **exported speaker embedding** — shape `(510,1,256)` float32. The actual owned voice; drop-in for the on-device ONNX runtime. |
| `generate_quark_voice.py` | Regenerates the embedding + reference master from the recipe |
| `reference-master/QUARK_*.wav` | The four canon reference clips + a stitched `ALL4` |

The **model weights are not committed** (`kokoro-v1.0.onnx` ~325 MB, `voices-v1.0.bin`
~28 MB) — fetch from the [kokoro-onnx model release](https://github.com/thewh1teagle/kokoro-onnx/releases/tag/model-files-v1.0).
The `.npy` embedding here is the small, owned artifact; the model is a reproducible dependency.

## The four reference registers

All original QUARK canon (Persona Pack v1.0 §9/§7). Acceptance bar: **Happy** (warm)
and **Warn** (grave, no-wit) must sound audibly different — they do.

1. **Happy** — "…You're back now. Welcome back, Operator."
2. **Idle status** — "Readiness degraded, 64 percent…"
3. **Warn** — "Power critical, 8 percent. Charge now or we go dark soon…"
4. **Refusal / boundary** — "…I just won't tell you it's safe when it's not."

## Integration — NOT yet done (the remaining Phase 2b on-device work)

Per the Synthetic-Seed note §3, still to do on the Fold 6:

1. **On-device runtime.** Swap the Phase 2a placeholder (`QuarkVoiceEngine`, Android
   `TextToSpeech`) for a Kokoro ONNX runtime that consumes `quark_voice_H2.f32.bin`,
   behind the **same 2a debug/voice toggle**. Caller API is unchanged by design.
2. **Latency re-check on hardware.** Kokoro runs slower than Piper/Android-TTS. Measure
   warm spoken-reply latency on the Fold 6; warm the engine at boot / first Scan so cold
   cost hides in the reactive beat.
3. **Fallback decision (only if measured latency forces it).** If too slow to read as
   *deliberate*, keep the fast engine for real-time lines and reserve QUARK-H2 for
   set-pieces (boot greeting, status reports). Record as a decision, not a silent
   degradation.
4. Confirm reactive-state sync, Stealth-mute respect, and Scan-beat sequencing carry over
   from 2a unchanged.
