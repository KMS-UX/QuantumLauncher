# `jniLibs/` — sherpa-onnx native libraries (QUARK Phase 2b)

The custom voice engine (`SherpaKokoroVoiceEngine`) needs sherpa-onnx's native libs here. They are
**not committed** (`*.so` is gitignored — tens of MB) and there is **no Maven artifact** for
sherpa-onnx Android.

Drop them in from the pinned release tarball, matching the vendored Kotlin API
(`com/k2fsa/sherpa/onnx/Tts.kt`, v1.13.2):

```
wget https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.2/sherpa-onnx-v1.13.2-android.tar.bz2
tar xf sherpa-onnx-v1.13.2-android.tar.bz2
cp -r jniLibs/arm64-v8a  app/src/main/jniLibs/     # Fold 6 is arm64-v8a
# (optional other ABIs: armeabi-v7a, x86_64)
```

Without these, the app still builds and runs — `SherpaKokoroVoiceEngine` catches the
`UnsatisfiedLinkError`, reports UNAVAILABLE, and the runtime falls back to the Phase 2a placeholder.
See `voice/quark-phase2b/HANDOFF.md`.
