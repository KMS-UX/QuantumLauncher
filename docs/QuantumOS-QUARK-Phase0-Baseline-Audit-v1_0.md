# QuantumOS — QUARK Core App: Phase 0 Baseline Audit
*Report v1.0 — Claude Code, 2026-07-13. Answers the Phase 0 Baseline Audit brief (Clara), run
before Google AI Studio starts Phase 1. Read-only pass against the live repo — a findings report,
matching the Optics & Nav Audit Pass discipline, not a code change. Exploratory: Dual Embodiment is
not yet a locked Bible decision; this audit only supplies interface facts for the Integration Spec.*

---

## 0. Purpose

The Integration Spec handed to Google AI Studio was written against the Bible's *description* of
the M5 QUARK Assistant View, not the shipped code, so it carried hedges ("confirm exact enum," "or
similarly named"). This report replaces those hedges with verified facts for the four items below.

---

## 1. The reactive-state broadcast

**Real name:** `QuantumStateEngine.masterState: StateFlow<QuantumLauncherState>` — defined in
`com.quantumos.core.QuantumState.kt` (pure Kotlin, no UI deps). This is **the single source of
truth for everything** (nav channel, vitals, environment, QUARK's posture, operator config) — there
is no separate "QUARK-only" broadcast.

- QUARK's reactive posture lives at `state.quarkBrain.activePosture`, typed `QuarkReflexPosture` —
  **exactly 4 values: `IDLE, SCAN, HAPPY, WARN`** (not more, not renamed).
- `QuarkBrainState` also carries `caption: String` (one-line status text, e.g. `"STANDING BY"`,
  `"SCANNING…"`) and `showCrisisResource: Boolean`.
- **Access pattern (uniform, verified in two places):** every consumer does
  `val state by engine.masterState.collectAsState()` against the **same process-singleton**
  `QuantumRuntime.engine` — `LauncherActivity` via `QuantumViewModel.engine = QuantumRuntime.engine`,
  and `QuarkAssistantActivity` directly. No per-screen duplication.
- **Current line-art QUARK:** a **private** `@Composable QuarkPresence(posture: QuarkReflexPosture,
  color: Color, dimColor: Color)`, defined inline inside `QuarkAssistantActivity.kt` (~line 367). It
  reads posture via a `LaunchedEffect(posture)` that drives short, discrete, stepped bursts
  (hop/shake/scan-sweep), not a continuous loop — Idle does zero redraw.
- **Flag for Phase 1:** `QuarkPresence` is **private** — not exported/importable from another file
  as-is. A parallel holographic renderer can subscribe to the same `state.quarkBrain.activePosture`
  trivially (it's just a `StateFlow` read, no encapsulation barrier), but it cannot literally reuse
  this composable without it being made internal/public, or the new renderer living in the same file.

## 2. The phosphor-hue state

**Real name:** `PhosphorHue` enum `{GREEN, AMBER, CYAN}` (core), stored at `state.environment.activeHue`
(`EnvironmentProfile.activeHue`).

- **Color mapping — the one token source:** `object Phosphor` in `com.quantumos.shell.ui.LauncherUi.kt`
  (line 116), with `Phosphor.bright(hue)` / `Phosphor.dim(hue)` plus `Phosphor.Warn` and `Phosphor.Crt`.
  Six literal hex constants total, nowhere else.
- **Mutation:** `engine.cyclePhosphorHue()` (GREEN→AMBER→CYAN→GREEN) or
  `updateEnvironmentProfile { it.copy(activeHue = ...) }` for a directly-named hue (typed
  "amber"/"cyan"/"green").
- **Live retinting:** every surface just re-derives color from the shared state each recomposition —
  `LauncherActivity`, `QuarkAssistantActivity`, and even the **out-of-app floating overlay**
  (`QuarkTriggerService`) re-tint via `LaunchedEffect(canOverlay, state.environment.activeHue)`
  redeploying the overlay with `Phosphor.bright(hue).toArgb()`. No caching, no stale color anywhere.

## 3. The Stealth-mode mechanism

**Real name:** `state.environment.isStealthMode: Boolean` (`EnvironmentProfile.isStealthMode`).
Toggled via `engine.toggleStealthMode()`.

Three independent effects, all gated on the same flag, each wired at its own consumption point (no
central "mute everything" switch):

- **Screen:** hard-dimmed to `screenBrightness = 0.04f` via `window.attributes`, applied
  **per-window** — separately in both `LauncherActivity` and `QuarkAssistantActivity` (each has its
  own `LaunchedEffect(state.environment.isStealthMode)`), because window attributes don't propagate
  across Activities.
- **SFX:** `SoundEngine.play(token, stealth)` mutes every cue **except** `STEALTH_DOWN`/`STEALTH_UP`
  themselves (the transition sound must be heard). The stealth boolean is read fresh at each call
  site (`QuantumRuntime.playCue`, the audio-cue-stream observer) from
  `engine.masterState.value.environment.isStealthMode`.
- **Voice (Phase 2, new this week):** `QuantumRuntime`'s `startVoiceObserver()` checks the same flag
  and skips speaking entirely when Stealth is on — falls back to text-only, matching the SFX rule.

**Assistant View's own reaction:** re-applies the window dim (Step 7 in the code comments —
"Stealth's window brightness is per-window, so it is re-applied here") and inherits the SFX/voice
mute for free since it drives the same engine/sound/voice singletons.

## 4. The M5 Assistant View — location and shape

**File:** `app/src/main/java/com/quantumos/shell/overlay/QuarkAssistantActivity.kt` (729 lines).
Class `QuarkAssistantActivity : ComponentActivity()`.

**Reachability — single door:** the M4 floating overlay (`QuarkTriggerService`, a system-wide
draggable `TYPE_APPLICATION_OVERLAY` window that persists over every app) launches it via
`Intent(this, QuarkAssistantActivity::class.java)` on tap. **No nav-channel button reaches it** —
HOME/APPS/STATUS/LOG don't host a QUARK entry point themselves.

**Composable shape**, one `setContent` block:
1. Header row: `◄ STOW` / `QUARK` title (triple-tap → debug mode) / state caption.
   - Debug-only lines beneath the title: `// BRAIN: ON-DEVICE`, `// VOICE: ON|OFF`,
     `// VOICE-ID: PLACEHOLDER|QUARK-H2`, `// [IMPORT VOICE MODEL]`.
2. `QuarkPresence` (the reactive mark) in a fixed `140.dp` box.
3. Body — conditional: `ModelAcquisitionPanel` (debug + brain not loaded) **or** `ConversationLog` +
   `CommandRail` (6 fixed actions) + `FreeTextEntry`.

**Shared state, explicitly by design** (its own doc comment says so): reads/mutates the *same*
`QuantumStateEngine` as the launcher — phosphor hue and Stealth carry over automatically; it is not
a parallel state tree.

## 5. Things in flux — flagged explicitly, not left implicit

- **Two parallel "brains" behind one debug gate, funneled into one pipe.** `QuarkParser` (the
  scripted, shipping brain) and `QuarkOnDeviceBrain` (on-device Gemma LLM, debug-only) both
  ultimately call `engine.quarkSay(...)` — same log, same reactive state. `FreeTextEntry`'s callback
  branches explicitly on `if (debugMode && brainLoaded)` to pick which one answers a given message.
  **A new renderer needs to know this branch exists** — which system is "live" varies by debug
  state, not by anything visible in `QuarkBrainState` itself.
- **No dedicated "speaking" state.** Phase 2 (voice, landed this week) has no fifth posture for
  "audio is playing" — it just holds whatever posture the text reply already carries (Happy/Idle/Warn)
  for the duration of playback, then dispatches a synthetic `"VOICE_DONE"` reflex back to `IDLE` on
  completion (`QuantumRuntime.startVoiceObserver()`'s `onDone` callback). If Phase 1/2's holographic
  work wants lip-sync timing hooks, **this `onDone` callback is the only start/end-of-speech signal
  that exists today** — there's no `isSpeaking` field on `QuarkBrainState`.
- **Voice itself is young and still moving.** Three live fixes landed on it in the days immediately
  before this audit (a crash, silent playback, latency tuning) — functionally solid now and
  confirmed on-device, but it's the newest, least-settled part of the state graph, worth treating as
  still-warm rather than bedrock.
- **Dual Embodiment is explicitly not a locked Bible decision** (per the audit brief's own framing) —
  this audit found no code artifact already assuming or reserving space for a second renderer;
  "add, don't replace" will be a genuinely new consumer of `masterState`, not a slot that's
  half-built for it already.

---

## Done when (per the brief)

- [x] Manifest covers all four §1 items with real names/locations/behavior.
- [x] Prototype instability relevant to Phase 1 flagged explicitly (§5 above).
- [ ] Handed back to Clara to fold into the Integration Spec — no further action needed from Claude
      Code until Phase 2 (the comparative review, once AI Studio's branch exists).

*End of Phase 0 Baseline Audit v1.0.*
