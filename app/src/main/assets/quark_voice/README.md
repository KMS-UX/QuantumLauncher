# `quark_voice/` — on-device assets for QUARK's custom voice (Phase 2b)

The custom voice runs on **sherpa-onnx** (`SherpaKokoroVoiceEngine`), which does Kokoro inference
**and** espeak-ng phonemization internally. The voice identity is owned and small; only the large
model is provisioned on-device.

| Asset | Committed? | What |
|---|---|---|
| `quark_voice_H2.f32` | ✅ yes (~512 KB) | The locked **QUARK-H2** embedding — 510×256 little-endian float32. This is a valid **1-speaker sherpa `voices.bin`** as-is; the engine copies it into place and uses `sid = 0`. |
| `kokoro_vocab.json` | ✅ yes | Kept for reference (the old raw-ONNX path's phoneme map). sherpa uses its own `tokens.txt` from the model dir, so this is not read at runtime. |

**Not here (provisioned on-device):** the sherpa Kokoro model dir — `model.onnx`, `tokens.txt`,
`espeak-ng-data/` — imported from the `kokoro-multi-lang-v1_0` tarball via the debug
`[IMPORT VOICE MODEL]` action (extracted to `filesDir/quark_voice/kokoro/` by
`VoiceModelProvisioner`).

Also required (build-time, not here): the sherpa native libs in `app/src/main/jniLibs/`. See
`voice/quark-phase2b/HANDOFF.md` for the full turnkey steps.
