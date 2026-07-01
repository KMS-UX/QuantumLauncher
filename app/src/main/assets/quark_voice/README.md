# `quark_voice/` — on-device assets for QUARK's custom voice (Phase 2b)

Consumed by `KokoroVoiceEngine`. The voice identity is owned and locked; only the (large) model
weights are fetched rather than bundled.

| Asset | Committed? | What |
|---|---|---|
| `quark_voice_H2.f32` | ✅ yes (~512 KB) | The locked **QUARK-H2** speaker embedding — 510×256 little-endian float32. This *is* the voice. |
| `kokoro_vocab.json` | ✅ yes | Phoneme→token-id map for kokoro-v1.0 (114 entries). |
| `kokoro-v1.0.onnx` | ❌ no (~325 MB) | Kokoro model weights — **fetched to `filesDir/quark_voice/` at first run**, gitignored. |

Fetch the model from the [kokoro-onnx model release](https://github.com/thewh1teagle/kokoro-onnx/releases/tag/model-files-v1.0)
(`kokoro-v1.0.onnx`) — same weights the build-machine audition used.

`KokoroVoiceEngine.isSupported()` gates on the model file being present **and** an on-device
phonemizer being wired; until both land it returns false and the runtime falls back to the Phase 2a
Android-TTS placeholder. See `voice/quark-phase2b/` for the recipe and reference master.
