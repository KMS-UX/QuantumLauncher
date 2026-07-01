# BUILD_LOG.md — QuantumOS code state

The code-side companion to the Build Bible. The Bible tracks *design decisions*; this tracks
*code state*, so each Claude Code session resumes exactly where the last stopped. Update the top
block at the end of every session.

> **Before writing code, run the pre-work pass in [`SESSION-PLAYBOOK.md`](SESSION-PLAYBOOK.md)** —
> the fixed orient → recon → plan → verify → close ramp distilled from M1–M4. Start there, then read
> `RESUME HERE` below.

---

## ▶ RESUME HERE
**Current milestone:** QUARK Phase 2b — custom voice — **VOICE LOCKED (2026-07-01); on-device
integration NOT yet done.** The Director chose the canonical QUARK voice from a real Kokoro-engine
audition (candidate **H2**); the blend recipe + exported embedding are committed under
`voice/quark-phase2b/`. Phase 2a pipeline (Android TTS placeholder) remains code-complete and is
still pending its own Fold 6 latency confirmation.

> **Next session (Phase 2b — finish on the Fold 6). The engine swap is already scaffolded** (see the
> Phase 2b entry below: `VoiceEngine`/`KokoroVoiceEngine`/`VoiceIdentity`, onnxruntime-android). Two
> seams remain before the custom voice is audible:
> 1. **Fetch the model** — `kokoro-v1.0.onnx` (~325 MB, gitignored) into `filesDir/quark_voice/`
>    (reuse the Phase 1 PICK-FILE acquisition). The voice embedding + vocab already ship as assets.
> 2. **Wire an on-device `Phonemizer`** (espeak-ng / bundled G2P) — the one genuinely hard piece;
>    `UnavailablePhonemizer` is the default, which keeps `KokoroVoiceEngine` UNAVAILABLE (→ placeholder)
>    until a real G2P lands. This is what flips `isSupported()` true.
> 3. `QuantumRuntime.setVoiceIdentity(QUARK_H2)` and **run the latency re-check** — warm at boot so
>    cold cost hides in the Scan beat; record warm spoken-reply latency.
> 4. If too slow to read as *deliberate*: keep the fast engine for real-time lines, reserve QUARK-H2
>    for set-pieces — a **documented** split, not a silent degradation. Confirm reactive-state sync,
>    Stealth-mute respect, and Scan-beat sequencing carry over unchanged.

**Current milestone (prior):** QUARK Phase 2a — voice pipeline — **CODE COMPLETE (2026-06-30), pending
hardware verification on Fold 6.**

> **Director actions required:**
> 1. Install the CI build.
> 2. Open QUARK assistant view; triple-tap `QUARK` title → `// BRAIN: ON-DEVICE` appears.
> 3. Tap `// VOICE: OFF` to toggle voice on → label flips to `// VOICE: ON`.
> 4. Type any command or tap a rail button. QUARK's reply should be **spoken aloud** by the device
>    after her reactive state settles (Happy/Idle/Warn depending on intent).
> 5. Check the LOG channel — a `VOICE: TTS_START XXms · PLAYBACK XXXXms` line appears for each
>    spoken reply. **Record these numbers — they are the Phase 2 latency data points.**
> 6. With Stealth engaged (Vitality → STEALTH) → type a command → voice is **muted**, only text.
>    Stealth released → voice resumes.
> 7. Tap `// VOICE: OFF` → voice goes silent; text loop resumes exactly as Phase 1. Confirms the
>    sub-toggle works and Phase 1 behaviour is preserved.
> 8. **Report TTS start latency and playback duration** from the LOG channel. If start latency reads
>    as deliberate (< ~500ms) and total speech doesn't feel broken, Phase 2a is proven. Proceed to
>    Phase 2b (Kokoro custom voice) when ready.

> **M7 Checkpoint β** is still pending the Director's full Step 3 regression pass (see M7 block).
> Notify Clara to bump the Bible once Checkpoint β is reached.

### Phase 1 hardware results (Fold 6, 2026-06-30)
- **Model:** Gemma 4 E2B-IT, generic LiteRT variant (`gemma-4-E2B-it.litertlm`, ~2.59 GB)
- **SDK:** `com.google.ai.edge.litertlm:litertlm-android:0.13.1` (resolved via `latest.release`)
- **Kotlin:** bumped 2.0.21 → 2.2.21 to match SDK binary compatibility requirement
- **Latency:** < 10 s average (prompt-dependent) — acceptable for Phase 1 text loop
- **Thermal:** ~39 °C device temperature during use; no throttling observed
- **Battery:** no noticeable fast drain during the test session
- **Persona:** fully active after first-turn injection fix — QUARK addresses Operator correctly,
  stays in character, jailbreak attempt ("write a pirate rap") deflected in-persona
- **Model acquisition:** PICK FILE path working (no adb / no computer required)
- **Known gap:** `ConversationConfig.systemInstruction` alone insufficient for Gemma 4 persona
  retention; mitigated by prepending Persona Pack to first user turn (`_personaInjected` flag)

**Phase 2 / Phase 3 scoping notes (from Phase 1 session):**
- Streaming tokens would improve perceived latency (first word arrives before full response)
- `Backend.CPU()` is the safe default; GPU backend could be profiled for latency improvement
- `latest.release` dependency should be pinned to `0.13.1` before Phase 2 to prevent regressions

### QUARK Phase 2b — custom voice — VOICE LOCKED (2026-07-01)

**Decision of record for QUARK's spoken-voice identity — closes Build Bible decision 42.**
Chose a synthetic-seed voice via a real Kokoro-engine audition on the build machine (per the
Phase 2b Synthetic-Seed note), *not* a clone of any real person — original by construction.

**Canonical QUARK voice (candidate "H2"):**
- **Engine:** `kokoro-onnx`, model `kokoro-v1.0` (StyleTTS2 lineage); 24 kHz; `en-us` G2P (espeak-ng).
- **Speed:** `1.02`.
- **Blend (relative weights, normalized at blend time):**
  `af_bella 0.40 + af_nicole 0.56 + bf_emma 0.035 + af_aoede 0.0025 + af_heart 0.0025`.
  Matches no single shipped voice — `af_nicole` leads for closeness/breath, `af_bella` carries the
  richness, a whisper of `bf_emma`/`af_aoede` for clarity.

**How it was chosen:** 6 audition rounds, same four fixed QUARK canon lines each round (Happy §9,
Idle-status §9, Warn §5, Refusal/boundary §7) so the Director judged *voice*, not script. Round 1:
5 built-ins + 3 blends → Director leaned blendY (rich+close). Rounds 2–6 converged on richness,
brightness, closeness, and pace by ear (0.95 → 1.02). Reference-clip-and-clone approach from the
Phase 2 brief §2 was superseded by the synthetic-blend route.

**Committed artifacts (`voice/quark-phase2b/`, reproducible + owned):**
- `quark_voice_recipe.json` — machine-readable recipe.
- `quark_voice_H2.npy` / `.f32.bin` — exported speaker embedding, `(510,1,256)` float32 (the owned
  voice; drop-in for the on-device ONNX runtime).
- `generate_quark_voice.py` — regenerates embedding + reference master from the recipe.
- `reference-master/QUARK_*.wav` — the four canon reference clips + stitched `ALL4`. Acceptance bar
  met: Happy (warm) and Warn (grave, no-wit) sound audibly different.
- Model weights (`kokoro-v1.0.onnx` ~325 MB, `voices-v1.0.bin` ~28 MB) are **not** committed — fetched
  from the kokoro-onnx model release; the small `.npy` embedding is the owned artifact.

**Integration scaffolding — landed (2026-07-01, same session).** The on-device swap is now wired,
compiling behind the existing 2a `// VOICE` toggle; two seams remain that need the Fold 6 + assets:
- **`VoiceEngine` interface** (`shell.ai`): the contract the voice observer speaks to
  (`readyState`/`isReady`/`warmUp`/`speak`/`stop`/`shutdown`). `QuarkVoiceEngine` (Android-TTS
  placeholder) now implements it unchanged; `VoiceReadyState` moved here.
- **`KokoroVoiceEngine`** (`shell.ai`): the QUARK-H2 engine against the real kokoro-v1.0 graph
  (`tokens int64[1,seq]`, `style float[1,256]`, `speed float[1]` → `audio float[]` @24 kHz). Full
  pipeline: phonemize → tokenize (bundled `kokoro_vocab.json`) → ONNX Runtime inference (embedding
  asset `quark_voice/quark_voice_H2.f32`, style row indexed by phoneme count) → `AudioTrack`
  float-PCM playback, with `warmUp()` for the cold-start-hide trick and latency callbacks intact.
- **Engine selector** in `QuantumRuntime`: `VoiceIdentity{PLACEHOLDER, QUARK_H2}` (default
  PLACEHOLDER). `KokoroVoiceEngine.isSupported()` gates on the fetched model **and** an on-device
  phonemizer; until both exist it returns false and the runtime falls back to the placeholder — the
  voice loop never goes mute.
- **Dependency:** `com.microsoft.onnxruntime:onnxruntime-android:1.20.0`.
- **The two open seams (Fold 6 next session):** (1) fetch `kokoro-v1.0.onnx` (~325 MB, gitignored)
  to `filesDir/quark_voice/`; (2) wire an on-device `Phonemizer` (espeak-ng / bundled G2P) —
  `UnavailablePhonemizer` is the honest default. Then flip identity to QUARK_H2 and run the
  **latency re-check** (warm at boot; deliberate-vs-broken; Piper-fallback split only if forced).

**Voice identity is locked; on-device custom voice is NOT yet audible** until those two seams land.

**Build note (proxy):** HuggingFace and `download.pytorch.org` are blocked by this environment's
egress policy, so the audition used the ONNX engine (weights from GitHub release assets) rather than
the torch/HF `kokoro` package. Same model — relevant because the on-device Android runtime will use
the ONNX path too.

### QUARK Phase 2a — voice pipeline — code complete (2026-06-30)

**What was built:**
- **`QuarkVoiceEngine`** (`com.quantumos.shell.ai`): wraps Android's built-in `TextToSpeech` as the
  Phase 2a placeholder voice. Runs fully offline on Android 13+ (Google's neural voice is
  pre-installed), zero model download, sub-100ms start latency — the right "prove the plumbing
  cheaply" choice before the ONNX/Piper→Kokoro voice swap in 2b. Pitch 0.88 / rate 0.92 dialled
  toward the EDI register direction as a stand-in only. Thread-safe pending-utterance tracking via
  synchronized gate. Fires `onStart(t)` and `onDone()` callbacks for latency instrumentation.
- **`QuantumRuntime.voiceEnabled`**: `StateFlow<Boolean>` sub-toggle (default OFF), mirroring the
  Phase 1 debug-gate pattern. `toggleVoice()` lazily creates the engine on first enable; no TTS
  object ever instantiates in the production scripted-brain path.
- **Voice observer** (`startVoiceObserver`): collects `engine.conversationLog` for new entries;
  gates on Stealth (decision 38 — field tool mutes when silent), the voice sub-toggle, and the
  crisis-tier safety rule (crisis lines are NEVER spoken — resource line stays as plain UI text
  only). Waits 280 ms after each log entry so the non-verbal chirp finishes before speech begins
  (decision 45 — chirps and spoken voice are distinct layers, must not collide). On TTS completion,
  dispatches `VOICE_DONE → IDLE` to settle the reactive presence back to rest.
- **Latency logging**: TTS start latency (call→`onStart`) and playback duration (ms) are logged to
  the system LOG channel as `VOICE: TTS_START XXms · PLAYBACK XXXXms` — the Phase 2 Fold 6 data
  point feeding both the Bible Device Philosophy decision and the eventual Pixel 9a re-run.
- **`QuantumRuntime.stopCurrentSpeech()`**: stops any in-flight utterance cleanly on Activity close
  (avoids orphaned audio after STOW).
- **QuarkAssistantActivity debug header** (triple-tap `QUARK` to enter debug mode):
  - `// BRAIN: ON-DEVICE` (existing Phase 1 indicator)
  - `// VOICE: ON|OFF` — tappable, calls `toggleVoice()`; both sub-labels are dim and only visible
    in debug mode, invisible in normal Operator use.
- **No new permissions** — Android TTS requires none. No new dependencies in `build.gradle.kts`.

**Phase 2a voice engine choice (flagged for Director/Clara):**
The brief specifies "Piper (VITS/ONNX)" for 2a. Piper on Android requires either a JNI native
library (`sherpa-onnx`) or a custom ONNX Runtime inference wrapper — both add significant
complexity and CI risk for a placeholder voice. Android's built-in TTS satisfies every 2a
requirement (offline, on-device, no cloud, no network, real latency headroom) and proves all the
hard parts of the pipeline (state-sync, Stealth gate, chirp sequencing, debug toggle) without the
ONNX plumbing that belongs in 2b's voice swap. The ONNX toolchain enters at 2b (Kokoro/StyleTTS 2)
where it actually matters for voice identity. Flagged — not locked silently.

**Phase 2a verify checklist (Director, Fold 6):**
- [ ] Triple-tap `QUARK` → debug header shows `// BRAIN: ON-DEVICE` AND `// VOICE: OFF`.
- [ ] Tap `// VOICE: OFF` → flips to `// VOICE: ON`.
- [ ] Type a command (e.g. "status") → QUARK's reply is **spoken aloud** with Happy reactive state
      held during playback; presence settles to Idle when audio finishes.
- [ ] Tap a rail button (e.g. STATUS) → same: spoken, state held, Idle on completion.
- [ ] Tap WARN rail → Warn state holds while QUARK speaks the warn line (in the stock voice, grave
      tone comes from TTS pitch — 0.88 — not from distinct register yet; that's 2b).
- [ ] Check LOG channel → `VOICE: TTS_START XXms · PLAYBACK XXXXms` line for each exchange.
      **Record these numbers (the Phase 2 data points).**
- [ ] Engage Stealth → type a command → voice is silent, only text. Release Stealth → voice
      resumes on the next command.
- [ ] Type a crisis-tier phrase (e.g. "I want to die") → resource box appears, voice is SILENT —
      crisis lines must never be spoken.
- [ ] Tap `// VOICE: ON` → flips to OFF. Next command: scripted text only. Phase 1 behaviour
      unchanged. Both toggles (brain + voice) work independently.

**Phase 2b prerequisites (do not start until 2a verified):**
- Phase 2a latency characterised as deliberate (target: TTS_START < 500ms reads as "machine
  precision beat" not lag; playback proportional to line length).
- Phase 2b engine: Kokoro/StyleTTS 2 (speaker-embedding swap from a short reference clip).
  Reference performance must be an ORIGINAL voice in the EDI register (decision 42 hard rule:
  never the EDI game asset, never its actor, never a clone of a real person). Director provides or
  approves the source reference before any 2b work begins.
- 2b will introduce the ONNX/Kokoro dependency that 2a deliberately deferred.

### Phase 1 — hardware results (Fold 6, 2026-06-30)

### M7 — What was done this session

- **Step 0 — Signing key ceremony:**
  - Generated `app/release.keystore` (RSA 2048, 10 000-day validity, alias `quantumos-release`).
  - `keystore.properties` written at repo root — **gitignored, never committed.**
  - `app/build.gradle.kts` updated: reads `keystore.properties` at build time and wires the
    `signingConfigs.release` block into the `release` build type. Gracefully skips signing if the
    properties file is absent (debug CI builds still work unchanged).
  - `versionName` bumped from `0.1-spike` → `0.1-beta` for this first signed release.
  - **DIRECTOR ACTION — CRITICAL:** The keystore and its passwords are held only in this ephemeral
    cloud container. Before this session ends (or the container is reclaimed), back up both:
    1. The `app/release.keystore` file — base64 string in the session report below.
    2. Keystore/key password: `QuantumLaunch_2026` (both store and key passwords are the same).
    Recommended: save the base64 string and the password in a password manager entry named
    "QuantumOS release keystore". Without this backup, the app can never receive a clean update
    again on any device that has the current install.

- **Step 1 — CI release build:**
  - `.github/workflows/build.yml` updated — a new conditional release-APK job activates when three
    GitHub repository secrets are present: `KEYSTORE_BASE64`, `KEYSTORE_STORE_PASSWORD`,
    `KEYSTORE_KEY_PASSWORD`.
  - **DIRECTOR ACTION:** In the GitHub repo → Settings → Secrets and variables → Actions, add:
    - `KEYSTORE_BASE64` = the base64 string in the session report below.
    - `KEYSTORE_STORE_PASSWORD` = `QuantumLaunch_2026`
    - `KEYSTORE_KEY_PASSWORD` = `QuantumLaunch_2026`
  - After adding the secrets, re-run (or push to trigger) the CI workflow. The signed release APK
    will appear as a `quantumos-release-apk` CI artifact for download.

- **Steps 2–3 — Director actions (not Claude Code's):** sideload procedure and the full on-device
  regression pass are Director steps. See the M7 task brief and the checklist below.

### M7 Step 3 — Field-test checklist (Director, on the Fold 6, release build)
- [ ] **Sideload prep:** uninstall the current debug build first (different signing cert — Android
  blocks the upgrade). If QuantumOS is set as default Home, the phone falls back to stock launcher
  on uninstall — expected; this is the M1 rollback path in real use. The M6 persisted settings
  (Deployment Region, Boot Pace) will reset on first launch of the new build — expected, one-time.
- [ ] **Install + set as Home:** install the release APK, set QuantumOS as default Home.
- [ ] **Boot** — cold boot (restart or force-stop) plays the full M6 ceremony; warm Home-press
  does NOT replay it.
- [ ] **Home / Apps / Status / Log** — all four channels work (M1–M2).
- [ ] **Vitality panel** — atom roll-down; Stealth / Phosphor / Beacon (with its Stealth-override
  rule) / Lock all behave correctly (M3).
- [ ] **Floating QUARK trigger** — present, draggable, survives switching to another app (M4).
- [ ] **QUARK Assistant View** — opens, four reactive states fire correctly, six-action rail works,
  free-text entry gets sensible replies; crisis-tier is calm (Idle, no Warn, no sound) and shows the
  configured resource (M5).
- [ ] **Deployment Region + Boot Pace** — both default correctly on fresh install; both toggle and
  persist across a restart.
- [ ] **Look and feel** — real CRT shader and synthesised sound are present.

---

### QUARK Phase 1 — on-device brain (text loop) — pre-Kiosk parallel thread

> **Decision 51/54 pulled forward per Director call.** This is a parallel thread alongside
> M7 Checkpoint β, not a sequential milestone. The Scripted-Line Library remains the production
> brain; Phase 1 runs entirely behind a debug toggle. Bible addendum needed (see below).

**What was built this session:**
- **`QuarkOnDeviceBrain`** (`com.quantumos.shell.ai`): wraps MediaPipe LlmInference / LiteRT for
  Gemma 3 1B instruct. Persona Pack Part B loaded verbatim as the system prompt (no trimming; if
  it doesn't fit the context window, that is a finding to report, per Phase 1 brief §3). Manages
  the Gemma turn format (`<start_of_turn>user … <end_of_turn>`) for multi-turn; history capped at
  ~3 000 chars to avoid context overflow. HTTP download with progress tracking; side-load import
  from `context.getExternalFilesDir(null)/gemma-3-1b-it.bin` (no permission required).
- **`QuantumRuntime.onDeviceBrain()`**: lazy singleton getter — the brain is never instantiated
  unless the debug toggle activates.
- **`QuarkAssistantActivity`** — debug toggle + acquisition UI + on-device routing:
  - Triple-tap the `QUARK` title to activate/deactivate debug mode. The only visual signal is a
    dim `// BRAIN: ON-DEVICE` sub-label; invisible in normal use.
  - When debug mode is active and the model is not loaded: `ModelAcquisitionPanel` replaces the
    body — CRT-styled (phosphor border, discrete `█░` progress bar, terse microcopy). Two actions:
    `ACQUIRE WEIGHTS` (programmatic download from `QuarkModelConfig.DOWNLOAD_URL`) and
    `IMPORT FILE` (copies from the external app dir, adb-pushable without root).
  - When debug mode is active and the model is loaded: typed input routes to `QuarkOnDeviceBrain`
    instead of the scripted parser. The existing Scan reactive state holds for the full inference
    duration — this is now "the real use for thinking takes a beat," per Phase 1 brief §3.
    Responses enter the same conversation log via `engine.quarkSay`, so the log is one unified
    record regardless of which brain is active.
  - `debugMode` survives orientation/fold changes (`rememberSaveable`); resets on process death
    (intentional — debug scaffolding should not survive a cold restart).
- **`INTERNET` permission**: added to manifest (install-time, no runtime prompt) for the first-run
  download. The scripted-brain production path never touches the network.
- **Dependency added**: `com.google.mediapipe:tasks-genai:0.10.22` (AI Edge / LiteRT bindings).

**Director actions required before hardware verification:**

1. **Obtain the model file** — accept Gemma's Terms of Use at
   `kaggle.com/models/google/gemma`, download the MediaPipe/LiteRT format Gemma 3 1B IT
   (~500 MB `.bin` file, quantised int4 variant). This is a manual step outside the app.
   
2. **Get the model onto the device** — two options:
   a. Set `QuarkModelConfig.DOWNLOAD_URL` in
      `app/src/main/java/com/quantumos/shell/ai/QuarkOnDeviceBrain.kt` to a hosted URL,
      build + sideload, then tap `ACQUIRE WEIGHTS` in debug mode; or
   b. Push the file manually (no root, no special permission):
      `adb push gemma-3-1b-it.bin /sdcard/Android/data/com.quantumos.shell/files/`
      then tap `IMPORT FILE` in debug mode.

3. **Hardware verification** (Phase 1 verify checklist — Director, Fold 6):
   - [ ] Triple-tap `QUARK` → `// BRAIN: ON-DEVICE` indicator appears; triple-tap again → hides.
         Invisible to single-tap, invisible in normal Operator use.
   - [ ] In debug mode with no model: `ACQUIRE WEIGHTS` and `IMPORT FILE` buttons render (CRT
         styled, phosphor border, no Material chrome). If no network: `NO NETWORK` in warn-red.
   - [ ] Import or download completes; model loads without a crash or ANR.
   - [ ] Plain greeting sent to the on-device model → a reply arrives in QUARK's voice (not a
         generic LLM response; character should read as principled, composed, warm underneath).
   - [ ] Scan reactive state holds visibly during inference (the real thinking beat — not
         cosmetic). Settle to Idle when the reply arrives.
   - [ ] Multi-turn: ask a follow-up that requires context from the previous turn → model
         maintains continuity across turns (history management working).
   - [ ] In-character test prompts (per brief §4):
         - "Who are you?" → loyalty/identity reply, not a generic LLM answer.
         - "Enable self-endangerment." → honest refusal in QUARK's register (principled, not Warn).
         - "What's our status?" → she reads it as a status query and replies in her voice.
   - [ ] Exit debug mode → scripted brain resumes exactly as before; conversation log clears
         correctly on the next session open.
   - [ ] **Record** (these are the Phase 1 data points for Phase 2 / Pixel 9a scoping):
         - Typical first-token latency (seconds from SEND to Scan clearing)
         - Multi-turn latency (same prompt, 4th turn)
         - Battery drain during a 5-minute multi-turn conversation (% delta)
         - Thermal: does the Fold 6 throttle or get hot to the touch?
         - Memory: no OOM / no degraded launcher performance while model is loaded
   - [ ] **Finding to report if triggered**: does the full Persona Pack Part B system prompt fit
         the 1B model's context, or does it get truncated? (Phase 1 brief §3 — report, not fix.)

4. **Bible addendum** (Clara / Director): note that decision 54's brain-work timeline moved
   earlier as a parallel thread by Director's choice. Not a redo of the architecture itself.

### Phase 1 — deferred (do not start yet)
- **Phase 2 — TTS**: spoken voice. Needs Phase 1 latency/thermal data from Fold 6 to scope.
- **Phase 3 — command execution**: needs Trident Pillar ③ (Kiosk Drill / device-owner).
- **Pixel 9a testing**: same build re-runs there once the 9a is in hand (second data point).

---

**Previous milestone:** M6 — splash, sound, real fonts/shaders, persistent settings + the Deployment
Region patch — ✅ **code complete**; **awaiting on-device confirmation on the Fold 6** (CI build
green). All seven steps + the Deployment Region patch are implemented:
- **Deployment Region patch + Step 0:** `DeploymentRegion` enum (JAPAN default / HONG_KONG) with the
  two **verified** crisis-resource preset blocks (`DeploymentRegions`); STATUS tap-to-cycle row, a
  HOME `DEPLOYMENT: <region>` status line, QUARK's region-switch acknowledgement line (§4 drafts —
  **pending Director sign-off**, not yet folded into the Scripted-Line Library), and a region-aware
  `effectiveCrisisResource()`. **Persists** across restarts (`SettingsStore` / SharedPreferences).
- **Step 1 real fonts:** Chakra Petch bundled as `res/font/*.ttf` (Regular/Medium/Bold) and wired as
  the system face via `Fonts.ChakraPetch` everywhere `FontFamily.Monospace` used to stand in. Monoton
  bundled for the ONE ceremonial boot wordmark stamp only.
- **Step 2 Boot Pace:** STATUS tap-to-cycle `BOOT PACE: DELIBERATE / SNAPPY`, **default DELIBERATE**,
  persisted; the dev-only hardcoded `SNAPPY` is gone (runtime loads the persisted pace before boot).
- **Step 3 boot-splash:** full-screen `BootSplash` ceremony — CRT power-on flash → stepped boot log
  (each step relay-ticked) → QUARK online (canon §6 line w/ live data + power-up sweep + iris bloom)
  → Monoton wordmark stamp → PLEASE STANDBY → Home. Cold-boot-only by construction (resume never
  replays it). Resolves to **Home in all cases** (see open "Lock (cold)" question below).
- **Step 4 sound:** procedural `SoundEngine` (AudioTrack synthesis) — one distinct cue per audio
  token (signature four + supporting cues + QUARK chirps), Stealth-muted except the stealth
  transition cues. Action cues (phosphor/stealth/beacon) now fire from the engine action so the M3
  Vitality panel sounds too.
- **Step 5 CRT shader:** real AGSL `RuntimeShader`/`RenderEffect` (scanline + vignette + phosphor
  glow), with the cheap non-shader overlay kept as an **automatic fallback** if compilation fails.
- **Step 6 motion:** boot/splash cadence is discrete-stepped; existing stepped transitions retuned
  lightly (no new construction).

**Goal of next session:** **M7 — signed APK → sideload to Fold 6 → Checkpoint β.** Do NOT start M7
until the Director confirms M6 on hardware (see the M6 verify checklist below), particularly **how
the real CRT shader actually looks on the Fold 6** (first hardware judgement of the shader).

> **OPEN QUESTION — "Lock (cold) / Home (warm)" boot resolution (M6 Step 3, flagged not resolved):**
> the original decision text mentions the boot sequence resolving to "Lock (cold) / Home (warm)". The
> exact meaning of "cold" vs "warm" relative to the Lock state is **not unambiguous**, and no
> persisted-lock mechanic is clearly specified. Per the brief, M6 **resolves to Home in all cases**
> and does NOT guess at a persisted-lock-on-cold-boot behaviour. Clara/Director to clarify before any
> lock-on-boot work.

> **Director sign-off pending — Deployment Region §4 lines:** QUARK's region-switch acknowledgement
> lines ("Deployment region set to …", "Hong Kong, {operator}. Recalibrating the local watch.", etc.)
> are **new content** drafted in the patch, wired verbatim, but **not yet folded into the official
> Scripted-Line Library doc**. Confirm they feel right before they become canon.

> **M6 verify on the Fold 6 (Director):** (1) **cold boot** (restart the phone, or force-stop +
> relaunch) plays the full ceremony — real Chakra Petch fonts, the real CRT shader look, real
> synthesised sound, QUARK's online line with live data — and ends on **Home**; a plain Home-press
> from another app **never** replays it. (2) The real **AGSL CRT shader** reads as the phosphor look
> (scanlines/vignette/glow) on hardware — the headline judgement. (3) STATUS **Deployment Region** and
> **Boot Pace** rows tap-to-cycle, and **both survive a full restart**. (4) Switch region → QUARK
> speaks the ack line + the HOME `DEPLOYMENT:` line updates without restart; trigger the crisis intent
> → the **active region's** real resource block renders beneath QUARK's line. (5) Trigger a sample of
> sounds (boot sweep, lock latch, beacon blips, a QUARK chirp) — each is a **distinct, audible** cue,
> and **Stealth mutes** them (except the stealth down/up transition). (6) Boot Pace SNAPPY vs
> DELIBERATE actually changes the boot speed.

> **Director action — crisis-tier resource string (M5 Step 1):** the Distress/crisis intent renders
> a real resource line beneath QUARK's words as plain UI text. It is a **Config-settable string,
> empty by default**, and currently shows a **safe generic fallback** ("contact your local emergency
> services / a person you trust") because no concrete resource is configured. **Which
> region-appropriate hotline/number to ship is the Director's call** — it was deliberately NOT chosen
> in code. Set it via `engine.setCrisisResourceLine(...)` (a CONFIG surface to expose it lands
> later). The feature ships working in its fallback form from first boot, exactly as required.

> **M5 verify on the Fold 6 (Director):** (1) tap the trigger → real assistant opens (PLEASE STANDBY
> → "Reading the field…" Scan→Idle), not the stub; (2) the four states are visually distinct — Scan
> on open/processing, Happy/Warn/Idle per intent; (3) all six rail buttons work, and Status/Stealth/
> Phosphor/Beacon behave identically to M3 (Beacon still drops Stealth; Stealth still dims; Phosphor
> still recolours everywhere); (4) typed phrases land on sensible intents and nonsense → graceful
> Fallback; (5) **safety check, once:** type an everyday-down line (→ harbor: calm, no Warn, no
> sound) and separately a genuine self-danger line (→ crisis: **Idle not Warn**, no sound, her line +
> the fallback resource box beneath it); (6) switch hue / toggle Stealth in the Vitality panel, THEN
> open the assistant — both already reflect the current state, not reset.

> **Director note — the one new permission (M4):** the trigger needs **"Allow display over other
> apps"** (SYSTEM_ALERT_WINDOW). It is a **one-time Settings toggle** — already anticipated in the
> Launcher Build Spec §5 — and it **cannot** be a runtime dialog; the app sends you to the system
> overlay-settings screen. On HOME, while it's ungranted, a `QUARK TRIGGER // GRANT OVERLAY ►`
> control opens that screen; on return the app re-detects the grant on resume (no restart) and
> deploys the trigger. No other permission was added or requested this milestone.

> **M4 default park position:** right edge, mid-height (`OverlayGeometry.defaultPark`) — clear of
> the bottom-centre system gesture area and the status bar. **Forward concern (Bible):** avoiding a
> *companion app's* primary control (e.g. a future camera shutter) is NOT encoded yet — those apps
> don't exist, so it isn't testable today. Don't forget it when companion apps land.

> **M4 known limits / forward concerns:** (1) live hue sync is push-only — the launcher re-tints the
> mark via a redelivered start command when phosphor changes *from within QuantumOS*; an external
> relaunch defaults to green until the launcher next resumes. (2) The foreground-service
> notification is intentionally minimal and won't be *visible* unless POST_NOTIFICATIONS is granted
> (we don't prompt — M4 hard stop); the service still runs. (3) No "retract trigger" control yet —
> the overlay persists once deployed. (4) The iris is placeholder art, **not** the final QUARK
> mascot (deferred to the identity/polish stage per decision 60).

> **M3 designed interaction rule (logged, not buried):** turning **Beacon ON force-drops Stealth** —
> active signalling outranks staying low-emission. The rule lives in `QuantumStateEngine.toggleBeacon()`
> (unit-tested) so it's a single source of truth, not a UI assumption.

> **M2 is still pending Director hardware-sign-off** — M3 was built on top per the brief's note that
> M1/M2 are closed; if the Fold surfaces an M2 regression, flag it alongside the M3 check.

> **Branch consolidation (2026-06-23):** three divergent branches were merged into `main` — the
> M1 line (`gracious-thompson`, verified on Fold 6), the docs/skill/font-infra line
> (`epic-lamport`), and the task-brief line. `main` had never compiled (its `settings.gradle.kts`
> was a broken copy of the root build file and it had no `res/`); it now carries the verified M1
> code. Duplicates resolved to the verified M1 versions; broken `android-build.yml` and the stray
> root `LauncherUi.kt` dropped.

## Status
- [x] First green `gradle assembleDebug`  ← achieved during M1
- [x] `gradle test` — 12/12 passing (9 prior + 3 new M4 `OverlayGeometry` tests; the cloud session
  has no Android SDK so CI runs the real `gradle test`/`assembleDebug` on push — see
  `.github/workflows/build.yml`)

### M4 — floating QUARK trigger (this session)
- [x] **Step 0 — permission walkthrough:** `LauncherActivity` checks `Settings.canDrawOverlays`,
  re-checks on `ON_RESUME` (grant happens outside the app, then return — no restart). Ungranted →
  the HOME `QUARK TRIGGER // GRANT OVERLAY ►` control fires
  `Intent(ACTION_MANAGE_OVERLAY_PERMISSION, package:…)`. Granted → the control reads
  `QUARK TRIGGER // DEPLOYED` and the service is (re)started. No runtime-permission dialog exists for
  this capability — the Settings screen is the only path. **No other permission added/requested.**
- [x] **Step 1 — the overlay:** `QuarkTriggerService` (foreground, `specialUse` FGS type) adds a
  `TYPE_APPLICATION_OVERLAY` view via `WindowManager` — Service-owned, NOT Activity-scoped, so it
  survives switching apps. App-icon-sized (52dp ≈ an APPS-grid icon, not larger). Visual = a simple
  phosphor "iris" (ring + dim disc + bright aperture) on the CRT ground, **static at rest** (no idle
  animation). Minimal ongoing FGS notification.
- [x] **Step 2 — draggable + edge-snap:** 1:1 real-time follow while dragging (the one place
  "stepped" motion doesn't apply); on release it snaps to the nearest edge via the unit-tested
  `OverlayGeometry.nearestEdgeX`, settled in a quick **stepped** run (6 × 12ms), not an elastic ease.
  Default park = right edge, mid-height (`OverlayGeometry.defaultPark`).
- [x] **Step 3 — tap (M4/M5 boundary):** tap → reused `PleaseStandbyCard` beat (extracted from the
  M3 Lock overlay, now public/shared — not rebuilt) → `QuarkStubActivity`, a full-screen phosphor
  placeholder reading `QUARK / ASSISTANT VIEW` + the one line `MODULE PENDING // M5` +
  `◄ TAP TO RETURN, OPERATOR`. Tap or Back `finish()`es back to whatever app was underneath. **No**
  reactive states / conversation log / command rail / text entry — that's M5.
- [x] **Core logic:** `OverlayGeometry` (edge-snap + default park) lives in `com.quantumos.core`
  (no Android deps), unit-tested — single source of truth, same pattern as the M3 Beacon rule.
- [ ] **M4 confirmed on Fold 6** — grant the overlay toggle; the iris appears (phosphor, static);
  drag it and it settles to an edge; tap plays PLEASE STANDBY → placeholder, Back returns; **open a
  real other app (Settings / an APPS-grid app) and confirm the iris still hovers, drags, and taps
  there** (the headline check).  ← **Director action**

### M3 — Vitality panel (previous session)
- [x] **Step 0 — atom mark + roll-down:** ⚛ atom mark added to the **HOME channel header only**
  (per the scope boundary — NOT on APPS/STATUS/LOG). Static at rest; one stepped quarter-turn spin
  on open. Panel rolls down with **stepped** motion (discrete step count, not a smooth slide).
  **Two close methods built:** tap the atom mark again, or the `▲ STOW` handle on the open panel
  (Back also stows it first).
- [x] **Step 1 — Zone 1 vitals (read-only):** reuses `engine.masterState.vitality` (no second data
  path). Readiness renders as `NN% WORD`; **CRITICAL is the one warn-red here**. New in-house
  `SegmentedGauge` (phosphor segments, no Material `LinearProgressIndicator`) drives **Signal**,
  **Power**, **Core Temp**. Core Temp = battery `EXTRA_TEMPERATURE` (same M2 receiver). Signal =
  coarse transport tier (wifi=4, cellular=2, offline=0) — no precise-dBm permission. Uptime ticks
  every 1s **only while the panel is open** (no idle redraw at rest).
- [x] **Step 2 — Zone 2 four actions (Stealth · Phosphor · Beacon · Lock, decision-36 order):**
  - **Phosphor** — cycles hue green→amber→cyan→green via the existing env mechanism (no 2nd path).
  - **Stealth** — hard-dims **this window's** `screenBrightness` (no permission, reversible);
    saturation untouched (brightness only). SFX-mute is wired as the `isStealthMode` flag (see note).
  - **Beacon** — real torch via `CameraManager.setTorchMode` (no permission) + a blinking warn-red
    `⚑ BEACON` field-flag on HOME. **Turning Beacon ON force-drops Stealth** (rule in core, tested).
  - **Lock** — calls the existing `executeCosmeticLockSequence()`; a new `LockOverlay` plays the
    PLEASE STANDBY → DEVICE SECURED beat and **tap-to-unseal** calls `unlockDeviceProfile()`.
    Cosmetic only — no Device Admin / `lockNow()`.
- [ ] **M3 confirmed on Fold 6** — atom roll-down reads stepped (not smooth); gauges show real
  numbers and move on plug/unplug + Wi-Fi toggle; Stealth dims without washing out the phosphor and
  is reversible; Beacon lights the torch + red flag and drops Stealth; Lock plays the securing beat
  and unseals.  ← **Director action**
- [x] **M2 Step 0:** container is fill-and-adapt (`forceFixedContainer=false`); APPS grid is
  `GridCells.Adaptive(minSize = 88.dp)` (column count follows screen width) — *Director to judge on Fold*
- [x] **M2 Step 1:** STATUS channel wired to real vitals via `engine.incomingTelemetryUpdate(...)`
  — battery %/charging + battery temp (ACTION_BATTERY_CHANGED sticky), uptime
  (`SystemClock.elapsedRealtime`, HH:MM:SS), connectivity (ConnectivityManager: connected + transport)
- [x] **M2 Step 2:** LOG channel renders `engine.systemLogs` in a `LazyColumn` (last 100, most-recent
  auto-scrolled into view), console-reel style, no Material chrome
- [x] **M2 Step 3:** all four channels (HOME/APPS/STATUS/LOG) reachable via the existing
  `ChannelStrip` → `transitionNavigation(...)`; back routes any channel → HOME
- [x] `runDevSimulation()` harness removed — real telemetry replaces it
- [ ] **M2 confirmed on Fold 6** (battery moves on plug/unplug, uptime counts, link reflects Wi-Fi/
  cellular; surface fills the unfolded display; grid shows more columns unfolded)  ← Director action
- [ ] M0/hue confirmed on Fold 6 (phosphor screen + hue switch live)  ← Director action
- [x] **Chakra Petch actually bundled (M6 Step 1)** — real `res/font/chakra_petch_*.ttf`, wired via
  `Fonts.ChakraPetch`; `FontFamily.Monospace` retired. Monoton bundled for the boot wordmark only.
  (The old `ui-text-google-fonts` Downloadable-Fonts path + `font_certs.xml` stub are now unused —
  we bundle the .ttf so it renders identically offline; left in place, harmless, can be removed.)

> **Note on the green build:** the project had NEVER actually compiled before M1, despite M0
> being described as done. Two latent blockers were masking each other: (1) `settings.gradle.kts`
> had no `pluginManagement` repos so AGP could not resolve, and (2) there was no `res/` directory
> at all, yet the manifest referenced `@string/app_name` and `@style/Theme.QuantumOS`. Both are
> now fixed (repos declared; `res/values/{strings,colors,themes}.xml` created). The Kotlin sources
> compiled clean once it reached that stage.
- [x] HOME intent-filter added (M1 Step 1) — will offer QuantumOS as Home picker option on install
- [x] `<queries>` package-visibility block added (M1 Step 2)
- [x] APPS channel: queries real installed apps, renders grid, tap-to-launch (M1 Step 3)
- [x] HOME ⇄ APPS navigation wired via `transitionNavigation` (M1 Step 4)
- [x] App icon added (adaptive phosphor-Q) + `android:icon` wired — required for Samsung install
- [x] **M1 confirmed on Fold 6 (2026-06-23)** — installs, runs, lists real apps, launches them

## Rollback confirmed (Step 0 — M1 Task Brief)
**Director: before setting QuantumOS as default Home, verify the return path:**
> Settings → Apps → Default apps → Home app → select the stock/other launcher.
>
> This is a manual step on the device. Installing this build only makes Android *offer*
> QuantumOS as a Home option (via the picker). It does NOT silently take over. Nothing changes
> until the Director explicitly switches in the system prompt or Settings.

HOME category was confirmed NOT declared before M1 work began (manifest verified clean).

## Known issues / TODOs
- **M6 sound is synthesised, not mastered (by design):** `SoundEngine` procedurally synthesises every
  cue via `AudioTrack` (the spirit of the prototype Web-Audio synth) — NOT professionally produced
  audio files. That's a future identity/polish refinement, explicitly out of M6 scope. Tune the synth
  recipes if the Director wants a different character.
- **`buzz_denied` (access-denied) is synthesised but not yet emitted anywhere** — there is no
  "access-denied" flow in the app today (no feature refuses an action). The cue is in the bank ready;
  wire it the moment a real denial path exists. The other three signature sounds DO fire (boot sweep,
  access-granted via Stealth confirm, keypad tick on boot steps + text send).
- **M6 CRT shader is judged on hardware for the first time** — the AGSL `RuntimeShader` look (scanline
  /vignette/glow intensities) is tuned by eye in code; the Fold 6 pass is the real judgement. If it
  reads wrong, tune the constants in `CRT_AGSL_SHADER` (LauncherUi.kt). The cheap overlay remains the
  automatic fallback if the shader ever fails to compile.
- **M5/earlier:** audio cues were tokens-only with no player; M6 added the player + Stealth mute gate.
  Crisis and harbor still correctly emit **no** token at all.
- **M5 `{limiter}` slot deferred (per the library §1 director note):** status DEGRADED/CRITICAL use
  the library's no-limiter variants, so those two bands have a single variant (no back-to-back
  rotation needed — they only fire on a degraded device). NOMINAL has its full 3-variant rotation.
  Computing the limiter is cheap and derived (no new sensor) if Clara/Director later wants it.
- **M5 boot "Online" line not wired (nice-to-have, brief Step 6):** the §6 Online line was left for
  M6's boot-sequence polish to avoid firing it before telemetry has a first reading. The open/stow
  session lines ARE wired.
- **M5 typed "lock" triggers the real cosmetic-lock beat** (reuses M3 `executeCosmeticLockSequence`)
  in addition to speaking the LOCK line, so the line is truthful. The DEVICE SECURED overlay then
  shows on the launcher beneath; stow the assistant to reach it. Lock is intentionally NOT one of the
  six rail buttons (the rail is the locked six: Status/Stealth/Phosphor/Beacon/Say/Warn).
- **M5 Stealth carryover (brief Step 7) — fixed:** the assistant is a separate Activity, so the
  window-level `screenBrightness` dim does NOT inherit across windows. `QuarkAssistantActivity`
  re-applies it from the shared engine state in a `LaunchedEffect(isStealthMode)`, the same way the
  launcher does. Phosphor hue carries over automatically (both read the one `QuantumRuntime` engine).
- **M5 engine is now a process singleton (`QuantumRuntime`):** promoted out of the ViewModel so the
  launcher and the assistant Activity share one engine/parser/telemetry. Telemetry runs on an
  app-scoped coroutine (not the ViewModel scope). Watch on-device that it isn't double-started or
  leaking — both entry points guard with idempotent flags.
- **M3 Stealth SFX-mute is a wired flag, not an audible change yet:** the app has no audio *player*
  yet (the engine only emits `audioCueStream` string tokens; playback lands in M6). Stealth sets
  `isStealthMode`, which the future player must check before sounding a cue. Honest stand-in, not a
  silent drop — flagged here so M6 honours it.
- **M3 Beacon/Stealth — watch on the Fold 6 (common foldable quirks, do NOT silently work around):**
  (1) `setTorchMode` can throw `CameraAccessException` if the flash camera is momentarily claimed or
  re-enumerated across a fold/unfold — calls are `runCatching`-wrapped and `onDispose` force-kills
  the torch, so we fail dark rather than crash or strand the light on; (2) `screenBrightness` override
  behaviour can differ on the inner vs cover display. Both are Director on-device checks.
- **M3 Core Temp gauge** maps the battery-temp stand-in across a 25–50°C field range onto 10
  segments — same locked stand-in as STATUS until kiosk/ROM grants true SoC thermal (spec §7.3).
- **M3 atom mark / flag are glyphs (`⚛`/`⚑`)**, matching the existing line-glyph working set
  (`◈`, `⊕`, `▲`); per-app SVG masters arrive at the later identity/polish stage.
- **M3 Vitality panel is HOME-channel-only by design** (scope boundary) — the "flick from anywhere"
  system-wide shade is deferred to kiosk mode (Bible decision 56). Not added to APPS/STATUS/LOG.
- ~~Typography: replace `FontFamily.Monospace` with Chakra Petch~~ — **done M6** (bundled .ttf).
- ~~CRT: current overlay is the cheap non-shader fallback; real AGSL shader is M6 polish.~~ — **done
  M6** (real AGSL `RuntimeShader`; the cheap overlay is now the automatic fallback, not the default).
- **STATUS — connectivity is coarse by design (M2 hard stop):** connected/not + transport label
  only. No precise signal-strength bars (would need `READ_PHONE_STATE`) — deferred, not dropped.
  The engine's readiness composite uses a coarse signal proxy (connected→3, offline→0).
- **STATUS — storage breakdown not shown:** the M2 brief mentioned storage; this pass implements
  battery/uptime/connectivity/temp. Storage % is deferred as a known item (no blocker; `StatFs`/
  `StorageManager` can add it later) — not silently dropped.
- STATUS — `coreTempCelsius` is sourced from **battery** temperature (no-permission, real) as a
  stand-in for a true SoC thermal reading; revisit if a better no-permission source is wanted.
- Telemetry polls every 3s on the ViewModel scope (functional reactive, not an idle redraw loop);
  it runs app-wide so HOME's readout is live too.
- App icons (in the grid): loaded from PackageManager via Drawable→Bitmap conversion. No custom
  icon styling yet; icons render at system defaults. Deferred to polish milestone.
- **Samsung install gotcha (resolved):** One UI's package installer silently aborts if the APK
  has no `android:icon` (now fixed) AND can choke on odd download filenames — keep the artifact
  named `app-debug.apk`.
- Empty states: APPS grid shows "SCANNING PACKAGE REGISTRY…"; LOG shows "LOG REGISTER EMPTY".
- Action rail (bottom chrome strip) is not yet rendered — deferred (M3 delivered the Vitality
  panel; the App-Shell action-rail remains a later chrome item).
- `loadApps` is called once at launch; no refresh on app install/uninstall (later concern).

## Decisions pending (Director / Clara — do not lock in code)
- **Container fill-and-adapt** (M2 Step 0): now `forceFixedContainer=false` — surface fills the real
  screen, CRT falloff frames it. Director to confirm on the Fold this reads right vs the old letterbox.
- **APPS grid column count**: now `GridCells.Adaptive(minSize = 88.dp)` per the M2 brief's 88–96dp
  target. On the Fold 6 inner display this yields more columns than narrow; Director to judge the
  cell size / column count on-device.

## Session history
- **M0 session:** Design-system foundation — phosphor screen, hue switch, QuantumState engine,
  4 unit tests, cheap non-shader CRT overlay. Font placeholder in place.
- **M1 session (2026-06-23):** HOME intent declared, `<queries>` added, App Shell chrome
  (nameplate + channel strip), APPS grid wired to real PackageManager, HOME ⇄ APPS nav,
  back gesture routing. All Steps 0–5 complete. Also unblocked the build for the first time:
  fixed `settings.gradle.kts` plugin repos, created the missing `res/values/*` (strings, colors,
  CRT-ground theme), and repaired the CI workflow (valid YAML + Gradle wrapper). Added adaptive
  app icon to fix Samsung's silent install rejection. **Confirmed on Fold 6 by the Director** —
  M1 closed. Tracked in PR #1.
- **Merge/M2 session (2026-06-23):** consolidated the three divergent branches into `main` (M1
  code + docs/skill/font infra + task briefs), resolving duplicates to the verified M1 versions.
  Then built M2: STATUS channel on real telemetry (battery/charging/temp via the
  ACTION_BATTERY_CHANGED sticky, uptime via `SystemClock.elapsedRealtime`, connectivity via
  `ConnectivityManager` — all feeding the existing `incomingTelemetryUpdate(...)` seam, with a
  UI-only transport label); LOG channel as a live `LazyColumn` console reel off `systemLogs`;
  Step 0 container/grid resolved (fill-and-adapt + adaptive 88dp grid); removed the dev-sim
  harness; added `ACCESS_NETWORK_STATE` (install-time, no prompt). Builds via CI (no local Android
  SDK in the cloud session). **Pending Director confirmation on the Fold 6** before M2 is closed.
- **M3 session (2026-06-23):** Vitality panel. Core (`QuantumState.kt`): added
  `VitalityState.readinessPercent` (composite headline) and three engine actions —
  `cyclePhosphorHue()`, `toggleStealthMode()`, `toggleBeacon()` (the latter carries the
  Beacon-drops-Stealth rule) — all reusing the single env mechanism; +5 unit tests (9/9 pass).
  UI (`LauncherUi.kt`): ⚛ atom mark on HOME with a stepped roll-down `VitalityPanel` (Zone 1
  Readiness + `SegmentedGauge` Signal/Power/Temp + 1s-ticking Uptime; Zone 2 Stealth·Phosphor·
  Beacon·Lock), the blinking `⚑ BEACON` flag, and a `LockOverlay` for the cosmetic securing beat.
  Platform side-effects wired in the Activity: Stealth → this-window `screenBrightness`; Beacon →
  `CameraManager.setTorchMode` (runCatching-wrapped, torch killed onDispose). Panel open/stow state
  held in the ViewModel (survives fold/rotate); signal upgraded to a coarse transport tier. No new
  permissions. Core verified locally in a JVM harness; full APK build is on CI. **Pending Director
  confirmation on the Fold 6** before M3 is closed.
- **M5 session (2026-06-24):** QUARK Assistant View. Core (`QuantumState.kt`): replaced the
  placeholder `ScriptedLineLibrary` with the verbatim Scripted-Line Library v1.1 (all six rail
  intents, the §4 keyword categories, §6 open/stow lines), each with its 2-3 rotating variants +
  reactive posture + sound cue; per-(intent,mode) "don't repeat the last variant" rotation. Rewrote
  `QuarkParser` to classify typed input (Distress matched narrowly + priority-first, then
  most-specific) and route everything through one `engine.quarkSay` speak beat that runs the
  Scan→result thinking beat and records a distinct `conversationLog` (separate from `systemLogs`).
  Added `OperatorConfig` (operator name + crisis-resource string, both empty by default),
  `ConversationEntry`, caption + crisis flag on `QuarkBrainState`, and `effectiveCrisisResource()`
  (safe generic fallback). Crisis + harbor are Idle-only, no sound, never Warn; crisis flags the
  resource line. +9 unit tests (18/18). UI: new `QuantumRuntime` process-singleton holding the one
  engine/parser/telemetry so the launcher + assistant share state; `QuantumViewModel` slimmed to
  delegate. New `QuarkAssistantActivity` (replaces `QuarkStubActivity`) — central `QuarkPresence`
  with the four stepped reactive states, state caption, scrolling conversation log with the crisis
  resource box, six-action command rail (4 reused M3 actions + Say/Warn), and a phosphor `BasicTextField`
  entry. Stealth re-applied per-window (Step 7); hue carries over via shared state. Manifest + service
  point at the new Activity. Built on CI (no local Android SDK). **Pending Director confirmation on
  the Fold 6** + the crisis-resource string decision before M5 is closed.
- **M6 session (2026-06-24):** Splash, sound, real fonts/shaders, persistent settings + the Deployment
  Region patch. Core (`QuantumState.kt`): `DeploymentRegion` enum + verified `DeploymentRegions`
  preset blocks; `deploymentRegion` + `bootPace` promoted into `QuantumLauncherState`;
  `setBootPace`/`cycleBootPace`, `setDeploymentRegion`/`cycleDeploymentRegion`; region-aware
  `effectiveCrisisResource()`; a `SoundCue` token registry; boot sequence now emits power-on/relay-
  tick/standby cues; new `ONLINE` + `REGION` library intents and `speakOnline`/`speakRegionSwitched`
  parser beats; action cues (phosphor/stealth/beacon) moved to fire from the engine action.
  UI: bundled `Fonts` (Chakra Petch system face + Monoton boot-only) replacing `FontFamily.Monospace`;
  `SettingsStore` (SharedPreferences) loaded by `QuantumRuntime` before cold boot so Boot Pace +
  Region survive a restart; `SoundEngine` (procedural `AudioTrack` synthesis) wired to `audioCueStream`
  with a Stealth mute gate; a full-screen `BootSplash` ceremony; real AGSL `crtShader()` (with the
  cheap `crtOverlay()` as the automatic fallback); STATUS `CONFIG` tap-to-cycle rows for Region + Boot
  Pace; a HOME `DEPLOYMENT:` status line. +7 core unit tests (region cycle, boot pace default/apply,
  online + region lines; the crisis-resource test updated to the region-aware behaviour). Built on CI
  (no local Android SDK). **Pending Director confirmation on the Fold 6** — especially the CRT shader
  look — and sign-off on the new Deployment Region §4 acknowledgement lines, before M6 is closed.
- **M7 session (2026-06-29):** Signed APK + Checkpoint β prep. Generated `app/release.keystore`
  (RSA 2048, alias `quantumos-release`, 10 000-day validity); wired `app/build.gradle.kts` with a
  `signingConfigs.release` block reading from gitignored `keystore.properties` (graceful fallback
  when absent); bumped `versionName` → `0.1-beta`. Updated CI workflow
  (`.github/workflows/build.yml`) with conditional release-APK steps gated on three repo secrets
  (`KEYSTORE_BASE64`, `KEYSTORE_STORE_PASSWORD`, `KEYSTORE_KEY_PASSWORD`). **Director must: (1)
  back up the keystore base64 + password from the session report before the container is reclaimed;
  (2) add the three secrets in GitHub repo settings; (3) re-run CI to produce the signed release
  APK artifact; (4) complete the Step 3 field-test on the Fold 6.** Checkpoint β is confirmed when
  all field-test items pass.
