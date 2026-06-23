# QuantumOS — Launcher Build Spec
*The translation brief that turns the Tree 1 prototype into a real, installable Android launcher. Companion to the Build Bible (v0.16). Version 1.1.*

> **Purpose.** This is the hand-off document. The Build Bible says *what QuantumOS is and why*; this spec says *exactly what to build first, in what order, and where the honest walls are* — written so Claude Code can execute it milestone by milestone, and so the on-device build is translation, not invention.
>
> **Scope.** This covers **Tree 1.5 — the Launcher App** (critical-path step 3, leads to **Checkpoint β**). It is the first move off free in-Claude prototyping onto real Android, built and tested **on the Galaxy Z Fold 6** (the dev/test bed, decision 49). Final-UI tuning waits for the real Pixel (foldable geometry); this stage proves the build works and feels right.
>
> **Nothing here re-litigates a locked decision.** Every surface below is a *translation* of a Bible decision into Android terms. Where the platform forces a compromise, it's flagged plainly with the fallback and where the full version returns.
>
> **v1.1 — the three launcher-stage scope calls in §7 are now LOCKED** by the director (Lock cosmetic-now · Vitality home-only · Core Temp battery-temp). The spec body already assumed them; §7 is updated to read as confirmed.

---

## 0. The platform envelope — what a launcher can and can't do

A "launcher" is just the home-screen app. On **stock Android, locked bootloader, no root** (decision 47), it is granted a real but bounded set of powers. Knowing the wall lines up front is what keeps scope honest.

**A launcher app CAN (all in scope for this build):**
- Be the home screen — replace the stock launcher entirely (you press Home, you get QuantumOS).
- Render its own surfaces with total freedom — our CRT falloff, phosphor glow, scanlines, stepped motion. *Our* screens are *ours*.
- List and launch every installed app (the APPS grid).
- Float a window over other apps — the persistent QUARK trigger ("draw over other apps").
- Read real device vitals — battery, uptime, connectivity, storage.
- Use the flashlight (Beacon), play our synthesized sounds, switch phosphor hue live.

**A launcher app CANNOT (deferred — see §6):**
- Replace Android's real **notification shade** → so the Vitality panel rolls down *inside our home screen* for now.
- Replace the **lock screen / keypad** (security-critical framework surgery) → the real first-boot PIN stays Android's; our designed lock screen stays a prototype until kiosk/ROM.
- Play a true **boot animation** → we mask it with a launcher-startup splash instead.
- Deeply theme **stock system surfaces** (the status bar, system dialogs) → kiosk mode makes this moot by sealing them away later.

**The key reassurance:** almost everything on that "cannot" list comes back **for free** in the very next step — **Field Ops Mode / kiosk (Tree 1.75)** — without a ROM. This build doesn't fight those walls; it builds right up to them and stops.

---

## 1. The recommended tech stack (the foundation decision)

| Choice | Recommendation | Why |
|---|---|---|
| **Language / UI** | **Kotlin + Jetpack Compose** (native) | A launcher needs deep platform hooks (HOME intent, app listing, overlay windows). Compose gives full custom rendering for the CRT/phosphor look via GPU shaders. Native performance suits a "field tool" that must feel responsive even when motion is *deliberately* stepped. **Same code folds into the optional ROM later as a system app — zero rewrite.** |
| **Rejected: WebView wrapper** | No (except as a throwaway demo) | Wrapping our HTML prototype stands up fast but feels laggy, can't do the floating trigger or launcher integration cleanly, and won't read as native. Look-and-feel is priority #1 — this loses on the one axis we care about most. |
| **Rejected: Flutter / React Native** | No | Cross-platform value is zero (we're Android-only, Pixel-bound) and both fight the launcher/overlay/system APIs we depend on. |
| **CRT effects** | **AGSL shaders (`RuntimeShader`) + `RenderEffect`** | The phosphor glow, scanlines, vignette and flicker are GPU effects, not images. Cheap on the Pixel/Fold's hardware; tunable live. |
| **minSdk / target** | **minSdk 33 (Android 13), target latest** | Both the Fold 6 and the Pixel comfortably exceed 33, and 33 unlocks the AGSL shaders the CRT look needs. Compose handles the rest. |
| **Fonts** | Bundle **Chakra Petch** (Monofonto web substitute, decision 9); **Monoton** for the wordmark accent only | Exactly mirrors the locked typography. The real Monofonto file slots in later with no structural change. |

**Foundation decision — LOCKED (decision 55):** native Kotlin + Compose. Everything below assumes it.

---

## 2. Component inventory — prototype → Android

Each prototype surface, mapped to its real implementation, with scope and any risk flag.

| # | Prototype surface | Android implementation | Scope | Flag |
|---|---|---|---|---|
| 1 | **Home + channel selector** (HOME/APPS/STATUS/LOG) | Single-Activity Compose app; declares the HOME intent; channels are destinations | ✅ In | — |
| 2 | **APPS grid** | Enumerate installed apps; launch on tap; phosphor/CRT styling | ✅ In | Needs `QUERY_ALL_PACKAGES` |
| 3 | **STATUS channel** | Reads battery/uptime/**storage** (storage lives here now, decision 35) + device info | ✅ In | — |
| 4 | **LOG channel** | Styled event log of in-app events | ✅ In | — |
| 5 | **Vitality panel** | In-launcher roll-down from the atom mark on our home surface | ✅ In (home-only) | "From anywhere" deferred to kiosk |
| 5a | — Readiness (composite) | Derived in-app from power/signal/temp (not a sensor) | ✅ In | — |
| 5b | — Power · Uptime | `BatteryManager` · `SystemClock` | ✅ In | — |
| 5c | — Signal | Connectivity/signal APIs | ⚠️ Partial | Full signal strength can need a permission; approximate first |
| 5d | — Core Temp | Battery temperature as the readable stand-in | ✅ In (stand-in) | True SoC thermal needs system privileges → kiosk/ROM |
| 5e | — Phosphor switch | Recolor our own theme live | ✅ In | Trivial — it's our app |
| 5f | — Stealth | Dimming overlay + mute | ✅ In | Real hardware backlight drop arrives with system privileges |
| 5g | — Beacon | Torch via `CameraManager.setTorchMode` + warn flag | ✅ In | No CAMERA permission needed for torch |
| 5h | — Lock | Cosmetic "securing" beat; real `lockNow()` deferred to kiosk | ✅ In (cosmetic) | Real lock arrives free in device-owner mode — Device Admin *not* grabbed now |
| 6 | **Floating QUARK trigger** | Overlay window (`SYSTEM_ALERT_WINDOW`), app-icon sized, static, draggable | ✅ In | User grants overlay permission once in Settings |
| 7 | **QUARK Assistant View** | Full-screen Activity; presence + reactive states + log + command rail + text entry; **scripted brain** | ✅ In | Real LLM = Chat 04, unchanged |
| 8 | **Launcher-startup splash** | A masking entry screen (PLEASE STANDBY / boot-log styling) shown as *our* app starts | ✅ In | Not the true Android boot animation (deferred) |
| 9 | **Lock screen / keypad** | — | ❌ Deferred | Framework surgery; stays prototype-only until kiosk/ROM |

---

## 3. The design system as code (build this first — everything depends on it)

This is Milestone 0. It's the reusable spine; every screen draws from it.

- **Phosphor palette** → theme tokens for the three hues (green default / amber / cyan, with their `-dim` pairs and `--warn` / `--crt`, exactly per Bible §3). A single live "phosphor switch" recolors the whole app.
- **CRT treatment layer** → one reusable overlay (scanlines + edge vignette + subtle flicker + phosphor glow on text/lines) applied app-wide via shader. Tunable so we can dial intensity on real hardware.
- **Stepped-motion specs** → custom animation curves that advance in discrete clicks, *not* smooth interpolation (the "slide-projector" feel, Bible §3). Motion is reactive, never ambient — utility marks and QUARK sit **static at rest** (decision 41).
- **PLEASE STANDBY component** → the universal loading card (replaces all spinners), reused on launch, the QUARK open beat, and the Lock "securing" beat.
- **Typography** → Chakra Petch bundled as the app font; Monoton reserved for the wordmark accent only.
- **Sound stubs** → short synthesized clips matching the locked sound language (decision 44): Boot power-up sweep, relay-tick keypad, two-note granted, harsh-buzz denied, plus QUARK's non-verbal chirps. Placeholders now; final masters at polish.

**Verify Milestone 0:** the app launches to a phosphor CRT screen with our font and a working hue switch. That alone proves the whole aesthetic engine before any feature exists.

---

## 4. The build sequence — Claude Code task list

Ordered so each milestone is a self-contained, testable deliverable. Each ends with an on-device check on the Fold 6.

- **M0 · Design-system foundation** *(§3)* → app shows a phosphor CRT screen, font + hue switch live.
- **M1 · Launcher core** → declare the HOME intent; build the Home channel; set QuantumOS as the default launcher; APPS grid lists and launches real apps. *Verify: it works as a (rough-but-real) home screen.*
- **M2 · STATUS + LOG channels** → wired to real battery/uptime/storage + event log.
- **M3 · Vitality panel** → atom-mark roll-down; Zone 1 vitals (real where possible, battery-temp stand-in for Core Temp); Zone 2 actions (Phosphor, Stealth, Beacon real; Lock cosmetic). *Verify on device.*
- **M4 · Floating QUARK trigger** → overlay window, static + draggable, PLEASE STANDBY beat. *Verify it hovers over other apps.*
- **M5 · QUARK Assistant View** → full-screen activity, all four reactive states, conversation log, command rail wired to real actions (status read, stealth, phosphor, beacon, say/warn), text entry, scripted brain.
- **M6 · Launcher splash + sound + polish** → masking startup splash; sound pass; CRT-shader and stepped-motion tuning.
- **M7 · Ship + field-test** → signed APK, sideload to the Fold 6, live test → **Checkpoint β**.

---

## 5. Permissions — in plain language

What the app asks for, and why. (Most of these *shrink* later: in Field Ops Mode / kiosk, the device grants them automatically.)

- **"Be the home screen"** — the HOME intent. No pop-up; you simply pick QuantumOS as your default launcher.
- **"See your apps"** — to show them in the APPS grid (`QUERY_ALL_PACKAGES`).
- **"Float over other apps"** — for the QUARK trigger. A one-time toggle in Settings (`SYSTEM_ALERT_WINDOW`).
- **"Use the flashlight"** — for Beacon. (Torch needs no special permission.)
- **"Read signal strength"** — for the Signal vital. We approximate first to avoid an intrusive ask.
- **Not requested now:** Device Admin (for a real Lock) — deliberately skipped to keep the launcher's footprint light; the real lock comes free in kiosk.

---

## 6. Explicitly NOT in this build (the scope guard)

So scope can't creep. Each of these has a known home later:
- **True notification-shade Vitality panel** → Field Ops Mode / kiosk (Tree 1.75) or ROM Layer 2.
- **Real lock screen / keypad Keyguard** → kiosk-themed lock now possible later; true surgery is ROM Layer 2 (security-critical, can bootloop — stays a frontier branch).
- **True Android boot animation** → kiosk masking / ROM.
- **Deep theming of stock system surfaces** → made moot by kiosk; ROM Layer 2 for the real thing.
- **QUARK's real LLM brain + spoken voice** → Chat 04, unchanged.
- **Final sound masters; real Monofonto font** → polish stage.

---

## 7. Launcher-stage scope confirmations — LOCKED (decision 56)

The director confirmed all three. They are now part of the spec:

1. **Lock action — cosmetic now, real in kiosk.** ✅ The Lock quick action plays the "securing" beat but does not grab Device Admin; real `lockNow()` arrives free in device-owner mode (Tree 1.75). *Keeps the launcher's permission footprint minimal.*
2. **Vitality panel — home-only roll-down now, global pull-down deferred to kiosk.** ✅ A launcher cannot replace Android's real notification shade; the atom mark lives on our home surface for this build, and the flick-from-anywhere version returns free in kiosk.
3. **Core Temp — battery-temperature stand-in until kiosk/ROM thermal.** ✅ A real, honest reading that keeps the vital live now; true SoC thermal returns once we hold system privileges.

---

*End of Launcher Build Spec v1.1 — a companion to Build Bible v0.16. Update alongside the Bible whenever a decision here changes.*
