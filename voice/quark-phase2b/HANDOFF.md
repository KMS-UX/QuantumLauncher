# QUARK Phase 2b — on-device custom voice: HANDOFF / finish steps

**State:** the custom voice (**QUARK-H2**) is locked and owned, and the on-device integration is
fully wired and compiling behind the debug `// VOICE` toggle. It runs on **sherpa-onnx** (Kokoro +
built-in espeak-ng phonemization). Two things must be supplied to make H2 audible — both need either
a build machine with GitHub-release egress or the device, which the authoring session did not have:

1. **The sherpa native libraries** (build-time, into `app/src/main/jniLibs/`).
2. **The sherpa Kokoro model** (on-device, imported once via the app).

Everything else — the engine, the voice embedding, the toggle, the importer, the recipe — is done.

---

## Why sherpa-onnx (and why it's not just a Gradle line)

sherpa-onnx runs Kokoro **and** phonemizes with espeak-ng on-device, which removes the hardest part
(no hand-built G2P). But it publishes **no Maven artifact** (verified: the `com.k2fsa` group is
absent from Maven Central). Integration is therefore:
- **Kotlin API** — source-vendored: `app/src/main/java/com/k2fsa/sherpa/onnx/Tts.kt` (verbatim from
  tag **v1.13.2**, Apache-2.0). Already committed.
- **Native libs** — `.so` files copied into `jniLibs/` from the v1.13.2 android release tarball.
  Gitignored, not committed. **This is step 1 below.**

## Step 1 — native libs (build machine with github.com release access)

```
wget https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.2/sherpa-onnx-v1.13.2-android.tar.bz2
tar xf sherpa-onnx-v1.13.2-android.tar.bz2
cp -r jniLibs/arm64-v8a app/src/main/jniLibs/      # Fold 6 = arm64-v8a (add other ABIs if desired)
```
Keep the tarball version and `Tts.kt`'s vendored tag identical. Then build the APK as usual
(`gradle assembleDebug`). Without this, the app still runs but H2 stays UNAVAILABLE → placeholder.

## Step 2 — the model (on-device, one time)

The model is `kokoro-multi-lang-v1_0` (same Kokoro v1.0 voices our H2 blend came from — style space
matches):
```
https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2
```
On the Fold 6: download that `.tar.bz2` to the phone, then in the QUARK assistant view:
1. Triple-tap the **QUARK** title → debug indicators appear.
2. Tap **`// VOICE-ID: PLACEHOLDER`** → it flips to **`QUARK-H2`**.
3. Tap **`// [IMPORT VOICE MODEL]`** → pick the downloaded tarball. It extracts to
   `filesDir/quark_voice/kokoro/` and the status shows `MODEL READY`.
4. Tap **`// VOICE: OFF`** → `ON`. Type a line → QUARK speaks in **her** voice.

`VoiceModelProvisioner` builds sherpa's `voices` file automatically: a copy of the tarball's own
`voices.bin` (same total size the model expects) with one speaker slot overwritten by our owned H2
embedding (`voices-quark-h2.bin`), and reports back the `sid` that slot lives at. **Do not** point
sherpa at the raw H2 embedding alone — its float count won't match what the model's ONNX metadata
demands, which is a fatal, uncatchable native `_Exit()`, not a recoverable error (this was the cause
of the crash-on-import / crash-on-toggle bug fixed 2026-07-04 — see BUILD_LOG).

## Step 3 — the latency re-check (the actual Phase 2b acceptance)

On the Fold 6, read the LOG channel's `VOICE: TTS_START …ms · PLAYBACK …ms` per spoken reply:
- Engine is warmed at build/first-enable (`warmUp()`), so cold cost should hide in the Scan beat.
- If warm start-latency reads as **deliberate** → ship H2 as the live voice.
- If it reads as **broken** even warmed → keep the placeholder for real-time lines and reserve H2
  for set-pieces (boot greeting, status reports). Record it as a decision, not a silent
  degradation (Synthetic-Seed note §3).
- Confirm reactive-state sync, Stealth-mute respect, and Scan-beat sequencing (all inherited from
  2a unchanged).

---

## The locked voice (for reference)

| | |
|---|---|
| Engine | sherpa-onnx (Kokoro, `kokoro-multi-lang-v1_0`), speed **1.02**, `sid = 0` |
| Blend | `af_bella 0.40 + af_nicole 0.56 + bf_emma 0.035 + af_aoede 0.0025 + af_heart 0.0025` |
| voices.bin | our `quark_voice_H2.f32` (510×256 float32) IS a 1-speaker sherpa voices file |
| Recipe / master | `voice/quark-phase2b/` (recipe JSON, embedding, reference WAVs, generator) |

## Fidelity note to verify on first listen

Our H2 was blended from hexgrad Kokoro-82M **v1.0** voices; `kokoro-multi-lang-v1_0` is built on the
same lineage, so the 256-d style space should match and H2 should reproduce faithfully. If the timbre
sounds off on-device, regenerate the blend from the *sherpa* model's own `voices.bin` (same recipe
weights, same named speakers) and replace `quark_voice_H2.f32`. Low risk, but it's the one thing only
an on-device listen can confirm.
