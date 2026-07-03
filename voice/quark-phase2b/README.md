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

## Integration — wired on sherpa-onnx; two drop-ins remain

The on-device engine is **`SherpaKokoroVoiceEngine`** (sherpa-onnx: Kokoro + built-in espeak-ng
phonemization), behind the same 2a `// VOICE` toggle, with a `VOICE-ID: PLACEHOLDER/QUARK-H2`
selector and an in-app model importer. Our `quark_voice_H2.f32` embedding is used directly as a
1-speaker sherpa `voices.bin` (`sid = 0`), so H2 — not a stock speaker — is what plays.

**Turnkey finish steps are in [`HANDOFF.md`](HANDOFF.md):**
1. Drop the sherpa **native libs** (v1.13.2) into `app/src/main/jniLibs/` (build-time; no Maven
   artifact exists).
2. **Import the model** on the Fold 6 (`kokoro-multi-lang-v1_0` tarball) via the debug
   `[IMPORT VOICE MODEL]` action.
3. Run the **latency re-check** on hardware (warm start hides cold cost in the Scan beat); ship H2
   live, or reserve it for set-pieces if measured latency forces it — a documented split, not a
   silent degradation.

Until the native libs + model are present the engine reports UNAVAILABLE and the runtime falls back
to the Phase 2a placeholder — the voice loop never goes mute.
