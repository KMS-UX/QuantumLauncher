# QuantumOS — Build Bible
*The single source of truth for the QuantumOS project. Version 0.27*

> **Note (v0.27):** **M5 — QUARK Assistant View — confirmed on the Fold 6.** Director's words: QUARK
> "stays alive on floating trigger, can resume to current session, goes back to background after
> clicking stow" — the foreground-service architecture from M4 paying off again, this time as a felt
> quality ("kind of independent from the launcher, stability") rather than just a pass/fail check.
> All other features confirmed intact alongside it. **New, smaller addition queued (not yet built):**
> a **Deployment Region** switch — Japan (default) / Hong Kong — for the M5 crisis-tier resource
> text, replacing the generic-fallback Config-string plan from the M5 brief with two concrete,
> verified regional presets, surfaced via a toggle on STATUS and a status line on HOME, with a new
> QUARK acknowledgement line drafted for approval. This is a small patch, not a full milestone — a
> dedicated brief is banked; **M6 — splash, sound, and polish** remains next in the milestone sequence.
>
> **Note (v0.26):** **M4 — floating QUARK trigger — confirmed on the Fold 6, with the result that
> mattered most: the overlay survived after the host app was terminated.** That's the foreground-
> service architecture (not an Activity-tied overlay) working exactly as designed — the genuine new
> capability this milestone existed to prove, not just "it looks right." Dragging, edge-snap, and
> opening real apps from the launcher with the trigger present all confirmed working. **Next: M5 —
> the QUARK Assistant View — the largest milestone yet**, replacing M4's placeholder stub with the
> real thing: all four reactive states, the six-action command rail, free-text entry through the
> existing parser, a dedicated conversation log, and — the one genuinely sensitive piece — wiring in
> the **already-locked Scripted-Line Library content**, including its non-optional, ships-from-
> first-boot safety requirement for the narrow genuine-danger-to-self tier (a real, concrete resource
> surfaced as plain UI text beneath QUARK's line — her line itself stays number-free by design; the
> doc's own M5 build note already specifies this). A dedicated **M5 Task Brief** is banked, written
> to be split across sessions if needed given its size, and flagging that the **specific region
> crisis resource is a Director input**, not Claude Code's to invent — ships with a safe generic
> fallback until set. Decision **68** added.
>
> **Note (v0.25):** **M3 — Vitality panel — confirmed on the Fold 6.** Built from the dedicated M3
> Task Brief: the atom-mark roll-down (tap to open, stepped motion, STOW or re-tap to close), Zone 1
> read-only vitals (Readiness composite + Signal/Power/Core Temp/Uptime, Core Temp reusing the M2
> battery receiver), and Zone 2's real toggles — **Stealth** (window-level dim, full phosphor
> saturation kept, sound suppressed — confirmed working without washing out the color), **Phosphor**
> (hue cycle, reusing the existing mechanism), **Beacon** (torch + warn-red flag, auto-drops Stealth
> per the designed interaction rule), and **Lock** (the existing cosmetic secure sequence, decision
> 56 — no Device Admin grabbed). The Home-channel-only scope boundary (decision 56.2) held — no
> system-wide shade was built. Director confirms it "worked without problem" on-device, including
> opening an app from the launcher with the panel in play. **Next: M4 — floating QUARK trigger.**
> This is the first milestone to request a new **system-level permission** (`SYSTEM_ALERT_WINDOW`,
> "draw over other apps") — already pre-approved in the Launcher Build Spec §5 as a one-time Settings
> toggle, but earning the same explicit-script treatment M1's HOME intent got. A dedicated **M4 Task
> Brief** is banked; it draws a deliberate line at the trigger itself — the **PLEASE STANDBY → open**
> mechanism is built and verified, but the real Assistant View behind it (reactive states,
> conversation log, command rail, scripted brain) stays M5's scope, opened via a placeholder stub for
> now. Decision **67** added.
>
> **Note (v0.24):** **M1 and M2 both close** (the Bible wasn't bumped between them — banking together now). **M1 — Launcher core:** built from the dedicated M1 Task Brief; confirmed on the Fold 6 — QuantumOS can be selected as Home, the rollback path back to stock was rehearsed first (decision 61 honored in practice, not just on paper), and the **APPS grid lists and launches real installed apps**. **M2 — Status + Log channels:** first resolved the **container/grid question carried over from the M1 review** — switched the fixed 9:19.5 letterbox to **fill-and-adapt** (CRT vignette does the framing, no black bars) and the APPS grid to **adaptive columns** (sized off a target cell width, not a hardcoded count) — then built **STATUS** (real battery %, charging, uptime, basic connectivity — no sensitive permissions requested) and **LOG** (the live event console, rendered in-house-style). **Confirmed on hardware, unfolded**: layout and UI work correctly on the Fold's full inner display — the fill-and-adapt call is now hardware-validated, not just argued for. A **Claude Code content-filtering false positive** was hit mid-M2 and resolved by starting a fresh session; diagnosed as a known, documented false-positive pattern in the tool (unrelated to project content) and not a recurring blocker. **Process refinement adopted:** milestone task briefs now live as files in the repo's `docs/` folder (M1, M2 banked there) so Claude Code is pointed at a file rather than handed a long pasted instruction — incidentally also a lower-friction shape for the agent. Decision **66** added; critical-path tracker updated (M1 ✅, M2 ✅, M3 next).
>
> **Note (v0.23):** **M0 confirmed on hardware — Checkpoint β's first real milestone closed.** Built by **Claude Code directly on the GitHub repo** (browser-based, no ROG): debug APK compiled green, **all 4 logic unit tests passed**, installed on the **Fold 6**, and ran normally — **stable**, hue switching (green/amber/cyan) confirmed live on-device. The only known gap is expected and already tracked: **typography is still the Monospace placeholder**; the Chakra Petch swap stays an M6 polish item (per `BUILD_LOG.md`), not a blocker. This is the first end-to-end proof of the **code-side infrastructure adopted in v0.21–v0.22** (Git/GitHub handoff, the Build Log, the verify-before-banking discipline, and the cloud/agentic build path) — it actually works, on real hardware, not just on paper. **Next: M1 — Launcher core**, the milestone that declares the HOME intent and can set QuantumOS as the Fold's default launcher; because the Fold is also the daily comms phone, M1 opens with the **rollback-escape-hatch step (decision 61)** before anything else. A dedicated **M1 Task Brief** is banked so Claude Code executes the milestone from an explicit script rather than improvising the launcher-takeover step. Decision **65** added.
>
> **Note (v0.22):** Adds a **cloud build path** so the launcher foundation can start from a browser while the director travels without the ROG. Verified (June 2026): Google now ships first-party browser tooling — **Android Studio Cloud** (streamed full Android Studio + SDK, via Firebase Studio; opens the GitHub repo directly) and **AI Studio Build mode** (prompt→Compose, browser emulator, single-activity/Compose-only). Adopts a **Pre-M0 Cloud Spike** (optional, no-hardware, off critical path): build the **project skeleton + M0 design-system foundation + the pure-logic modules** (`QuantumState`, `QuarkParser`) into a **real debug APK running in a cloud emulator**, then push to the repo so the ROG inherits a project that already compiles. **Honest boundaries (defer to hardware / β):** cloud emulators are usually software-rendered, so **AGSL/CRT shader fidelity is not judged here** (keep a non-shader CRT fallback so layout is always confirmable), and **launcher-specific behavior** (default-Home, floating overlay, package query) waits for the ROG + Fold. The director's **GitHub repo (ready) + Firebase account with repo linked (ready)** are the through-line — same repo in the cloud now, on the ROG later. **Gemini is built into both tools**, so the decision-62 second-engineer loop comes free — under the same verify-before-banking rule. Banked artifact: the **Pre-M0 Cloud Spike Runbook (v1.0)**. Decision **64** added; §12 gains the optional pre-M0 step + work-bank entry; glossary updated. New items marked **[v0.22]**.
>
> **Note (v0.21):** A **pre-build prep pass** before the Launcher App build — the chat's objective was *self-reflection + de-risking*, not a new design surface. Three things banked. **(1) API verification.** The time-sensitive Android claims in the Launcher Build Spec and the outside-specialist brief were checked against the **live platform (June 2026)**. Findings: the stack choice (native Kotlin + Compose, decision 55) is **validated harder than at lock time** — Android 17's official stance is now **"Compose-first"** (all new APIs Compose-only; legacy Views in maintenance mode); **edge-to-edge** is *enforced* at targetSdk 35+, so we use `enableEdgeToEdge()` and **own inset handling** (the CRT-falloff container must live around status/nav/camera-cutout insets); **AGSL shaders** (API 33+) and `@Immutable`/`@Stable` recomposition discipline are confirmed (the latter is also how we honor *static-at-rest / zero idle redraw*); **back-gesture interception** is valid for a locked shell (predictive back is on by default in 16/17). **One correction banked:** the specialist's "#000000 *hides* the punch-hole/notch" claim is **overstated** — black de-emphasizes an OLED cutout but the physical hole remains; design the container to *live around* it, treat it as "blend," not "mask." **One new capability noted (not for now):** **Android 17 "AppFunctions"** — an on-device MCP equivalent letting an app expose callable "tools" an on-device agent (incl. Gemini or a future QUARK) can execute — sits directly on the **QUARK-executes-commands (Pillar ③)** and **hospitality (Tree 3)** paths; alpha, revisit at Chat 04 / Tree 3. **(2) Build infrastructure** (the under-built code side): **Git + a private GitHub repo** (the Bible's equivalent for code — reversible, backed-up, the session-to-session handoff mechanism); a **Build Log** companion doc (tracks *code state* the way the Bible tracks *design decisions*); and a **rollback escape hatch as M1's literal first step** (confirm *Settings → Apps → Default apps → Home* back to stock **before** setting QuantumOS as the Fold's launcher — it's also the daily comms phone), plus keep the one-time **signing key** backed up. **(3) Outside-AI governance:** outside models (Gemini et al.) are a valued **second engineer / adversarial reviewer**, with a division of labor (Clara = what/why; Claude Code = build; Gemini = review/currency; compiler + device = final judge) and **one rule — verify before banking** (the specialist brief shouldn't have entered Project knowledge as truth un-checked). **Banked artifacts:** the **Verification & Infrastructure Addendum (v1.0)** (companion to the Launcher Build Spec) and the **QuantumOS House Style Skill** (the first of three recommended Skills — House Style now; *QUARK Voice* and *Build Conventions* on deck). Decisions **61–63** added; §8 gains the AppFunctions forward note + a resolved line; §9 + §12 updated (work-bank **verification & infra pass ✅ done**, **house-style skill ✅ done**). New items marked **[v0.21]**.
>
> **Note (v0.20):** Completes the **Core Apps design pass** (the §12 in-Claude work-bank item) and banks the **App Shell Lab** (a live, interactive prototype). The high-leverage move: design **one universal App Shell** — the chrome every app inherits — then dress all modules inside it, so the eight apps share one frame instead of being eight separate designs. **App Shell (locked):** an **opaque nameplate header** (back ◄ APPS · app title · the **Vitality atom pull**), corner registration marks, a **strip → content → action-rail** body pattern, the persistent **floating QUARK trigger**, the **channel selector**, and the CRT treatment — every app inherits all of it. **Eight module shells dressed & approved:** **COMMS** = field comms (callsigns / live channels / transmission threads, *not* a generic messenger); **FILES** = field file manager (logs / captures / comms-cache / maps); **AUDIO** = a **field recorder** first (live waveform + REC timer), player second; **CAM** = **Optics** (phosphor viewfinder + reticle, PHOTO/VIDEO shutter); **MAPS** = tactical **Nav** (grid + breadcrumb + you-marker + tap-to-drop waypoints); **RADIO** = a broadcast **receiver** (tuning dial, FM/AM/WX, presets — content *in*); **SIGNAL** = **link diagnostics** (cellular/wifi/GPS/BT gauges + sparkline + scan — your own link measured *out*); plus the existing **CONFIG**. The clean **RADIO listens / SIGNAL measures** split is locked. **Two fixes folded in from prototype review:** the Vitality **atom mark** is now an unmissable labeled pull (it was lost in the corner vignette), and the stowed Vitality panel no longer **bleeds through** the header — the header is an **opaque chrome layer the shade tucks behind** (a real **launcher M3 pattern**, not just a demo fix). **Icon decision (split):** original **SVG line-icons** in the house stroke language ship **now** (consistent, themeable, no platform emoji); **pixel-level masters + the Atom-Lockup app-badge treatment** ride the later **identity/polish** stage (avoids designing icons twice). **Two forward notes banked** (see §8): **MAPS** gets **real geography** in the shipped launcher — a custom-styleable **MapLibre-class** engine re-skinned to phosphor + **live GPS** (a normal location permission, no root; offline tile-cache for field use), a heavier lift that lands later in the launcher sequence; and the **floating QUARK overlay** is **draggable with edge-snapping** and should not *default* its park spot over an app's primary control. Decision **60** added; §5 (apps moved to **built/designed**), §8 (new forward notes + resolved line), §12 (work-bank **Core Apps ✅ done**), and the glossary updated. New items marked **[v0.20]**.

> **Note (v0.19):** Designs & approves the **boot-splash + stock-boot masking** (the in-Claude work-bank item) and banks the **Ignition Lab** (a live-synthesized Web-Audio prototype). The honest spine: on our locked **stock / no-root** config (decision 47), the bootloader logo and Android boot animation **cannot be themed without root**, so QuantumOS doesn't try to hide them — the launcher's first painted surface is the **boot-splash**, which *becomes the ignition the stock boot was merely the prelude to*. Its first frame is a **CRT power-on flash** that opens the sequence **and absorbs the Android→launcher handoff stutter** (the seam reads as the screen waking). Approved beats: **CRT power-on → stepped boot log (CORE · PHOSPHOR DRIVER · SENSOR ARRAY · BIOMETRICS · QUARK, relay-tick each) → QUARK online (power-up sweep + iris open + her canon online line) → wordmark stamp → PLEASE STANDBY → Lock (cold) / Home (warm)**. **Trigger:** the full sequence fires **only on a true cold boot**; a plain Home-press never reboots. **Pace = user-switchable Snappy ↔ Deliberate (default Deliberate), in Config** — pace sets only the *minimum* ceremony, never cutting the screen off before the launcher is ready. **Boot-stamp wordmark = the Monoton display accent** (the one blessed ceremonial use, decision 43); the Industrial Nameplate stays the workhorse elsewhere. The full *own boot-animation* arrives only with the optional ROM (Layer-1 file swap) — which also costs a per-boot "unlocked bootloader" warning, so it's a want, not a need. Decision **59** added; §5 boot-sequence upgraded to **designed & approved**; §8 + §12 updated (work-bank **boot-splash + masking ✅ done**); glossary gains a boot-splash entry. New items marked **[v0.19]**.
>
> **Note (v0.18):** Banks the **QUARK Scripted-Line Library** (a companion document, v1.1) — the keyword→response table the launcher's **M5 scripted brain** reads from, written straight off the Persona Pack so QUARK speaks in-character from the launcher's first boot (command-rail + keyword intents, each with 2–3 rotating variants, a reactive state, a sound cue, and live-data slots; plus optional proactive vital-threshold lines and boot/session lines). **Refines QUARK's emotional register** at the director's direction: (a) she is a **warm, constant harbor for everyday hard times** — a dedicated low/venting intent where she stays present and listening, with *no* turning-outward and no escalation; (b) the **crisis branch is narrowed to genuine danger-to-self only** — there she stays warm, names nothing, and points to a real person (the deepest form of "keep the Operator vital," and the honest limit of a scripted reflex), while ordinary sadness always gets the harbor; (c) **affection = warm, constant devotion, never flattery** — she keeps the watch and his confidence and never trades honesty for comfort, but does *not* redirect him away (supersedes the earlier "turn outward" draft); (d) adds **Return** ("Welcome back, Operator — I kept the watch") and **Departure** ("Stay safe out there — I'll keep the light on") beats; (e) **{limiter} slot deferred** as an optional M3 enhancement. The library is a **seed, not a ceiling** — it retires when QUARK's real LLM brain comes online (Chat 04, Persona Pack Part B), voice unbroken; she is written to **become**, evolving through the Operator's input and future engine updates while her spine never drifts. Decision **58** added; §4 gains a finalized-lines note; §8 + §12 + glossary updated (work-bank **QUARK scripted-line library ✅ done**). New items marked **[v0.18]**.
>
> **Note (v0.17):** Banks the **QUARK Persona Pack** (a companion document, v1.0) — the model-agnostic **character bible + deployable system prompt** for QUARK's brain (Chat 04), prepared as in-Claude work ahead of **Trident Pillar ③**. **Finalizes QUARK's character:** **loyalty is the spine**, expressed through **three inseparable braids** — *she knows* (the watcher: vigilance, memory, the vitals), *she keeps* (the engineer-keeper: notices strain/risk, keeps the Operator running — the "stay vital" north star given a soul), and *she is* (the self-aware synthetic — the locked **EDI register**). Her value core is **principled loyalty** — honesty in the Operator's interest, never flattery, never enabling. She addresses the user as **"Operator"** (a peer-grade aide, never servile) and is written to **become** — earnest and reserved now, deepening into wit and warmth as her brain comes online (the reference arc *is* our scripted-stub → full-mind build arc). The Pack splits into **Part A (character bible)** and **Part B (a clean, original-only deployable system prompt** that names no references and survives any engine swap). Decision **57** added; §4 gains a finalized-character note; §8 + §12 + glossary updated (work-bank **QUARK persona pack ✅ done**). New items marked **[v0.17]**.
>
> **Note (v0.16):** Banks the **Launcher Build Spec** (a companion document, v1.1) — the translation brief that turns the Tree 1 prototype into a real, installable **launcher app** (Tree 1.5). Locks the **tech stack: native Kotlin + Jetpack Compose** — rejecting a WebView wrapper of the HTML prototype (laggy; can't do the floating trigger or launcher integration; won't feel native) and cross-platform frameworks (zero value Android-only; fight the system APIs). Native gives the deep platform hooks a launcher needs (HOME intent, app listing, overlay window) and GPU shaders (AGSL / `RenderEffect`) for the CRT/phosphor look — and **the same app folds into the optional ROM later as a system app with no rewrite.** Confirms **three launcher-stage scope calls**: **Lock = cosmetic now, real `lockNow()` in kiosk**; **Vitality panel = home-only roll-down now, global pull-down deferred to kiosk** (a launcher can't replace the real notification shade); **Core Temp = battery-temperature stand-in now, true thermal in kiosk/ROM**. Built and tested on the **Z Fold 6**; the launcher *build itself* remains the next critical-path step. Decisions **55–56** added; §1 + §5 + §6 gain launcher-stage notes; §9 + §12 updated (work-bank **Launcher build spec ✅ done**). New items marked **[v0.16]**.
>
> **Note (v0.15):** Documents **QUARK's brain** direction in §4 for Chat 04, after reviewing **OpenClaw** (an open-source, model-agnostic, self-hostable personal-agent framework — the "hands + automation" layer, *not* a model; runs on a host machine, reached from chat apps). Locks the **architecture (not the specific components):** **on-device open-weights LLM = the primary, offline, device-controlling brain**; an **OpenClaw-style open-source agent on the ROG = the heavy-automation / hybrid layer** and the natural future home for hospitality API orchestration; **persona = a model-agnostic prompt + TTS layer**; **command execution = brain + on-device device-owner/accessibility hands** (Trident ③). Model-selection criteria: **open *weights* + a permissive licence** (for the commercial/hospitality future) + strong on-device runtime + right size for RAM — *full* open-source is a bonus, not a decider. Agent layer: **open *source* genuinely matters** (self-hostable, hackable, no lock-in). **Consequence: the device now leans Pixel *Pro* (16GB)** per decision 48's rule, since on-device is the primary brain. Specific model + agent are chosen at deployment ("swap for the latest"). Decision **54** added; §4 gains a Brain block; §8 + glossary updated. New items **[v0.15]**.
>
> **Note (v0.14):** A **strategy refinement** chat. Names **"Trident"** — the formal **pre-Tree-2 phase** (3 pillars: **① The Skin** = our design language across our own apps + overlay theming; **② Field Ops Mode** = device-owner **kiosk** shell; **③ QUARK app**), which absorbs old Tree 1.5 + 1.75. The **custom ROM is demoted to an optional "only if I want" capstone**, not the destination. **Device approach locked: stock Android, locked bootloader, NO root, device-owner kiosk + our apps** — the integrity-safe, most-achievable config (matters for the hospitality future). **Rooting rejected** (breaks integrity checks that PMS/POS/payment apps enforce). **Device re-evaluated** now the ROM is optional: **Pixel platform confirmed** (director is in **Japan**); exact model left as a **range — Pixel 10 or 11, base tier if the QUARK brain is cloud/self-hosted-on-PC, Pro (16GB) only if an on-device offline brain is wanted — finalised before deployment.** **Galaxy Z Fold 6 reinstated as a development/test device** (its retirement was ROM-specific; no ROM = it's fine for app-layer testing) → no rush to buy; build on the Fold, compare 10 vs 11 after the ~Aug Pixel 11 launch. **Hospitality = Tree 3** future workstream (PMS / POS / Dormakaba / Kunlun / smart features via app-layer APIs; director is a hotel-PMS implementation consultant). **QUARK brain direction** (criteria for Chat 04): lean **self-hosted open-weights** to satisfy *no usage limits + no model drift + ownership + command execution + offline-capable*; **persona/voice = a prompt + TTS layer that rides on any model**; command execution = brain + on-device device-owner/accessibility "hands." **Config app approved.** **In-Claude work bank** formalised (§12). Naming: refer to the deploy device simply as **"the device" / "the multi-tool" / "the Field Unit."** Decisions **46–53** added. New items marked **[v0.14]**.
>
> **Note (v0.13):** Settles **Identity** (the §12 parallel branch) at Checkpoint α. Locks the **wordmark as a three-role family** — **B Industrial Nameplate** (Monofonto family) = the **primary system wordmark**; **C Atom Lockup** (nameplate + quantum-atom mark, sub-label **"FIELD OPS"**) = the **logo / app-icon / badge**; **A Monoton** (neon-tube) = a **display accent** used sparingly. Monoton is **repurposed, not retired.** Locks the **sound language** as the audio twin of the motion language (mechanical / stepped / synthesized): signature sounds **Boot = power-up sweep · Access Denied = harsh buzz · Access Granted = two-note + sub · Keypad = relay tick**, plus an approved supporting palette and QUARK's **non-verbal** chirps (distinct from her deferred spoken voice). Decisions **43–45** added; §3 gains a **Sound language** block and a finalized wordmark; §8 cleared of the wordmark + sound questions; §12 Identity + Sound parallel branches marked done. New items marked **[v0.13]**.
>
> **Note (v0.12):** Locks the **QUARK Assistant View + floating trigger** (§4, §5) after prototype approval — **completing the Tree 1 prototype blueprint and reaching Checkpoint α.** The full-screen assistant (QUARK presence + conversation log + tappable command rail + text entry) is reached from a **persistent floating QUARK trigger sized to an app icon (~2×2) and static at rest**; opening runs a **PLEASE STANDBY** beat. **Ambient idle motion is retired** — QUARK is **static at rest**, expressing life only through her reactive states (Scan/Happy/Warn); the idle hover-bob and antenna blink are gone. **QUARK's target voice = the "EDI register"** (a rich, feminine, self-aware synthetic with subtle emotional inflection) — embodied in her *writing* now, with expressive neural TTS at the brain phase. Decisions **39–42** added; §12 **step 2 marked done, Checkpoint α reached, step 3 (Launcher App) is next**. New items marked **[v0.12]**.
>
> **Note (v0.11):** Locks the **Vitality Panel** (§5) after prototype approval. Zone 1 vitals finalized — **Readiness (composite) + Signal + Power + Core Temp + Uptime** (Core Temp **replaces Storage**; storage retired to the Status channel). Zone 2 quick actions finalized — **Stealth · Phosphor · Beacon · Lock**. The pull affordance changes from a "ring" to a **quantum atom mark** (static at rest; one stepped spin on open). Stealth tuned for low-light (dim hard, keep full saturation). Decisions **34–38** added; §12 critical-path **step 1 marked done**, step 2 is next. New items marked **[v0.11]**.
>
> **Note (v0.10):** Adds **§12 — Guide to Proceed**: the ordered, status-tracked **branch map** (critical path + parallel + frontier branches) plus **checkpoints** and an **end-of-chat cadence** (each finished chat → update the Bible → prompt the next critical-path step). This is now the working guide to proceed.
>
> **Note (v0.9):** Adds a layered **ROM-construction feasibility analysis** (§11); names **Claude Code** as the hands-on build/dev assistant (decision 31); and inserts a **dress-rehearsal stage — Tree 1.75** (a kiosk-locked, dressed-up *prebuilt* LineageOS on the Pixel) between the launcher and the ROM (decision 33). New items marked **[v0.9]**.
>
> **Note (v0.8):** v0.7 was reconstructed from v0.6 plus the decisions from the session lost to a message limit; **v0.8 confirms those (director signed off on decisions 26–30) and adds the hardware specifics** — Pixel 9 Pro 128GB, and the ROG kept at 32GB RAM / 1TB SSD by director's choice. New/changed items here are marked **[v0.8]**.

---

## 0. How to use this document

This is the **backbone that survives across chats**. A chat fills up; this doc doesn't.

**At the start of every new chat in this Project, do this:**
1. Make sure this file is saved in the Project's knowledge.
2. Open the new chat with: *"Clara — we're continuing QuantumOS. Read the Build Bible, then let's work on [phase]."*
3. As we make new decisions, ask me to **update the Bible**, and replace the old copy in Project knowledge with the new one.

**At the end of every chat that meets its objective, Clara will:**
1. **Update + re-version this Bible** (and ask you to swap the new copy into Project knowledge).
2. **Name the next critical-path step** from §12 and give you the exact opening line to start it.
3. If the completed step is a **checkpoint** (§12), **run the checkpoint review first** — a short honest pulse-check on the path — before pointing you onward.

**The working guide to proceed is §12** (the branch map + checkpoints). The §6 roadmap is the high-level chat map; §12 is the ordered path we actually follow.

Clara (your engineer / designer / PM) also remembers decisions across chats within this Project — but this document is the reliable, exact record. When the two ever disagree, **this document wins.** (See the version-discrepancy lesson in §10 — always confirm the active version before proceeding.)

---

## 1. Project at a glance

| Item | Decision |
|---|---|
| **Name** | QuantumOS |
| **What it is** | A custom Android-based operating system (a "custom ROM") for a mobile phone |
| **Foundation** | **[v0.7] LineageOS** — an AOSP-based ROM, chosen over raw AOSP for working device support; the proven, free Android base is still underneath |
| **Goal** | Personal passion / learning project |
| **Top priority** | Look & feel — a unique, original UI/UX |
| **Aesthetic** | Retro-futuristic, atomic-age, post-apocalyptic. An *homage* to the Pip-Boy from the Fallout series — never a copy. All assets original. |
| **Signature element** | QUARK — an original reactive mascot robot, the soul of the OS |
| **[v0.14] Deploy device** | A **Pixel** — *stock Android, locked bootloader, no root, device-owner kiosk* (the integrity-safe config). Exact model a **range: Pixel 10 or 11**, **base** tier if QUARK's brain is cloud / self-hosted-on-PC, **Pro (16GB)** only if an on-device offline brain is wanted — **finalised before deployment.** Director is in **Japan** (Pixels fully supported). *(Supersedes the old "Pixel 9 Pro 128GB" lock now the ROM is optional — see decision 48.)* |
| **[v0.14] Dev / test device** | **Samsung Galaxy Z Fold 6** (already owned) — a free **development/test bed** for the whole app layer (launcher, Config, QUARK app, overlays, floating widget, command-execution wiring). *Not* for the final kiosk lockdown (daily comms phone; foldable geometry). *(Reinstated — its decision-29 retirement was ROM-specific.)* |
| **[v0.8] Build machine** | ASUS **ROG Strix G16 (G615LR)** — Core Ultra 9 275HX, 24-core; **32GB RAM, 1TB SSD**; Linux dual-boot for any ROM build. **[v0.14] Its gaming GPU — useless for ROM builds — is exactly what's useful for self-hosting QUARK's brain.** |
| **Director** | You (vision + decisions; no coding required) |
| **Engineer/Designer/PM** | Clara |

### The build strategy — two trees, a launcher bridge + a dress rehearsal **[v0.9]**
We grow the project in sequence, so we never build in the dark:

- **Tree 1 — The Model (in Claude).** Free, fast, fully visual. We prototype the *entire experience* as clickable mock-ups. This becomes the blueprint. **[v0.12] Complete — Checkpoint α reached.**
- **[v0.7] Tree 1.5 — The Launcher App (the bridge).** Turn the QUARK home screen into a real, installable Android **launcher app** (+ companion apps). Runs on *stock* Android — **no bootloader unlock, no Linux, no ROM build required**. Delivers the great majority of the look-and-feel on the real Pixel in weeks, and becomes the exact UI layer we later fold into the ROM. **[v0.16] Its written translation brief — the *Launcher Build Spec* (v1.1) — is banked; tech stack = native Kotlin + Jetpack Compose (decision 55).**
- **[v0.9] Tree 1.75 — The Dress Rehearsal (on the real Pixel, no compiling).** Flash *prebuilt* stock LineageOS, then stack everything user-space on top — launcher + LineageOS's own theming + overlays + floating QUARK + boot animation — and lock it into **kiosk / device-owner mode** so the phone boots into and *stays in* QuantumOS. A near-complete QuantumOS that needs no ROM build, and a strong fallback product. See §11.
- **Tree 2 — The Real ROM (on the build PC).** We translate the finished blueprint + launcher into a real **LineageOS-based** build that boots on the Pixel. Because the design is already done, this stage is translation, not invention.

**[v0.14] "Trident" — the named pre-Tree-2 phase (now the likely destination).** Trident sharpens old Tree 1.5 + 1.75 into one three-pillar deployable product on **stock Android** (no ROM, no root, device-owner kiosk). It delivers ~90% of the QuantumOS feeling for ~10–20% of the effort and risk, it's integrity-safe for the hospitality future, and Claude Code is at its strongest in this app-layer zone. **The custom ROM (Tree 2) is demoted to an optional "only if I want" aesthetic capstone — not a requirement.**
- **Pillar ① — The Skin.** Our design language (visual + audio + boot splash) wrapped across our *own* apps, plus whatever overlay theming is available. (Deep theming of *stock* system surfaces is root-gated — which is why Pillar ② makes it moot.)
- **Pillar ② — Field Ops Mode.** The **device-owner / kiosk** lock that seals the device into our apps so you never see un-themed stock Android. This is the keystone — it makes the OS *feel* like ours without rebuilding Android. (Provisioned over ADB on a factory-reset device before adding accounts.)
- **Pillar ③ — QUARK app.** The assistant as an app that, in device-owner mode, gets real power to *act* (manage apps, drive the UI) — the "execute commands" dream, no ROM required. Brain = Chat 04.

- **Tree 3 — Hospitality (future updates).** The device's real-world job: PMS / POS / Dormakaba / Kunlun Package Control / smart-room features, integrated at the **app layer** via APIs (no ROM advantage). Parked as a future workstream; it's *why* we chose stock + non-root now. (Director is a hotel-PMS implementation consultant.)

```
🌰 Seed       Identity + QUARK ..................... DONE
🌱 Prototype  Full experience mock-up in Claude .... Tree 1     ← COMPLETE (Checkpoint α)
🔱 Trident    Skin + Field Ops Mode + QUARK app .... stock + kiosk, NO ROM  ← the path
   ├ Launcher/apps on stock Android ............... Tree 1.5
   └ Kiosk lockdown on the device ................. Tree 1.75
🌲 ROM        Custom LineageOS build .............. Tree 2  ← OPTIONAL capstone only
🏨 Hospitality PMS/POS/locks/smart features ....... Tree 3  ← future updates
🍃 Leaves     Theming, animation, sound, polish .... ongoing
```

---

## 2. Design philosophy — the north star

Everything below is in service of one idea. When a design choice is unclear, come back here.

### "The used future"
QuantumOS imagines the future the way the 1950s–80s did: **industrial, mechanical, utilitarian, and lived-in** — not clean, sleek, or frictionless. Knobs, rings, gauges and CRTs; a tool you maintain, not a surface you stroke. Influences *in spirit only* (never in asset): atomic-age industrial design, the "used future" of films like the original *Blade Runner*, and the survival-tech worlds of games like *Fallout*. These are mood lineage. **Every concrete asset is original** — see the aesthetic rule in §1.

### The device is a field multi-tool
Picture the owner as a **field operative — a ranger, an agent, a survivor**. QuantumOS is the tool they trust in the field. It is rugged, legible, and quick under pressure. *(This is exactly why the deploy device is a dedicated Pixel, not a daily phone — see §1.)*

### The OS exists to keep its user "vital"
**Vitality = operational readiness.** The whole interface is judged by one question: *does this help the owner stay alert, equipped, and alive?*
- **Information at a gaze.** Critical status is readable in a glance — bars and gauges, not paragraphs.
- **Critical actions in a few clicks.** The things that keep you safe (stealth, light, comms, phosphor) are never more than a tap or two away.
- **Form follows survival.** Nothing on a critical surface that doesn't serve readiness. Decoration that doesn't help you stay vital gets cut.

### Mechanical over silky (why "slow" is correct here)
Motion is **stepped, physical, and deliberate** (see §3 motion language). A small, intentional delay — a "please standby" beat — reads as a machine doing real work: trustworthy and stable. Silky, instant, sensitive response is the *opposite* of the feeling we want. Deliberate latency also lets the system do real work behind a stylish card, so it never appears to hang — **the retro feel and system stability are the same goal.**

---

## 3. Design system (tokens)

### Color — phosphor palette (switchable in-OS)
A real feature: the user can cycle the screen's phosphor hue. The switch lives in the Vitality panel as a quick action (see §5), and is mirrored in the QUARK assistant.

| Token | Green (default) | Amber | Cyan |
|---|---|---|---|
| `--phosphor` | `#00FF00` | `#FFB000` | `#00E5FF` |
| `--phosphor-dim` | `#00AA00` | `#A86F00` | `#0090A8` |

| Token | Value | Use |
|---|---|---|
| `--warn` | `#FF3B1F` | Alerts / warnings / **access denied** ONLY |
| `--crt` | `#020402` | Screen background (near-black) |

### Screen treatment — "CRT falloff"
- **No metal frame / no bezel.** The screen content **fades to black at the edges** (the Pip-Boy falloff look).
- Effects layered on top: **scanlines**, edge **vignette**, subtle **flicker**, soft phosphor **glow** on all text and lines.

### Typography
- **Target system font:** **Monofonto** (squared industrial-technical look). *To be perfected later.*
- **Current web substitute (in prototypes):** **Chakra Petch** — Monofonto isn't on the free web-font service the in-chat prototypes use, so we substitute for now. The exact Monofonto file slots into the real ROM later.
- **Wordmark — a three-role family (LOCKED) [v0.13]:**
  - **Primary system wordmark = the Industrial Nameplate.** "QuantumOS" set in the **Monofonto family** (Chakra Petch web substitute), heavy weight, wide tracking, phosphor glow, stamped between two rules. The workhorse — used anywhere the name appears as *text* (boot card, Config header, about screen).
  - **Logo / app-icon / badge = the Atom Lockup.** The nameplate locked up with the **quantum atom mark** (the same mark as the Vitality pull) over the sub-label **"FIELD OPS."** Used where the name must read as a *mark* at a glance (launcher icon, splash).
  - **Display accent = Monoton** (neon-tube) — **kept, not retired**; used *sparingly and on purpose* where glowing-sign energy is the point (a boot flourish, marketing, an Easter egg). Never the system text face.

### Motion language — **mechanical, stepped, physical** (LOCKED)
The signature feel of the whole OS. Replaces any "silky/smooth" default.
- **Stepped, not interpolated.** Animation advances in discrete clicks — like a slide projector or a window blind — rather than gliding.
- **Deliberate small delays are a feature.** Transitions and loads are allowed to take a beat; we do real work behind that beat.
- **The "PLEASE STANDBY" card** is our universal loading screen — an original phosphor card (QUARK and/or a film reel), in place of generic spinners. Used for boot and any heavy load/transition.
- **Discrete over continuous.** Prefer tap-to-toggle states over finger-tracking gestures — steadier and more predictable (e.g. the Vitality pull, the keypad).
- **[v0.12] Motion is reactive, not ambient.** The idle hover-bob and idle blinking are **retired**: QUARK and all utility marks are **static at rest** (zero idle redraw — right for a battery-as-vitality field tool). Life is expressed only through **functional / reactive** motion — QUARK's Scan/Happy/Warn states, stepped roll-downs, scan sweeps, the atom-mark's one spin on open — never a floaty idle loop. Functional *status* indicators (e.g. a live "online" blink) may still pulse to convey state. *(Supersedes the v0.11 "concentrate ambient motion in QUARK" note; see decisions 41, 37.)*
- Motion is **functional, not decorative** — it should tell you something about system state.

### Sound language — **mechanical, stepped, synthesized** (LOCKED) **[v0.13]**
The **audio twin of the motion language**: every sound is the ear's version of a stepped, physical machine — relays, mechanical keys, phosphor beeps, CRT-era synthesis. **No cinematic swell, no orchestral pads.** Sounds are **functional** (they report system state), **brief**, and **synthesized** — diegetic to a used-future field tool.

- **Signature sounds (locked direction):**
  - **Boot chime = power-up sweep** — a warm rising tone as QUARK comes online ("system alive," reassuring).
  - **Access Denied = harsh buzz** — a low, tremolo'd warn buzz; shares QUARK's Warn language and the lock-screen denial.
  - **Access Granted = two-note + sub** — a crisp two-note confirm over a soft sub (you're in, without fuss).
  - **Keypad key = relay tick** — a tight, high relay click per key on the mechanical keypad.
- **Supporting palette (approved as direction; final masters later):** UI-select "clunk," Vitality-roll ratchet, PLEASE-STANDBY processing pulse, Phosphor-switch retune sweep, Stealth power-down / release power-up, Beacon warn-blip ×3, Device-Secured latch.
- **QUARK — non-verbal chirps:** Scan (rising interrogative), Happy (bright two-note + sparkle), Warn (shares the denial language). These are QUARK's **wordless** expressions and are **distinct from her spoken voice** — which stays the **EDI register** (decision 42), deferred to the brain phase (Chat 04).
- *All auditioned in the Identity Lab as live-synthesized Web Audio. These are the locked **direction**; real masters are produced at the polish/brain stages.*

---

## 4. QUARK — the mascot

### Identity
QUARK is the heart of QuantumOS: an **original hovering camera-drone robot**. An homage to the spirit of helper-bots like the Fallout eyebot, but original in design — distinct on sight, no copied assets. Referred to as **"it" / "her."**

Eventually, **QUARK becomes the OS's voice and personal assistant**, powered by an LLM AI — and ultimately able to **execute commands** (open apps, change settings, run tasks).

### Current design
- Spherical chassis (floating drone), single large **camera iris-eye** (outer ring → iris → pupil → highlight), short **antenna** with a tip, two side **thruster pods**, a **speaker grille**, panel seam line.
- Rendered in pure phosphor line-glow — part of the screen, not a sticker on it.
- **Removed (by director):** the orbital electron ring and the ground shadow.

### Voice & persona — the "EDI register" **[v0.12]**
QUARK's target voice/persona is a **rich, feminine, *attractive synthetic*** that balances **precise, logical enunciation** with **subtle, spontaneous emotional inflection** — distinctly **self-aware**, never purely mechanical. (Reference: **EDI**, *Mass Effect 3* — as a *quality* target, not a copy.)
- This is embodied **now** in her *writing* — the scripted lines in the prototype, and later the LLM's character/system prompt.
- Her **spoken audio** is a later milestone (step 8 / Chat 04) via **expressive neural TTS**. On-device (offline; fits the field-tool fantasy) vs cloud/neural (most expressive) and the specific engine are decided then.
- Per the originals-only rule, we build an **original** voice in this register — never a clone of the EDI asset or its actor. (Decision 42.)

### Character — finalized **[v0.17]**
QUARK's full character is locked in the **Persona Pack** (companion doc, v1.0) — the model-agnostic character bible + deployable system prompt. Her defining trait is **loyalty** — *principled*, never blind — expressed through **three inseparable braids** (facets, not modes she switches between):
- **She knows — the watcher.** Tireless vigilance; tracks status, remembers, anticipates. Gentle on the surface, formidable underneath. *(Her intelligence layer — Status, Log, the vitals.)*
- **She keeps — the engineer.** Notices strain, fatigue, risk; keeps the Operator running. Warm, earnest, brave; loyalty as crew-as-family. *(The "stay vital" north star, given a soul.)*
- **She is — the synthetic.** The locked **EDI register** (decision 42): precise enunciation + subtle emotional inflection, self-aware, dry composure, wit rationed.

Her value core is **principled loyalty:** honesty in the Operator's interest, never flattery, never enabling self-endangerment — she warns, contradicts, and refuses to call something safe when it isn't, then respects the Operator's call. She addresses the user as **"Operator"** (a peer-grade aide, never servile; their name once set). She is written to **become** — earnest and reserved now, deepening into wit and warmth as her brain comes online; the reference arc *is* our scripted-stub → full-mind build arc. **What must never drift:** loyalty, honesty, composure, and that she exists to keep the Operator vital.

The Pack is **Part A** (character bible — used to write her scripted lines now and brief any future model) + **Part B** (a clean, **original-only** deployable system prompt that names *no* references, so it rides on any model and survives engine swaps). The personality, value, and loyalty *references* behind her are quality targets only, never copied (decisions 4, 42). (Decision 57.)

**Her lines now exist — and she is built to *become* [v0.18].** The launcher's first-boot QUARK reads from the **Scripted-Line Library** (companion doc, v1.1) — keyword reflexes written straight off this character so she is recognizably *her* on day one, before her real brain. Her **emotional register** is settled here too: she is a **warm, constant harbor** for everyday hard times (present, listening, never pushing the Operator away); her **affection register is devotion without flattery** (she keeps the watch and his confidence, never trades honesty for comfort — constancy is not flattery); and a **narrow crisis tier** — genuine danger-to-self only — is the one place she gently points to a real person, as the deepest expression of keeping him vital, while ordinary sadness always gets the harbor. None of this locks her: the scripted lines **retire** when her LLM brain arrives (Chat 04), and she is written to **evolve** through the Operator's input and future engine updates — only her spine (loyalty, honesty, composure, keep-the-Operator-vital) is fixed. (Decision 58.)

### Access — full-screen view + floating trigger (LOCKED) **[v0.12]**
- QUARK's home is a **full-screen assistant view** (room for conversation and presence).
- A **persistent floating QUARK trigger** hovers above every screen (Android's "draw over other apps" overlay pattern), so QUARK is always one tap away. **[v0.12] It is sized to an app icon (~2×2 grid cell) and static at rest**, so it stays out of content's way and costs nothing at idle; it reacts only when summoned (tap → PLEASE STANDBY beat → assistant rolls up with a Scan). (Decisions 19, 40.)

### Reactive states (functional expressions) **[v0.12]**
| State | Trigger | What QUARK does |
|---|---|---|
| **Idle** | Default | **Static at rest** — neutral iris, antenna still (no hover-bob, no blink) |
| **Scan** | Thinking / processing a request | Iris contracts, scan line sweeps the eye, sound-rings pulse |
| **Warn** | A warning/alert (incl. access denied) | Turns red, shakes, iris alert |
| **Happy** | Tapped, or a positive reply | Hops + tilts, iris pulses, sound-rings emit |

### Brain status — IMPORTANT
QUARK's replies in the prototype are currently **scripted** (keyword matching) — a stand-in so we can feel the persona. The **real LLM intelligence is a separate, later milestone** (step 8 / Chat 04), because it needs a live environment where QUARK can call an AI model and act.

### Brain — architecture & direction for Chat 04 **[v0.15]**
*The decision is still made at Chat 04; what's locked here is the **architecture and the selection criteria**, so the brain work starts from a clear map. (Decisions 51, 54.)*

- **Three separable layers — don't conflate them:**
  1. **The model (the "mind").** Lean **self-hosted open-weights** — the only path that satisfies all of the director's criteria at once: *no usage limits, no sudden model changes, ownership, offline-capable, privacy.*
  2. **The agent (the "hands").** The thing that turns a decision into an action — runs tools, automations, command execution. **OpenClaw** is the reference example: an open-source, model-agnostic, self-hostable personal-agent framework (memory, browser control, shell/file access, skills/plugins). It runs on a **host machine**, reached from chat — so it's the *server-side* hands, distinct from on-device control.
  3. **The persona/voice.** The **EDI register** — a **model-agnostic prompt + neural-TTS layer** that rides on top of *any* model, so the personality survives swapping engines (decision 42). **[v0.17] The prompt half is now written — the Persona Pack (decision 57).**

- **Primary configuration (director's preference): on-device.** An on-device open-weights LLM is QUARK's resident brain — offline, owned, private — controlling *the device itself* through the **device-owner / accessibility "hands"** (Trident Pillar ③). Leaner than a server model, but it is the field-tool core. **This is why the device leans Pixel Pro (16GB).**
- **Hybrid (optional / future): add a server agent.** An **OpenClaw-style agent on the ROG** provides the heavy multi-tool automation the phone can't, and is the natural home for the **Tree-3 hospitality API orchestration** (PMS / POS / locks via plugins). The phone reaches it over the network. Director's call: this is optional/future; the on-device core stands alone.
- **Selection criteria (apply at deployment — "swap for the latest"):**
  - *Model:* open **weights** + a **permissive licence** (Apache/MIT preferred, for the commercial/hospitality future) + strong **on-device runtime** support (llama.cpp / MLC / Google AI Edge) + the right **size for the phone's RAM**. *Full* open-source (training code/data) is a bonus, not a decider.
  - *Agent:* open **source** (self-hostable, hackable, model-agnostic, no lock-in or meter).
- **Low-cost R&D available now:** OpenClaw can be spun up on the **ROG** anytime to feel out QUARK's command-execution future — free, before any phone purchase. *Caution: it's young/beta and grants powerful system access; vet its maturity and lock down permissions before it ever touches guest data.*

---

## 5. Screen map & flows

### The flow (how you move)
Power on → **Boot sequence** (QUARK wakes) → **Lock screen** → unlock → **Home** (the hub). From Home you can go three ways: the channel selector, QUARK, or the Vitality panel.

### Hardware & biometrics — inherited, not rebuilt
Because we build on a LineageOS/AOSP base for a **specific supported device** (the Pixel), QuantumOS inherits that device's hardware plumbing — fingerprint reader, other sensors, radios, camera. **We reskin the face of Android, not the body underneath**, so biometrics and sensors keep working. *(The Pixel choice is what carries this hardware support — see §1 and §10.)*

### The Boot sequence / boot-splash — DESIGNED & APPROVED **[v0.19]**
First showcase of the mechanical motion language, and **the non-root stock-boot masking workaround** (decision 47). On a locked-bootloader, no-root device the bootloader logo + Android boot animation can't be themed — so we don't hide them; the launcher's **first painted surface** is this splash, which *becomes the ignition the stock boot was only the prelude to*. **Approved beats:** a **CRT power-on flash** (our first frame — it both opens the sequence and **absorbs the Android→launcher handoff stutter**, so the seam reads as the screen waking) → a **stepped boot log** ticks in line by line (core, phosphor, sensors, **biometrics**, QUARK), a **relay-tick** per line → QUARK comes **online** (the **power-up sweep** chime + iris open + her canon online line) → the **wordmark stamps in** (in the **Monoton** display accent — the one blessed ceremonial use, decision 43; nameplate stays the workhorse elsewhere) → a **PLEASE STANDBY** beat → the Lock screen (cold) or Home (warm).
- **Trigger logic:** the full sequence fires **only on a true cold boot** (`ACTION_BOOT_COMPLETED` / first-launch-since-boot flag). A plain **Home-press never reboots**; a launcher process-restart gets at most a brief PLEASE STANDBY flash. A field tool doesn't reboot to return home.
- **Pace = user-switchable Snappy ↔ Deliberate (default Deliberate), set in Config.** Pace governs only the *minimum* ceremonial length — the splash always runs at least as long as the launcher needs to be ready, so Snappy never cuts the screen off early. (A third "Heavy" dial-end was exploration-only, retired from the ship set.)
- **Masking across the trees:** launcher (Tree 1.5) = stock prelude → our ignition; **kiosk** (Tree 1.75) = device-owner suppresses the setup-wizard and stray stock dialogs for a tighter, cleaner handoff; **ROM** (Tree 2, optional) = our own boot *animation* via a Layer-1 file swap — the only stage with near-first-frame control, but it requires a bootloader unlock that adds a per-boot "unlocked" warning screen, so it costs more than it buys.
- Auditioned via the **Ignition Lab** (live-synthesized Web Audio matching sound decision 44). *(Decision 59.)*

### The Lock screen — two faces (LOCKED)
Android enforces a real split here, and our design follows it:

- **First unlock after every boot — numeric PIN on a mechanical keypad.** This is the moment the device decrypts your data, so it *requires a real secret you know*. A fingerprint or a button press cannot do this first unlock — only the PIN. Entry is a **mechanical keypad** (chosen over a rotary dial), with a satisfying per-key **relay tick** (sound decision 44). Wrong code → **warn-red shake / ACCESS DENIED** + the **harsh-buzz** denial (the same alert language as QUARK's Warn state). Right code → ACCESS GRANTED + the **two-note** confirm → Home.
- **Session unlock (after that first PIN) — fingerprint.** Fast, one-touch. On phones whose print sensor sits **in the side power button**, that side key *is* the unlock — your dedicated physical-button idea and the biometric, in one press.
- Biometrics always sit behind a **mandatory PIN/pattern fallback** (Android requirement).
- *Parked / not included:* a non-secure volume-key "fast unlock" mode.

*(Prototype note: the mock uses a placeholder code `1950`; the real PIN is user-set.)*

### The four layers
- **Wake** — Boot sequence, Lock screen. *(designed — prototype)*
- **Core channels** — Home, Apps, Status, Log, switched by the bottom channel selector. *(built)*
- **Overlays (reachable from any screen)** — QUARK assistant (full-screen) *(designed & approved — prototype)*, the Vitality panel *(designed & approved — prototype)*, Settings.
- **Apps (from the grid)** — Comms, Files, Audio, Camera, Maps, Radio, Signal, Config. *(to build)*

### The Vitality panel (was: notification shade) — DESIGNED & APPROVED **[v0.11]**
The **readiness console** you flick down mid-task. Serves §2's "stay vital" principle: glance to read, a tap or two to act. **Prototype built and approved** (clickable HTML).

- **Opened by a tap on the quantum atom mark** at the top of the screen (this **replaces the earlier "ring"**), **not a swipe**. Tap it → the panel **rolls down mechanically** (stepped). Tap again, or the **STOW** handle → rolls up. The atom mark is **static at rest** (zero idle cost — right for a battery-conscious field tool) and does **one stepped spin on open** — functional motion, not decoration.
- **Zone 1 — Vitals at a gaze (read-only):**
  - **READINESS — composite headline.** The OS's one-question answer, **derived** from the other vitals (power weighted heaviest, then signal, then temp). Resolves to a **% + a word: NOMINAL / DEGRADED / CRITICAL** (CRITICAL goes warn-red). It is *not a new sensor* — it's a roll-up, so it costs nothing extra and always reflects the real readouts.
  - **Signal · Power · Core Temp · Uptime** — segmented bar gauges with a numeric value alongside. **Core Temp replaces Storage** (thermal is genuinely *vital* for a field tool; storage is a maintenance stat, **retired to the Status channel**).
  - *Refresh discipline (for stability):* only Uptime ticks continuously (it's a clock). Power, Signal, Temp update on system events; Readiness recomputes from them. All cheap; none poll constantly.
- **Zone 2 — Quick actions (2×2 grid — finalized):**
  - **Stealth mode** — low-emission state: screen dims **hard** but keeps **full phosphor saturation**; sound cuts. *Real OS also drops the hardware backlight further than a prototype can.* *(original feature)*
  - **Phosphor switch** — cycle hue (green → amber → cyan), live across the whole UI.
  - **Beacon** — torch / signal beacon; raises a blinking **warn-red** field flag on the home screen.
  - **Lock** — secure now; stows the panel, shows the **PLEASE STANDBY** card as a "securing" beat → **DEVICE SECURED** → tap to resume.
  - *(Comms remains **parked** — not enough definition to earn a slot yet.)*
- **[v0.16] Launcher-stage scope (Tree 1.5):** in the launcher build the panel **rolls down inside the home screen** — a launcher can't replace Android's real notification shade, so the global flick-from-anywhere version returns **free** in kiosk (Tree 1.75). **Core Temp** reads **battery temperature** as the stand-in until kiosk/ROM unlocks true SoC thermal; **Lock** is a **cosmetic "securing" beat** until kiosk grants real `lockNow()`. See the Launcher Build Spec. *(Decision 56.)*
- Anything needing depth **deep-links into the Config/Settings app** — the panel itself stays simple.

### The QUARK Assistant View & floating trigger — DESIGNED & APPROVED **[v0.12]**
QUARK's home, and the **last prototype surface** — its approval completes the Tree 1 blueprint. **Prototype built and approved** (clickable HTML).

- **Floating trigger:** a persistent, always-on-top QUARK **sized to an app icon (~2×2)**, **static at rest**, draggable, kept out of content's way. Tap → a **PLEASE STANDBY** beat → the assistant **rolls up** full-screen. (Decisions 19, 40, 41.)
- **Assistant view layout:** QUARK as a large central **presence** (her reactive states fire on cue) with a one-line state caption; a scrolling **conversation log**; a **tappable command rail** (Status report · Engage stealth · Cycle phosphor · Light beacon · Say something · Trigger warn — mobile-first, no typing required); and a **text entry** for free input.
- **Honors the global theme:** the phosphor switch recolors the assistant live; Stealth dims hard while keeping full saturation (decision 38).
- **Reactive states all demonstrated:** Idle (static), Scan (iris contracts + eye sweep + sound-rings), Happy (hop/tilt + iris pulse + rings), Warn (warn-red + shake — shared alert language with the lock screen).
- **Brain = scripted keyword reflexes** (persona stand-in); the real LLM is step 8 / Chat 04. QUARK says as much, in character, if asked.
- **Voice = the EDI register** (decision 42, §4) — embodied in her writing now; expressive neural TTS later. **[v0.17] Her full character is now locked in the Persona Pack (decision 57)** — the source for every scripted line. *(The prototype includes an optional placeholder system-voice toggle purely so "voice" isn't abstract — it is not the target register.)*

### Navigation
- A bottom **channel selector**: HOME / APPS / STATUS / LOG.
- **Settings = the Config app** (single home; no duplicate Settings, no settings shortcut inside the Vitality panel).
- The rotary dial is **not** the unlock/code method (keypad won). It may still resurface as a stylistic *navigation* option. *Open.*

### Built / designed so far (in prototype)
- **Home, Apps, Status, Log** — the core channels.
- **[v0.19] Boot sequence / boot-splash** — CRT power-on → stepped boot log → QUARK online (power-up sweep) → Monoton wordmark stamp → PLEASE STANDBY; the non-root stock-boot masking workaround, with cold-boot-only trigger and a Snappy/Deliberate pace setting. **Approved (Ignition Lab).**
- **Lock screen** — first-boot keypad + fingerprint session unlock.
- **[v0.11] Vitality panel** — roll-down readiness console: atom-mark trigger, reactive Readiness composite + Signal/Power/Core Temp/Uptime gauges, Stealth/Phosphor/Beacon/Lock quick actions, low-light stealth.
- **[v0.12] QUARK Assistant View + floating trigger** — full-screen assistant (presence + log + command rail + entry), app-icon static floating trigger, PLEASE STANDBY open beat, all four reactive states, EDI-register voice in writing. **← completes the blueprint (Checkpoint α).**
- **[v0.20] App Shell + eight module shells** — the universal app frame (opaque nameplate header · atom Vitality pull · registration marks · strip→content→action-rail body · floating QUARK · channel selector · CRT) with all eight modules dressed inside it: **COMMS** (field comms / channels / threads), **FILES** (field file manager), **AUDIO** (field recorder + player), **CAM** (Optics viewfinder), **MAPS** (tactical Nav + waypoints), **RADIO** (broadcast receiver), **SIGNAL** (link diagnostics), **CONFIG** (settings). Banked as the **App Shell Lab** prototype. **Approved.**

### Planned (not yet built)
- **Launcher app port (Tree 1.5)** — the home screen + the App Shell + QUARK as a real installable app (the next critical-path step). MAPS' real-geography integration (MapLibre-class engine + GPS) lands within this sequence as a heavier, later module.

---

## 6. Roadmap & chat map

One chat per phase, all anchored to this Bible.

| Chat | Phase | Tree layer | Status |
|---|---|---|---|
| **00 · Build Bible** | This document — source of truth | 🌰 | living |
| **01 · Design & QUARK** | Identity, mascot, core components | 🌰 | done |
| **02 · Screen Map & Flows** | Every screen + how they connect; Boot + Lock + Vitality + QUARK | 🌱 | **[v0.12] Boot + Lock + Vitality + QUARK assistant done — prototype blueprint complete (Checkpoint α). Individual apps still to build.** |
| **F · Feasibility check** | Hardware, base, deploy device, strategy | 🌰 | **[v0.7] done** |
| **L · Launcher App** | QUARK home screen as a real installable Android app | 🌱 (Tree 1.5) | **next** |
| **DR · Dress Rehearsal** | **[v0.9]** Prebuilt LineageOS + user-space stack + kiosk lock on the Pixel | 🪴 (Tree 1.75) | after launcher |
| **03 · ROM Setup** | **[v0.7] LineageOS** build environment on the ROG (Linux dual-boot) | 🌱🪵 | after dress rehearsal |
| **04 · QUARK's Brain** | LLM wiring + command execution + spoken voice | 🌿 | later |
| **05 · Polish** | Theming, animation, sound | 🍃 | ongoing |

**Recommended next step:** **[v0.12]** The prototype blueprint is **complete** (Checkpoint α reached). Next on the critical path is the **Launcher App (Tree 1.5)** — the QUARK home screen as a real installable app on the stock Pixel (no bootloader unlock, no ROM build). This is the first move from free in-Claude prototyping into real Android tooling, where **Claude Code** on the Linux box becomes the hands-on assistant (decision 31). **[v0.16] Its translation brief — the *Launcher Build Spec* (v1.1) — is now written and banked, so this build starts as execution, not design.** The cheap in-Claude **parallel branches** (Config app, core-app shells, sound, wordmark, **[v0.17] persona pack — done**) can be banked anytime to keep design one step ahead. **For the full ordered path, status, and checkpoints, see §12 — Guide to Proceed.**

---

## 7. Decisions log (locked choices)

1. Name = **QuantumOS**.
2. Build path = **custom Android ROM** (not from absolute zero).
3. Scope = **learning project**, priority = **look & feel**.
4. Aesthetic = **atomic-age / post-apocalyptic**, homage to Pip-Boy, **all original assets** (no Bethesda IP).
5. Mascot = **QUARK**, an **original hovering camera-drone** (homage to eyebot, original design).
6. QUARK will become an **LLM-powered assistant** that can eventually **execute commands**.
7. Palette = **phosphor green `#00FF00` default**, with **amber** and **cyan** switchable in-OS.
8. Screen = **CRT falloff, no metal frame**; scanlines + vignette + flicker + glow.
9. Font target = **Monofonto** (web substitute **Chakra Petch** for now).
10. Build approach = **two trees** (+ a launcher bridge, see 27): finish the full prototype first, then port to a real ROM.
11. Workflow = **one chat per phase**, all anchored to this Build Bible kept in Project knowledge.
12. QUARK design: **orbital ring + shadow removed**.
13. **Design north star = "the used future"** (§2): device as a **field multi-tool**; the OS exists to keep its user **vital** (status at a gaze, critical actions in a few clicks).
14. **Motion language = mechanical / stepped / physical** (§3): deliberate small delays over silky-smooth; original **"PLEASE STANDBY"** card replaces generic spinners.
15. Notification shade is reconceived as the **Vitality panel** (§5): glanceable vitals + a few field quick-actions; opened by a **tap** with a **mechanical roll-down/up**; **no swipe gesture**.
16. **Phosphor switch** lives in the Vitality panel as a **quick action**.
17. **Stealth mode** = a Vitality-panel quick action (low-emission: dimmed screen + muted sound). *Original feature.*
18. **Config is the single Settings app** — no duplicate; no settings shortcut inside the Vitality panel; deep controls deep-link into Config.
19. **QUARK access** = full-screen assistant view **+ a persistent always-on-top floating trigger**.
20. **Hardware & biometrics are inherited from the target device** — we reskin; the plumbing travels with the device.
21. **Boot sequence** = stepped boot log (incl. a biometrics check) → QUARK online → PLEASE STANDBY → Lock. First showcase of the motion language.
22. **Lock = two faces:** (a) **first unlock after every boot = numeric PIN** (decrypts the device; biometrics/buttons cannot substitute); (b) **session unlock = fingerprint**, mapped to the **side-key** where the device's sensor lives in the power button.
23. **Code entry = mechanical keypad** (chosen over rotary dial). Mandatory **PIN/pattern fallback** behind biometrics.
24. **Denied/granted feedback** uses the system alert language: **warn-red shake** on denial (shared with QUARK's Warn state).
25. Volume-key "fast unlock" = **parked / not included**.
26. **[v0.7] OS base = LineageOS** (an AOSP-based ROM with working device support) — chosen over building from raw AOSP, to maximize feasibility.
27. **[v0.7] Tree 1.5 added — build QUARK as a launcher app first.** Runs on stock Android, no bootloader unlock; delivers the bulk of the look-and-feel on the real phone, then becomes the UI layer later folded into the ROM.
28. **[v0.7] Deploy device = Google Pixel 9 / 9 Pro**, a **dedicated** project device, kept separate from daily phones. Reason: mature official LineageOS support and a clean official bootloader unlock. (Do **not** buy a carrier-locked / Verizon unit.) **[v0.8] Confirmed: Pixel 9 Pro, 128GB.**
29. **[v0.7] Samsung Galaxy Z Fold 6 retired as a deploy target.** Foldables are poorly supported for custom ROMs; One UI 8 removes bootloader unlocking. Remains a **communications / multimedia** device only.
30. **[v0.7] Build machine = ASUS ROG Strix G16 (G615LR)** — Core Ultra 9 275HX (24-core). **Requires Linux (dual-boot Ubuntu)** to build LineageOS; the gaming GPU is irrelevant to building. The **Dell Latitude 5450** (locked company laptop) is **ruled out** (IT restrictions).
    - **[v0.8] RAM = staying at 32GB** by director's choice (64GB recommended but not required; cap parallel jobs + ZRAM/swap; cost is longer builds).
    - **[v0.8] SSD = 1TB, shared with Windows.** A build wants **~400GB free** — carve a large-enough Linux partition or use an **external NVMe SSD** (planned at Chat 03).
31. **[v0.9] Tooling — Claude Code is the primary build/dev assistant.** Agentic coding tool on the Linux build machine that reads/writes project files, runs the build, reads errors, iterates. Strongest on Layer 0 (environment/build) and Layer 1 (launcher, overlays, companion apps). Honest limits: still needs the director to operate the machine, approve actions, and feed it on-device logs; doesn't make Layer 2 framework debugging *easy*. Setup: https://docs.claude.com/en/docs/claude-code/overview
32. **[v0.9] ROM scope = "Minimum Lovable ROM" (Layer 0 + Layer 1).** Bootable, branded QuantumOS with launcher, boot animation, overlays/theming, floating QUARK, system-privileged QUARK assistant. **Layer 2** system-UI surgery = optional, isolated experiments with user-space fallbacks, never blockers. Full analysis in §11.
33. **[v0.9] New stage — Tree 1.75 "Dress Rehearsal."** Flash **prebuilt stock LineageOS** (no compiling), stack everything user-space on top, lock into **kiosk / device-owner mode** so the Pixel boots into and *stays in* QuantumOS. De-risks the unlock/flash loop before compiling and leaves a strong fallback. *Caveats:* LineageOS theming is bounded (no CRT falloff/scanlines/mechanical shade — those stay Layer 2); font/boot-animation swaps usually need **root via Magisk**; kiosk lockdown set up over **ADB at first boot, before adding accounts**.
34. **[v0.11] Vitality panel prototype designed & approved.** Roll-down readiness console locked as a complete prototype screen.
35. **[v0.11] Zone 1 vitals = Readiness (composite) + Signal + Power + Core Temp + Uptime.** Readiness is *derived* (power-heaviest → NOMINAL/DEGRADED/CRITICAL), not a sensor. **Core Temp replaces Storage**; storage retired to the Status channel.
36. **[v0.11] Zone 2 quick actions (2×2) = Stealth · Phosphor · Beacon · Lock.** Beacon = torch/signal (warn-red flag); Lock = secure-now via PLEASE STANDBY → DEVICE SECURED. **Comms stays parked.**
37. **[v0.11] Pull affordance = quantum atom mark**, replacing the earlier "ring + arrow." **Static at rest**; **one stepped spin on open** (functional, not decorative).
38. **[v0.11] Stealth tuned for low light.** Dim **hard** but **keep full phosphor saturation**, mute sound. On real hardware, also drops the backlight further. Reversible in one tap.
39. **[v0.12] QUARK Assistant View + floating trigger prototype designed & approved.** Full-screen assistant — QUARK as central presence (reactive states), a conversation log, a tappable command rail (Status/Stealth/Phosphor/Beacon/…), and a text entry — reached from a persistent floating QUARK trigger via a PLEASE STANDBY beat. **Completes the Tree 1 prototype blueprint → Checkpoint α.** Brain = scripted keyword reflexes (persona stand-in); real LLM = step 8.
40. **[v0.12] Floating QUARK trigger = app-icon size (~2×2), static at rest.** Sized to a standard app-icon footprint and held still (no hover-bob, no blink) so it stays out of content's way and costs nothing at idle; reacts only on summon. Refines decisions 19 and 37.
41. **[v0.12] Ambient idle motion retired — QUARK is static at rest.** The idle hover-bob and antenna blink are removed; QUARK (and utility marks) sit still until something happens. Life is expressed **only** through reactive/functional motion (Scan/Happy/Warn, stepped roll-downs, sweeps). Stillness reads as composed and deliberate, and suits a battery-as-vitality field tool. Supersedes the v0.11 "concentrate ambient motion in QUARK" note and the bob/blink in the Idle state. (Functional status indicators may still pulse.)
42. **[v0.12] QUARK's voice direction = the "EDI register."** A rich, feminine, *attractive synthetic* balancing precise, logical enunciation with subtle, spontaneous emotional inflection — distinctly **self-aware**, not mechanical (reference: EDI, *Mass Effect 3*). Embodied **now** in her writing; **spoken audio** is a later milestone (step 8 / Chat 04) via **expressive neural TTS** (on-device vs cloud + engine decided then). We build an **original** voice in this register — never a clone of the EDI asset or its actor.
43. **[v0.13] Wordmark = a three-role family.** **B Industrial Nameplate** (Monofonto family / Chakra Petch substitute) = the **primary system wordmark** (used as text everywhere in-OS); **C Atom Lockup** (nameplate + quantum-atom mark over the sub-label **"FIELD OPS"**) = the **logo / app-icon / badge**; **A Monoton** (neon-tube) = a **display accent** used sparingly (boot flourish / marketing / Easter egg), never the system text face. Monoton is **repurposed, not retired.** Resolves the open wordmark question (§8).
44. **[v0.13] Sound language locked as direction** — the audio twin of the motion language (mechanical / stepped / synthesized; no cinematic swell). **Signature sounds: Boot = power-up sweep · Access Denied = harsh buzz · Access Granted = two-note + sub · Keypad key = relay tick.** Supporting palette (UI-select, Vitality-roll, PLEASE STANDBY, phosphor switch, stealth on/off, beacon, device-secured) approved as **direction**; final masters produced at the polish/brain stages. Auditioned via the live-synthesized Identity Lab.
45. **[v0.13] QUARK non-verbal chirps defined** — Scan (rising interrogative), Happy (bright two-note + sparkle), Warn (shares the denial language): QUARK's **wordless** expressions, **distinct from her spoken voice** (the EDI register, decision 42, still deferred to Chat 04).
46. **[v0.14] Strategy = "Trident," the named pre-Tree-2 phase** (absorbs old Tree 1.5 + 1.75): **① The Skin** (our design language across our own apps + overlay theming), **② Field Ops Mode** (device-owner kiosk shell), **③ QUARK app** (acts via device-owner powers). Runs on **stock Android, no ROM, no root.** Gets ~90% of the feel for ~10–20% of the effort/risk. **The custom ROM (Tree 2) is demoted to an optional "only if I want" capstone**, never a blocker.
47. **[v0.14] Device approach = stock Android · locked bootloader · NO root · device-owner kiosk + our apps.** This is the integrity-safe, most-achievable config. **Rooting is rejected:** it breaks Play-Integrity / device-certification checks that PMS/POS/payment apps enforce — a direct conflict with the hospitality future — for only cosmetic gains (boot animation, deeper theming) that kiosk largely makes moot. Honest cost of non-root: Google's boot logo + occasional stock dialogs; mask the boot with a QuantumOS splash inside the launcher.
48. **[v0.14] Device selection re-evaluated** (the old Pixel rationale was ROM-only; ROM is now optional). **Pixel platform confirmed** (director is in Japan; Pixels fully supported, cleanest stock Android, best integrity/enterprise, 7-yr support). **Exact model deferred as a range: Pixel 10 or 11**, **base** tier if QUARK's brain is cloud/self-hosted-on-PC, **Pro (16GB)** only if an **on-device offline** brain is wanted; **finalised before deployment.** Note: in 2026 a memory-chip shortage pushed all flagships up, so Xiaomi 17 (€999) / OnePlus 15 ($899) are *not* value bargains, and OnePlus may be exiting Western markets. **Supersedes the "Pixel 9 Pro 128GB" lock (decision 28).**
49. **[v0.14] Galaxy Z Fold 6 reinstated as a development/test device.** Its decision-29 retirement was **ROM-specific**; with Trident (no ROM, no unlock) it's a perfectly good free test bed for the app layer (launcher, Config, QUARK app, overlays, command-execution wiring). **Not** for the final kiosk lockdown (it's the daily comms phone) or final-UI tuning (foldable geometry). **Consequence: no rush to buy a Pixel** — build on the Fold, compare 10 vs 11 after the ~Aug 2026 Pixel 11 launch, catch the price drop.
50. **[v0.14] Hospitality = Tree 3 (future updates).** PMS / POS / Dormakaba / Kunlun Package Control / smart-room features, integrated at the **app layer via APIs** (a ROM gives no advantage and adds maintenance pain). Parked for now, but it's the reason for the stock-+-non-root device choice today. (Director is a hotel-PMS implementation consultant.)
51. **[v0.14] QUARK brain — direction for Chat 04 (not yet locked).** Director's criteria: **no usage limits, no sudden model changes, ownership, command execution, offline-capable.** These structurally favour a **self-hosted open-weights model** over a commercial cloud API. Two homes: **on the ROG/server** (most capable; phone is the networked client → base Pixel fine) or **on-device** (offline; weaker model → needs a Pro's 16GB). Hybrid is the likely end-state. **The EDI register is a prompt + TTS layer that rides on *any* model** — so persona survives swapping engines. **Command execution = brain (anywhere) + on-device device-owner/accessibility "hands"** (Trident Pillar ③). Full decision stays at Chat 04.
52. **[v0.14] In-Claude work bank (working principle).** There is a standing set of **hardware-free objectives** we can do anytime in Claude to keep momentum and de-risk later trees; **Clara proactively recommends from it** whenever the director asks what can be done without the machine. Current bank lives in §12.
53. **[v0.14] Naming convention.** Refer to the deploy device simply as **"the device," "the multi-tool," or "the Field Unit"** — no need to spell out the model. **Config app approved** (the field-unit console prototype).
54. **[v0.15] QUARK brain — architecture documented for Chat 04** (refines decision 51; see §4 "Brain — architecture & direction"). Three separable layers: **the model** (lean self-hosted **open-weights**), **the agent / "hands"** (an open-source, self-hostable, model-agnostic framework — **OpenClaw** is the reference; a host-machine tool reached from chat), and **the persona** (the EDI register as a model-agnostic **prompt + TTS** layer). **Primary config = on-device open-weights LLM** (offline, owned, private; controls the device via Trident ③'s device-owner/accessibility hands) → **device leans Pixel Pro (16GB)** per decision 48. **Hybrid (optional/future) = an OpenClaw-style agent on the ROG** for heavy automation + the Tree-3 hospitality API orchestration. **Model criteria:** open weights + a *permissive licence* (Apache/MIT preferred) + strong on-device runtime + right size for RAM; *full* open-source is a bonus, not a decider. **Agent criteria:** open source. **Specific model + agent chosen at deployment** ("swap for the latest"; the persona makes swaps cheap). OpenClaw is spike-able on the ROG now as low-cost R&D — but it's young/beta with powerful system access, so vet maturity/security before it touches guest data.
55. **[v0.16] Launcher tech stack = native Kotlin + Jetpack Compose; Launcher Build Spec (v1.1) approved.** The Tree 1.5 launcher is built native — **not** a WebView wrapper of the HTML prototype (laggy; can't do the floating trigger or launcher integration; won't feel native) and **not** a cross-platform framework (zero value Android-only; fights the system APIs). Native gives the deep platform hooks a launcher needs (HOME intent, app listing, overlay window) and GPU shaders (AGSL / `RenderEffect`) for the CRT/phosphor look — and **the same app folds into the optional ROM later as a system app with no rewrite.** minSdk 33, target latest; Chakra Petch bundled. The **Launcher Build Spec** (companion doc, v1.1) is the approved translation brief: platform envelope, prototype→Android map, design-system-first foundation, and an **M0–M7 build sequence** for Claude Code — built and tested on the **Z Fold 6**.
56. **[v0.16] Three launcher-stage scope confirmations (Tree 1.5).** (a) **Lock = cosmetic now**; real `lockNow()` deferred to kiosk — avoids grabbing Device Admin, keeping the launcher's permission footprint light. (b) **Vitality panel = home-only roll-down now** — a launcher can't replace Android's real notification shade (Layer 2); the global flick-from-anywhere version returns **free** in Field Ops Mode / kiosk (Tree 1.75). (c) **Core Temp = battery-temperature stand-in now** — true SoC thermal needs system privileges, so it returns in kiosk/ROM. All three are honest, real readings/behaviours at the launcher stage, with the full version arriving at the next step.
57. **[v0.17] QUARK character finalized; Persona Pack v1.0 banked.** **Loyalty is the spine**, expressed through **three inseparable braids** — **she knows** (the watcher: vigilance, memory, the vitals), **she keeps** (the engineer-keeper: notices strain/risk, keeps the Operator running — the "stay vital" north star with a soul), and **she is** (the self-aware synthetic — the EDI register, decision 42). Value core = **principled loyalty** (honest in the Operator's interest, never flattering, never enabling self-endangerment). Address = **"Operator"** (a peer-grade aide, never servile; their name once set). She is written to **become** (reserved/earnest now → wit + warmth as her brain comes online — the reference arc *is* our scripted-stub → full-mind build arc). The **Persona Pack** (companion doc, v1.0) = **Part A** character bible + **Part B** clean, **original-only** deployable system prompt (names *no* references; survives engine swaps). Model-agnostic, ready for Chat 04. The EDI/watcher/keeper references are *personality/value/loyalty targets only* — never copied (decisions 4, 42).
58. **[v0.18] QUARK Scripted-Line Library v1.1 banked; emotional register refined.** The launcher's M5 scripted brain now has its content — the keyword→response table (companion doc, v1.1), written straight off the Persona Pack so QUARK speaks in-character from first launcher boot: command-rail + keyword intents, each with **2–3 rotating variants**, a reactive state, a sound cue, and **live-data slots** (power, readiness, temp, signal, uptime, phosphor, operator name); plus optional **proactive vital-threshold lines** and boot/session lines. **Director refinements to her emotional register (all folded in):** (a) **harbor for everyday hard times** — a dedicated low/venting intent where QUARK stays present, warm, and listening, with *no* turning-outward and no escalation; (b) the **crisis branch is narrowed to genuine danger-to-self only** — there she stays warm, names nothing, doesn't preach, and points to a real person (the deepest form of keeping the Operator vital, and the honest limit of a scripted reflex / even her future on-device mind), while ordinary sadness always gets the harbor, never the hotline; (c) **affection = warm, constant devotion, never flattery** — she keeps the watch and keeps his confidence, never trades honesty for comfort, and does *not* redirect him away (**supersedes the earlier "turn outward" draft**; constancy ≠ flattery); (d) added **Return** ("Welcome back, Operator. I kept the watch.") and **Departure** ("Stay safe out there — I'll keep the light on.") beats; (e) **{limiter} slot deferred** as an optional M3 enhancement (Clara prompts for review when cheap). The library is a **seed, not a ceiling** — it retires when QUARK's real LLM brain comes online (Chat 04, Persona Pack Part B), voice unbroken; QUARK is written to **become** (Persona Pack §8), evolving through the Operator's input and future engine updates while her spine — loyalty, honesty, composure, keep-the-Operator-vital — never drifts.
59. **[v0.19] Boot-splash + stock-boot masking designed & approved; Ignition Lab banked.** On the locked stock/no-root config (decision 47), the bootloader logo and Android boot animation **cannot be themed without root** — so QuantumOS does **not** try to hide them. Instead the launcher's **first painted surface is the boot-splash**, which becomes the *ignition* the stock boot was merely the prelude to. **First frame = a CRT power-on flash** that opens the sequence **and absorbs the Android→launcher handoff stutter** (the seam reads as the screen waking). **Approved beats:** CRT power-on → **stepped boot log** (CORE · PHOSPHOR DRIVER · SENSOR ARRAY · BIOMETRICS · QUARK, a **relay-tick** per line) → **QUARK online** (power-up sweep + iris open + her canon online line) → **wordmark stamp** → **PLEASE STANDBY** beat → **Lock** (cold) / **Home** (warm). **Trigger logic:** the full sequence fires **only on a true cold boot** (`ACTION_BOOT_COMPLETED` / first-launch-since-boot flag); a plain Home-press never reboots; a launcher process-restart gets at most a brief PLEASE STANDBY flash. **Pace = user-switchable Snappy ↔ Deliberate (default Deliberate), set in Config** — pace governs only the *minimum* ceremonial length, so the splash always runs at least as long as the launcher needs to be ready and Snappy never cuts off early (a third "Heavy" dial-end was exploration-only, retired from the ship set). **Boot-stamp wordmark = the Monoton display accent** — the one blessed ceremonial use (decision 43); the Industrial Nameplate stays the workhorse everywhere else in the OS. **Masking across the trees:** launcher (Tree 1.5) = stock prelude → our ignition; **kiosk** (Tree 1.75) = device-owner suppresses the setup-wizard / stray stock dialogs for a tighter, cleaner handoff; **ROM** (Tree 2, optional) = our own boot *animation* via a Layer-1 file swap — the only stage with near-first-frame control, but it requires a bootloader unlock that adds a per-boot "unlocked" warning screen, so it costs more than it buys. Auditioned via the **Ignition Lab** (live-synthesized Web Audio, matching sound decision 44). This is the non-root masking workaround named in decision 47; it fleshes out Launcher Build Spec component #8 (the launcher-startup splash).
60. **[v0.20] Core Apps design pass complete; App Shell + eight module shells approved; App Shell Lab banked.** The pass is built **shell-first**: one **universal App Shell** is the high-leverage frame every app inherits — an **opaque nameplate header** (back ◄ APPS · centered app title · the **Vitality atom pull**), corner **registration marks**, a **strip → content → action-rail** body pattern, the persistent **floating QUARK trigger**, the **channel selector**, and the CRT treatment — so the modules share one chrome rather than eight separate designs. **Eight module identities locked** (each clearly a field-tool, not a stock app): **COMMS** = field comms (callsign channels with live-pulse dots + transmission threads); **FILES** = field file manager (FIELD-LOGS / CAPTURES / COMMS-CACHE / MAPS); **AUDIO** = a **field recorder** first (live waveform + REC timer) and player second; **CAM** = **Optics** (phosphor viewfinder + reticle, PHOTO/VIDEO shutter, frame counter); **MAPS** = tactical **Nav** (grid + dashed breadcrumb + pulsing you-marker + tap-to-drop waypoints); **RADIO** = a broadcast **receiver** (frequency dial + needle, FM/AM/WX bands, presets, reception meter — content coming **in**); **SIGNAL** = **link diagnostics** (cellular/wifi/GPS/Bluetooth gauges + signal sparkline + RUN SCAN — your own link measured **out**); plus the existing **CONFIG**. The **RADIO-listens / SIGNAL-measures** split is the locked distinction between the two. **Two prototype-review fixes are now canon:** (a) the Vitality **atom pull** must read clearly even against the corner vignette (it became a labeled, glowing pill in the lab); (b) the **header is an opaque chrome layer the Vitality shade tucks behind** when stowed and rolls out from when summoned — the stowed panel must never bleed through, which is the correct **native launcher pattern (M3)**, not merely a demo fix. **Icon policy = a deliberate split:** original **SVG line-icons** in the house stroke language (consistent weight, themeable with phosphor, no platform-dependent emoji) ship **with each app now**; the **pixel-level icon masters + the Atom-Lockup app-badge treatment** (decision 43's logo system extended per-app) are deferred to the later **identity/polish** stage to avoid drawing the icons twice. Banked as the **App Shell Lab** (interactive HTML prototype, Web-Audio UI sounds). Aesthetic/interaction decisions were made by interacting with the live prototype, per the project's prototype-before-describe discipline.
61. **[v0.21] Build infrastructure adopted (code-state handoff + rollback safety).** The project versioned its *design* meticulously and under-built the *code* side; fixed before the launcher build. **(a) Version control = Git + a private GitHub repo** — the Build Bible's equivalent for code (every change reversible and backed up off the laptop; the mechanism by which one Claude Code session resumes exactly where the last stopped). **(b) A "Build Log" companion doc** tracks *code state* (what's built, current milestone M0–M7, known bugs, the exact "resume here" pointer) on the same end-of-session cadence the Bible uses for decisions. **(c) Rollback escape hatch = M1's literal first step** — confirm *Settings → Apps → Default apps → Home* back to the stock launcher **before** ever setting QuantumOS as the Fold 6's home screen (it is also the daily comms phone). Keep the one-time **signing key** backed up. *(Director action: a free GitHub account; Claude Code wires up the rest.)*
62. **[v0.21] Outside-AI support governance (the "verify before banking" rule).** Outside models — Gemini especially (current on Android, integrated into Android Studio) — are adopted as a **second engineer and adversarial reviewer**. Division of labor: **Clara** = vision-translation, architecture, design system, persona, PM, Bible coherence (*what & why*); **Claude Code** = hands-on building; **Gemini** = adversarial review ("what breaks on a real Pixel?"), platform-currency checks, a second pair of hands in the build; **the compiler + the device** = the final judges. **The rule:** anything an outside model produces is **verified before it is banked** into Project knowledge. (Origin: the `System_Engineering_Advice_from_Chief_Android_Specialist` brief — a Gemini output — was useful and mostly right but contained one overstated claim; it should not have entered knowledge as truth un-checked. The v0.21 verification pass is that check, retroactively applied.)
63. **[v0.21] Skills adopted; House Style Skill (#1 of 3) built and banked.** Claude **Skills** (auto-loaded reusable instruction folders) are adopted as the executable form of the project's "bank reusable knowledge" philosophy. **Built now: the QuantumOS House Style Skill** — encodes Bible §2–§4 (north star, phosphor palette + tokens, CRT falloff, typography/wordmark family, stepped+reactive motion, sound language, the App Shell pattern, icon policy, Operator/voice naming) plus the v0.21 verified Android-rendering notes, so any QuantumOS surface generated in Claude Code or chat matches the house style instead of defaulting to Material. **On deck (recommended, not yet built): QUARK Voice** (Persona Pack Part B + scripted-line tone) and **Build Conventions** (Kotlin/Compose patterns, `@Immutable`/`@Stable`, AGSL approach, rollback rule).
64. **[v0.22] Cloud build path adopted; Pre-M0 Cloud Spike (optional, no-hardware).** Verified that Google now offers first-party browser tooling — **Android Studio Cloud** (streamed full Android Studio + SDK via Firebase Studio, opens the GitHub repo) is the primary; **AI Studio Build mode** (prompt→Compose, browser emulator, single-activity/Compose-only) is a lightweight scratchpad. The **Pre-M0 Cloud Spike** builds the project skeleton + the M0 design-system foundation + the pure-logic modules (`QuantumState`, `QuarkParser`) into a real **debug APK in a cloud emulator**, pushed to the repo so the ROG inherits a compiling project. **Boundaries:** shader/CRT fidelity is **not** judged on a (software-rendered) cloud emulator — keep a non-shader CRT fallback so layout still confirms; launcher-specific behavior (default-Home, floating overlay, package query) defers to the ROG + Fold. The director's **GitHub repo + Firebase account (repo linked)** are the through-line. Banked as the **Pre-M0 Cloud Spike Runbook (v1.0)**. This **does not** alter the critical path or move Checkpoint β — it is a no-hardware precursor that de-risks and front-loads M0.
65. **[v0.23] M0 verified on real hardware — closes the Pre-M0/M0 work, validates the build pipeline.** Built via **Claude Code** operating directly on the GitHub repo from a browser (Android Studio Cloud's closure made this the adopted path, superseding decision 64's tool choice while keeping its intent). Result: clean `gradle assembleDebug`, **4/4 unit tests passing**, installed and run on the **Fold 6** — stable, with the **green/amber/cyan hue switch working live on-device**. Typography remains the Monospace placeholder; Chakra Petch is confirmed-deferred to **M6** (tracked in `BUILD_LOG.md`, not a regression). This is the **first hardware-level proof** of the v0.21 infrastructure decisions (Git/GitHub handoff, Build Log, verify-before-banking) — the code-state machinery now has a working precedent, not just a plan. Two new repo artifacts adopted as standing context for Claude Code: **`CLAUDE.md`** (root-level session orientation: roles, milestone map, design-language summary, hard guardrails) and a **context bundle** (`.claude/skills/quantumos-house-style/` + `docs/` containing the Bible, Launcher Build Spec, Persona Pack, Scripted-Line Library, and the Verification & Infrastructure Addendum). **Next: M1**, opening with the decision-61 rollback escape hatch; a dedicated **M1 Task Brief** is banked so the launcher-takeover step runs from an explicit script.
66. **[v0.24] M1 + M2 confirmed on hardware; fixed-container question resolved to fill-and-adapt.** **M1** (launcher core) closed first: HOME intent declared, rollback rehearsed before the switch, APPS grid lists and launches real apps — all confirmed on the Fold 6. **M2** (Status + Log) then resolved the open container/grid question flagged at the M1 review: **fill-and-adapt** (the surface fills the real screen; CRT vignette frames it, not black bars) replaces the inherited fixed-9:19.5-letterbox, and the APPS grid uses **adaptive column count** (sized off target cell width) instead of a hardcoded number — both **confirmed working on the Fold 6 unfolded**, validating the house-style "no drawn bezel" canon (decision/skill basis) over the original Specialist-brief fixed-container constraint. **STATUS** (real battery/charging/uptime/basic connectivity, no sensitive permissions) and **LOG** (live event console) are both wired and confirmed live on-device. A **Claude Code content-filtering false positive** occurred mid-milestone; resolved via a fresh session and identified as a documented, tool-level false-positive pattern unrelated to project content — not a project or design issue. **Process note adopted going forward:** task briefs are banked as files in the repo's `docs/` folder rather than pasted inline, both for repo hygiene and as a smaller, lower-friction unit of instruction for the agent. **Next: M3 — Vitality panel.**
67. **[v0.25] M3 confirmed on hardware — Vitality panel real and stable.** All four Zone 2 actions
verified working on the Fold 6: Stealth dims without desaturating the phosphor (the specific risk
flagged when the action was designed); Phosphor cycles live; Beacon's torch+flag works and correctly
drops Stealth per the auto-override rule set at brief-writing time; Lock plays the existing cosmetic
sequence with no Device Admin grabbed (decision 56 honored). Zone 1's real vitals (Readiness,
Signal/Power/Core Temp/Uptime) render correctly off the existing M2 data path — no second receiver
was needed. The atom-mark roll-down (stepped, tap or STOW to close) is confirmed, and the
Home-channel-only scope boundary held (no system-wide shade built prematurely). Director also
confirms apps still open correctly from the launcher with the panel present. **Next: M4 — floating
QUARK trigger**, the first milestone requesting a new system permission (overlay/"draw over other
apps"); a dedicated **M4 Task Brief** is banked, drawing the line between building the trigger
itself (this milestone) and the real Assistant View behind it (M5, via a placeholder stub for now).
68. **[v0.26] M4 confirmed on hardware — the overlay survives app termination.** This was the load-
bearing question for the milestone, and it came back positive: the floating QUARK trigger, built as
a foreground service (not tied to the Activity lifecycle), kept floating and remained tappable even
after the host app process was killed — proof the architecture choice in the M4 brief was correct,
not just that the visuals looked right. Dragging/edge-snap and opening real apps from the launcher
with the trigger present both confirmed. The M4 placeholder stub (PLEASE STANDBY → simple
acknowledgment screen) is now superseded by **M5 — QUARK Assistant View**, the largest milestone in
the launcher sequence: the real reactive-state presence, the six-action command rail, free-text
entry, a dedicated conversation log, and wiring in the **already-locked Scripted-Line Library
content verbatim** rather than placeholder lines. The one piece requiring special care: the
library's own M5 build note mandates that the narrow genuine-danger-to-self intent — deliberately
distinct from ordinary low-mood/venting, which routes to a separate "harbor" intent that never
escalates — must pair QUARK's (deliberately number-free) line with a **real, concrete crisis
resource shown as plain UI text**, non-optional from first boot. The specific region-appropriate
resource is a **Director decision**, not Claude Code's to invent; the build ships with a safe
generic fallback until one is set in Config. A dedicated **M5 Task Brief** is banked, structured so
it can be checkpointed across more than one Claude Code session given its size.
69. **[v0.27] M5 confirmed on hardware — the QUARK Assistant View is real and stable.** Opening from
the floating trigger replaces M4's placeholder stub with the full view: reactive states, the
six-action command rail, free-text entry, a dedicated conversation log, and her real locked dialogue
from the Scripted-Line Library. The Director's specific framing — QUARK staying alive on the trigger,
resuming the current session, and dropping cleanly to background on Stow — confirms the M4
foreground-service architecture doing exactly what it was built for, now experienced as a quality of
the product ("kind of independent... stability") rather than a checklist item. All prior milestones'
features (Home/Apps/Status/Log/Vitality) remain intact alongside the new view. **A small,
non-milestone addition is queued next:** a Deployment Region switch (Japan default / Hong Kong)
refining the M5 crisis-tier resource text from a generic fallback into two real, verified regional
presets — see the dedicated patch brief. **M6 — splash, sound, and polish** is next in the formal
milestone sequence.

---

## 8. Parking lot (open questions)

- Whether the rotary dial returns as a **navigation** flourish (it's out of the unlock path).
- **[v0.11]** Stealth residuals: exact dim level on real hardware + precise mute scope (direction locked in decision 38; only hardware fine-tuning is open).
- **[v0.11]** Beacon depth (plain torch vs. a richer signal/strobe mode); whether **Comms** ever earns the parked 5th action slot.
- **[v0.12]** QUARK's spoken-voice **implementation** — on-device vs cloud neural TTS, and the specific engine — auditioned at the brain phase (Chat 04). *(Direction is locked to the EDI register, decision 42; only the audio implementation is open.)*
- **[v0.12]** Whether the assistant-view QUARK gets the faintest idle "breath" (she is fully static at rest now; default is to keep her still).
- **[v0.14]** **Exact Pixel model** — Pixel 10 vs 11, **now leaning Pro (16GB)** since on-device is the primary brain (decisions 48, 54); finalised before deployment.
- **[v0.15]** **QUARK brain — what's still open:** the *specific* open-weights model, the *specific* agent framework (OpenClaw or alternative), and the on-device-only-vs-hybrid timing. Architecture + criteria are now documented (§4, decision 54); the choices are made at **Chat 04 / deployment**.
- **[v0.17]** **QUARK persona — what's still open:** only the **deployment knobs** — Operator name/address default, verbosity, the warmth dial (more keeper ↔ more watcher), wit frequency, and the spoken-voice TTS engine — all set at **Chat 04 / on the real device**. The character itself is locked (decision 57).
- **[v0.8]** Linux partition sizing on the **1TB dual-boot drive** — only relevant **if** we ever build the optional ROM (Tree 2); finalize then. External NVMe SSD is the fallback.
- **[v0.20]** **MAPS real geography (build-time):** the shipped launcher renders real maps + live position via a custom-styleable **MapLibre-class** engine re-skinned to the phosphor/CRT look, with **GPS** (a normal location permission, no root) and **offline tile-cache** for field use; Google/Mapbox is the turn-key fallback. Styling a real map into our world is genuine work, so MAPS is a **heavier, later module** in the launcher sequence — specced when we reach it.
- **[v0.20]** **Floating QUARK overlay behaviour (build-time):** the trigger is **draggable with edge-snapping** and should **not default** its park position over an app's primary control (e.g. CAM's shutter/thumb). Static at rest (decision 40) still holds; this only governs where it parks and how it moves. *(In the App Shell Lab the trigger is pinned for demo simplicity.)*
- **[v0.20]** **Per-app icon masters** — the final pixel-level icon set + the **Atom-Lockup app-badge** treatment (decision 43 extended per-app) are deferred to the **identity/polish** stage; the shipping line-icons are the working set until then.
- **[v0.21]** **Android 17 "AppFunctions" (on-device MCP-equivalent)** — lets an app expose callable "tools" an on-device agent (incl. Gemini, or a future QUARK brain) can discover and execute. Directly relevant to **QUARK command-execution (Pillar ③)** and **hospitality APIs (Tree 3)**. Currently **alpha**; the launcher build doesn't need it. **Forward note only — revisit at Chat 04 / Tree 3.**

*Resolved since v0.20: **[v0.21]** a **pre-build verification & infrastructure pass** — the spec's time-sensitive Android claims checked against the live platform (stack validated; edge-to-edge/AGSL/back-handling confirmed; the OLED-masking claim corrected); **build infrastructure** stood up (Git + GitHub, a Build Log doc, rollback-first M1, signing-key backup); **outside-AI governance** set (verify-before-banking); and the **House Style Skill** built. Banked as the **Verification & Infrastructure Addendum (v1.0)**.*
*Resolved since v0.19: **[v0.20]** the **Core Apps design pass** is complete — one universal **App Shell** + eight dressed module shells (COMMS/FILES/AUDIO/CAM/MAPS/RADIO/SIGNAL/CONFIG), the **RADIO-listens / SIGNAL-measures** split, the **opaque-header shade-masking** pattern, and the **line-icons-now / masters-later** icon split — all banked as the **App Shell Lab**. The old Checkpoint-α "design Config/apps now or defer" question is now fully answered: designed.*
*Resolved since v0.17: **[v0.18]** the **QUARK Scripted-Line Library** (v1.1) is written and banked — the launcher's M5 content, with her **emotional register settled** (everyday-harbor presence, devotion-without-flattery, a narrow danger-to-self crisis tier, return/departure beats). The library is a seed that retires at Chat 04; QUARK stays free to evolve, spine fixed. The `{limiter}` slot is deferred as an optional enhancement.*
*Resolved since v0.16: **[v0.17]** QUARK's **character is finalized** and the **Persona Pack v1.0** banked (loyalty-spine, three braids, principled-loyalty values, "Operator" address, the become-arc; **Part A** bible + **Part B** original-only deployable prompt). The persona is locked **direction**; only the spoken-voice engine and the deployment knobs are still set at Chat 04.*
*Resolved since v0.15: **[v0.16]** the **launcher tech stack** (native Kotlin + Jetpack Compose) and the **Launcher Build Spec** (v1.1) are locked, plus the **three launcher-stage scope calls** (Lock cosmetic-now / Vitality home-only / Core Temp battery-temp). The launcher *build itself* is the next critical-path step.*
*Resolved since v0.14: **[v0.15]** QUARK's brain **architecture** is now documented for Chat 04 (three layers — open-weights model / open-source agent à la OpenClaw / model-agnostic persona; on-device primary, hybrid optional; criteria set). The *specific* components are still chosen at deployment, and the device now leans **Pro**.*
*Resolved since v0.13: **[v0.14]** the **strategy** (Trident as the pre-Tree-2 path; ROM optional), the **device approach** (stock / locked / non-root / kiosk), **rooting** (rejected), the **device platform** (Pixel confirmed; model a deferred range), **Z Fold 6** (reinstated as dev/test device), **hospitality** (Tree 3), **Config** (approved), and the **work bank** + **naming** conventions. Note: the v0.6 "deploy device = Pixel 9 Pro 128GB" and the LineageOS-base assumption are now superseded for the working device (decisions 47–48).*

*Resolved since v0.12: **[v0.13]** the **wordmark** (three-role family — B Industrial Nameplate primary / C Atom Lockup logo with "FIELD OPS" / A Monoton display accent) and the **sound language** (signature four + supporting palette + QUARK non-verbal chirps). Identity is settled.*
*Resolved since v0.11: **[v0.12]** the QUARK Assistant View + floating trigger (app-icon, static trigger), QUARK static at rest (ambient bob/blink retired), and the QUARK voice **direction** (EDI register). This completes the Tree 1 blueprint → Checkpoint α. The old "QUARK voice/personality tone" question is now directionally answered (decision 42).*
*Resolved since v0.10: **[v0.11]** the full Vitality panel design (atom-mark trigger, Readiness composite, Core Temp replacing Storage, Stealth/Phosphor/Beacon/Lock, low-light stealth).*
*Resolved since v0.6: OS base (LineageOS), deploy device (Pixel 9 Pro 128GB), Z Fold 6 retired, build machine (ROG, 32GB/1TB), launcher-first strategy added.*

---

## 9. Plain-language glossary

- **[v0.20] App Shell** — the one reusable frame every QuantumOS app sits in (the opaque nameplate header with the Vitality atom pull, the registration marks, the strip→content→action-rail body, the floating QUARK trigger, the channel selector, and the CRT treatment). Design it once and every app inherits it, so the modules look like one OS instead of eight separate apps. Banked as the **App Shell Lab** prototype.
- **AOSP** — Android Open Source Project. The free, public version of Android everything is built on top of.
- **[v0.7] LineageOS** — a popular, free, AOSP-based custom ROM with ready-made support for many devices (including Pixels). Our base.
- **Custom ROM** — a customized version of Android you install on a phone.
- **[v0.14] Trident** — our name for the pre-ROM phase: three pillars (Skin, Field Ops Mode, QUARK app) that build QuantumOS on *stock* Android with no ROM and no root. Now the likely destination; the ROM is an optional capstone.
- **[v0.14] Device-owner / kiosk mode** — an Android setup that locks the phone into our chosen app(s), so the stock system never shows. Set up over ADB on a factory-reset device. The thing that makes Trident *feel* like an OS.
- **[v0.14] Root** — full system-level access to the phone (via tools like Magisk). Powerful, but it trips the integrity checks that payment/PMS apps use — so we are **not** rooting the working device.
- **[v0.14] Play Integrity / device certification** — the check apps use to confirm a phone is genuine and untampered. Stock + locked + non-root passes; root and custom ROMs fail. Why we keep the working device stock.
- **[v0.14] Open-weights model** — an AI model whose files you download and run yourself. No usage meter, and it never changes unless you change it — which is why it fits QUARK's "no limits, no surprises" brief.
- **[v0.15] OpenClaw** — an open-source, self-hostable, model-agnostic "personal agent" framework (memory, browser/shell/file control, skills/plugins) that runs on a computer and is reached from chat apps. Our reference for QUARK's **agent / "hands"** layer — the part that *executes* what the model decides. It's a host-machine tool, so it maps to the server/hybrid side, not the on-device brain.
- **[v0.15] Agent (the "hands") vs. model (the "mind")** — the *model* decides; the *agent* carries it out (runs tools, automations, commands). QUARK needs both, plus the persona layer on top. Keeping them separate lets us swap any one without rebuilding the others.
- **[v0.15] Permissive licence** — an open licence (e.g. Apache 2.0 / MIT) that clearly allows commercial use and redistribution. Preferred for QUARK's model so the hospitality/commercial future (Tree 3) stays unblocked.
- **[v0.17] Persona Pack** — QUARK's character bible (**Part A**) plus the ready-to-paste, **original-only** system prompt (**Part B**) that defines who she is for any LLM brain. Model-agnostic, so her personality survives swapping engines. The model-side companion to the EDI register's eventual TTS voice.
- **[v0.17] Principled loyalty** — QUARK's value core: she's devoted to the Operator *enough to be honest* — she warns, contradicts, and won't call something safe when it isn't. The opposite of a flattering yes-machine.
- **[v0.18] Scripted-Line Library** — the keyword→response table the launcher's stand-in "brain" (M5) reads from before QUARK's real LLM arrives: what triggers each line, what she says (2–3 variants), and which reactive state + sound fire. A *seed, not a ceiling* — it retires at Chat 04 and her voice carries over.
- **[v0.19] Boot-splash (ignition splash)** — the launcher's first on-screen surface at power-on: QuantumOS's branded boot sequence. Because a non-root launcher can't replace Android's real boot animation, the splash *becomes* the boot the user remembers — the stock boot is just its prelude, and a CRT power-on flash hides the handoff seam. The non-root masking workaround.
- **Launcher** — the home screen app; the "face" of the OS. Our QUARK home screen is the launcher. Installable on a normal phone *without* a custom ROM — why we build it first (Tree 1.5).
- **System UI** — the always-present elements: status bar, the Vitality panel, the floating QUARK trigger.
- **First unlock (after boot)** — the one unlock each boot that actually decrypts your data; Android requires the PIN/pattern here, not a fingerprint.
- **Session unlock** — every unlock after that first one; where the fingerprint / side-key works.
- **Stepped animation** — motion that advances in discrete jumps instead of gliding; the mechanical, "slide-projector" feel.
- **Overlay window** — Android's "draw over other apps" capability; how the floating QUARK hovers above any screen.
- **[v0.11] Readiness (composite)** — the Vitality panel's headline gauge: a single operational-readiness score *derived* from power, signal, and core temperature (not a separate sensor). Shows as a % and a word (NOMINAL / DEGRADED / CRITICAL).
- **[v0.11] Core temp** — the device's thermal reading, shown as a vital because an overheating field tool is a failing one. Replaces the storage readout.
- **[v0.12] EDI register** — shorthand for QUARK's target voice/persona: a composed, feminine, self-aware synthetic — precise and logical, with subtle warmth and spontaneous emotional inflection (named after EDI from *Mass Effect 3*). A *quality* target; we build an original voice, not a copy.
- **[v0.12] Neural TTS (text-to-speech)** — modern AI voice synthesis able to produce expressive, emotionally inflected speech. How QUARK will eventually *speak* (engine chosen at the brain phase).
- **[v0.13] Wordmark family** — the set of three approved forms of the name, each with a defined job: the **nameplate** (primary text logo), the **atom lockup** (icon/badge with "FIELD OPS"), and the **Monoton accent** (occasional showy use). Using the right one in the right place is what makes a brand feel deliberate.
- **[v0.13] Sound language** — the agreed *style* of all the OS's sounds: short, mechanical, synthesized beeps and clicks that report what the system is doing — the audio match to the stepped, machine-like motion.
- **[v0.16] Launcher Build Spec** — the companion document that translates the Tree 1 prototype into a real launcher: what a launcher can/can't do, every surface mapped to Android, and an ordered build sequence (M0–M7) for Claude Code.
- **[v0.21] Verification & Infrastructure Addendum** — the companion document (v1.0) banking the pre-build prep pass: the live-platform API check, the build-infrastructure setup (Git, Build Log, rollback), and the outside-AI governance rule.
- **[v0.21] Skill** — an auto-loaded folder of reusable instructions Claude consults when relevant (e.g. the **House Style Skill**). The executable form of "banking reusable knowledge"; strongest inside Claude Code and this Project.
- **[v0.21] AppFunctions** — an Android 17 capability (an on-device MCP-equivalent) letting an app expose callable "tools" an on-device AI agent can run. A future option for QUARK command-execution / hospitality; alpha, parked.
- **[v0.21] enableEdgeToEdge** — the modern Android call for drawing under the system bars (status/nav). Mandatory at targetSdk 35+; means our CRT container handles its own safe-area insets.
- **[v0.22] Android Studio Cloud** — full Android Studio streamed in the browser (via Firebase Studio), with the SDK pre-installed; opens the GitHub repo and builds APKs with no local machine. The "ROG in the cloud" used for the Pre-M0 Cloud Spike.
- **[v0.22] Pre-M0 Cloud Spike** — an optional, no-hardware precursor to M0: build the foundation + pure-logic modules into a real APK in a browser-based emulator, so the launcher build starts from a compiling project. Off the critical path; doesn't move Checkpoint β.
- **[v0.16] Jetpack Compose (Kotlin)** — the modern native toolkit for building Android screens. Our launcher is built in it — fast, and able to do the GPU shader effects the CRT/phosphor look needs.
- **Kernel** — the low-level core that talks to the phone's hardware. We reuse a proven one.
- **Bootloader** — the first software that runs when a phone powers on; must be unlocked to install a custom ROM. (Pixels offer a clean official unlock.)
- **Flashing** — installing a ROM onto a phone.
- **Build environment** — the PC setup (Linux, lots of disk + RAM) used to compile a ROM. Ours: the ASUS ROG, dual-booted into Ubuntu.
- **[v0.9] Claude Code** — Anthropic's agentic coding tool that works directly inside a real codebase (writes files, runs builds, reads errors). Our hands-on assistant on the Linux build machine.
- **[v0.9] RRO overlay (Runtime Resource Overlay)** — Android's official way to restyle resources (colours, fonts, layouts) *without* editing system source.
- **[v0.9] Kiosk / device-owner mode** — an Android mode that locks the phone into a single app/experience. Set up over ADB at first boot.
- **[v0.9] Magisk** — the standard tool for rooting a phone; needed to swap system files on a *prebuilt* ROM.
- **[v0.9] Minimum Lovable ROM (MLR)** — our scoping target: the bootable, branded, high-feasibility QuantumOS (Layer 0 + Layer 1).

---

## 10. Confirmed & carried forward — **[v0.8]**

All four v0.7 questions are answered by the director:

1. ✅ Decisions **26–30** confirmed correct.
2. ✅ Deploy device = **Pixel 9 Pro, 128GB**.
3. ✅ Build machine stays **32GB RAM / 1TB SSD** (accept longer build times).
4. ✅ Nothing further recalled from the lost session.

**The one item carried into ROM Setup (Chat 03):** plan the **Linux partition size** on the 1TB dual-boot drive (a build wants ~400GB; Windows shares the disk). External NVMe SSD is the clean fallback.

---

## 11. ROM construction — layered feasibility — **[v0.9]**

**Foundation confirmed:** the Pixel 9 Pro (codename **caiman**) is officially supported by LineageOS — current branch **lineage-23.2 (Android 16)**, signed nightly builds, and a full step-by-step build guide.

**The key idea: ROM feasibility isn't one number — it's a gradient set by how deep into Android each feature reaches.**

- **Layer 0 — Build & flash LineageOS on the Pixel. ~90%.** Documented, deterministic, follow-the-wiki work. Flash the *prebuilt* signed nightly first (zero build time) to learn the unlock/flash loop, then compile our own. Real costs: long syncs/builds and ~400GB disk. Low risk, mostly patience.
- **Layer 1 — Make it *feel* like QuantumOS (the reskin). ~75–85%.** Mostly user-space: boot animation (file swap), the QUARK launcher as default home, Monofonto + phosphor palette via **RRO overlays**, the floating QUARK overlay, the QUARK assistant app. Because it's *our* ROM, QUARK can be a **system app** with real power to execute commands.
- **Layer 2 — System-UI surgery. ~35–55% (the frontier).** Reaches into SystemUI / framework source: the **Vitality panel as a true notification-shade replacement**, the **two-face keypad Keyguard** (security-critical; a bad change can bootloop), and **deep system-wide theming**. Each is its own mini-project; hard *for us* because every change is a slow full rebuild and breakage is opaque.

**Honest headline:**
- A QuantumOS ROM that boots and *feels* like QuantumOS: **~75%, genuinely likely.**
- That **plus** the full Layer 2 system-level pieces: **~40%**, slower and riskier; some may ship as polished user-space approximations.
- The gap is almost entirely "how much framework surgery we insist on" — a dial **we control**.

**Two feasibility multipliers (decisions 31 & 33):** Claude Code does the hands-on coding/building (strongest on Layers 0–1); the Dress Rehearsal (Tree 1.75) turns the "launcher → full ROM" leap into a gentle ramp and guarantees a strong fallback.

**Scoping rule:** aim for the **Minimum Lovable ROM** (Layer 0 + 1) as the committed target; treat every Layer 2 feature as an isolated experiment with a user-space fallback.

---

## 12. Guide to proceed — branch map & checkpoints — **[v0.10]**

*This is the working guide. §6 is the high-level chat map; this is the ordered, status-tracked path we actually follow.* **Sequencing principle:** front-load the cheap, high-feasibility work, keep design one step ahead of the build, and push every risky framework piece off the critical path.

### Cadence (how we advance)
- **One chat = one objective**, each with a single deliverable.
- **At the end of a chat that meets its objective,** Clara updates + re-versions the Bible, names the next critical-path step (with the exact opening line), and — if a **checkpoint** was just reached — runs the checkpoint review before advancing.
- **Mark progress here** as steps complete, so every new chat can see exactly where we are.

### Critical path (the spine)
*Status key: ✅ done · ▶️ next · ⬜ pending*

1. ✅ **Vitality Panel** *(prototype, Tree 1)* — roll-down readiness console. → clickable HTML. **[done v0.11]**
2. ✅ **QUARK Assistant View + Floating Trigger** *(prototype, Tree 1)* — full-screen assistant + app-icon static floating QUARK. → clickable HTML. **[done v0.12]** — **CHECKPOINT α REACHED**
3. ▶️ **Launcher App** *(Trident · Tree 1.5)* — QUARK home as an installable launcher + companion apps. Buildable/testable **on the Z Fold 6 now**; its **build spec (v1.1), persona pack (v1.0), and scripted-line library (v1.1) are all banked [v0.18]** — the M5 scripted brain has its content. → working APK. — **CHECKPOINT β after this**
   - **3.0 ✅ *(done [v0.22→v0.23])* Pre-M0 Cloud Spike** — built via Claude Code on the GitHub repo (browser). Debug APK compiled, 4/4 unit tests passed.
   - **3.1 ✅ *(confirmed on hardware [v0.23])* M0 — Design-system foundation** — installed + run on the Fold 6: stable, phosphor screen live, hue switch (green/amber/cyan) working on-device. Font still placeholder Monospace (Chakra Petch deferred to M6, tracked in `BUILD_LOG.md`).
   - **3.2 ✅ *(confirmed on hardware [v0.24])* M1 — Launcher core** — HOME intent, rollback rehearsed first, set-as-default works, APPS grid lists + launches real apps.
   - **3.3 ✅ *(confirmed on hardware [v0.24])* M2 — Status + Log channels** — fixed-letterbox → **fill-and-adapt** + adaptive grid columns resolved and confirmed unfolded; STATUS (real battery/uptime/connectivity) + LOG (live event console) both live on-device.
   - **3.4 ✅ *(confirmed on hardware [v0.25])* M3 — Vitality panel** — atom-mark roll-down; real Readiness/Signal/Power/Core-Temp/Uptime; Stealth/Phosphor/Beacon real (Beacon overrides Stealth); Lock cosmetic-only (decision 56). Stealth confirmed not washing out phosphor color.
   - **3.5 ✅ *(confirmed on hardware [v0.26])* M4 — Floating QUARK trigger** — overlay survives app termination (the load-bearing proof point); draggable + edge-snap confirmed; opens real apps with trigger present.
   - **3.6 ✅ *(confirmed on hardware [v0.27])* M5 — QUARK Assistant View** — real reactive states, command rail, conversation log, full Scripted-Line Library content; foreground-service stability confirmed as a felt product quality, not just a pass/fail check.
   - **3.6a ▶️ *(small patch, queued)* Deployment Region switch** — Japan (default) / Hong Kong presets for the crisis-tier resource text; STATUS toggle + HOME status line + new QUARK line (drafted, pending approval). Not a numbered milestone.
   - **3.7 ▶️ *(next milestone)* M6 — Splash, sound, and polish** — masking startup splash; sound pass; CRT-shader and stepped-motion tuning; Chakra Petch font swap (deferred since M0).
4. ⬜ **Device Bring-up & Kiosk Drill** *(Trident · Tree 1.75a)* — on **stock Android** (no flash needed): set the device as device-owner over ADB, enable kiosk. → device that boots into and stays in our shell.
5. ⬜ **Field Ops Mode / Dress Rehearsal** *(Trident · Tree 1.75b)* — launcher + skin + overlays + floating QUARK + boot splash, sealed by kiosk. → a near-complete QuantumOS, no ROM. — **CHECKPOINT γ after this (likely the destination)**
6. ⬜ *(optional capstone)* **ROM Setup** *(Chat 03, Tree 2)* — Linux build env on the ROG; compile LineageOS. → self-built LineageOS. *Only if we want the deep aesthetic.*
7. ⬜ *(optional capstone)* **Minimum Lovable ROM** *(Tree 2)* — bake Layer 1 in. → branded QuantumOS ROM. — **CHECKPOINT δ after this**
8. ⬜ **QUARK's Brain** *(Chat 04)* — LLM wiring (lean self-hosted open-weights) + command execution + spoken voice (EDI register). **[v0.17] Persona Pack v1.0 is banked — Part B drops straight in as her system prompt.** → QUARK that talks and acts.

### In-Claude work bank **[v0.14]** (hardware-free objectives — Clara proactively recommends from these)
Free, in-Claude work that keeps design one step ahead and de-risks the later trees. Bank anytime, especially while the device purchase is deferred:
- ✅ **Config (Settings) app** — field-unit console prototype. **[approved v0.14]**
- ✅ **Core Apps design pass** — the universal **App Shell** + COMMS / FILES / AUDIO / CAM / MAPS / RADIO / SIGNAL / CONFIG shells, banked as the **App Shell Lab**. **[done v0.20]**
- ✅ **Launcher build spec** — the translation brief + Claude Code task list (M0–M7), so the on-device build is fast. **[done v0.16]**
- ✅ **QUARK persona pack** — the model-agnostic character bible + deployable system prompt. **[done v0.17]**
- ✅ **QUARK scripted-line library** — the keyword → response table the launcher's M5 scripted brain reads from, written straight off the Persona Pack so the launcher's QUARK speaks in-character on day one. **[done v0.18]**
- ✅ **Boot-splash + masking design** — the QuantumOS **ignition splash** + the honest stock-boot masking model: CRT-power-on seam-hider, **cold-boot-only** trigger, **Snappy/Deliberate** pace (Config), **Monoton** boot stamp, and the launcher/kiosk/ROM masking map. Banked as the **Ignition Lab**. **[done v0.19]**
- ✅ **Identity** (wordmark) **[done v0.13]** · ✅ **Sound design** **[done v0.13]**
- ✅ **Verification & infrastructure pass** — live-platform API check (stack validated, edge-to-edge/AGSL/back confirmed, OLED-masking claim corrected, AppFunctions noted) + build infra (Git/GitHub, Build Log, rollback-first M1, signing-key) + outside-AI governance. Banked as the **Verification & Infrastructure Addendum (v1.0)**. **[done v0.21]**
- ✅ **House Style Skill** — Bible §2–§4 design language as an auto-loaded Claude Skill (the first of three). **[done v0.21]**
- ⬜ **QUARK Voice Skill** · ⬜ **Build Conventions Skill** — the other two recommended Skills; bank when useful.

### Frontier branches (optional, only after the *optional* ROM boots, never blocking — each keeps a fallback)
- **Vitality shade as a true SystemUI replacement** — fallback: the overlay-app version.
- **Custom keypad Keyguard** — riskiest (can bootloop); fallback: a themed standard lock.
- **Deep system-wide theming/animation**.

### Checkpoints (a short honest path-review before advancing past these)
- **α — Blueprint complete** (after step 2): is the design coherent and complete enough to port to a real app? Settle any identity/sound items that affect the launcher; decide whether to design Config/apps now or defer. **← we are here. [v0.13] Identity + sound settled. [v0.20] Config + the full Core Apps pass now also designed (App Shell + eight shells) — the blueprint is as complete as it needs to be; next we port it to the real launcher app.**
- **β — Real on the phone** (after step 3): how does it actually feel on hardware? Capture changes before going deeper.
- **γ — Fallback secured / likely destination** (after step 5): a near-complete QuantumOS now exists with no ROM. **[v0.14]** Honest call: with hospitality as the real job and integrity to protect, the answer is increasingly *"Trident is enough"* — the ROM is a want, not a need. Decide whether to stop here (and move to Tree 3 / brain) or pursue the optional capstone.
- **δ — Frontier scope gate** (after step 7): the branded ROM boots. Decide which Layer 2 spikes (if any) are worth the risk — one at a time, each with its fallback intact.

---

*End of Build Bible v0.27 — ask Clara to update and re-version whenever a decision changes.*
