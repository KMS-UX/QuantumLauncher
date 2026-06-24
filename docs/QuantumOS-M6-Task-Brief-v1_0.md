# QuantumOS — M6 Task Brief: Splash, Sound, and Polish
*For Claude Code. Companion to the Launcher Build Spec (v1.1), `CLAUDE.md`, the M1–M5 Task Briefs,
and the Deployment Region Patch. Version 1.0.*

> Read `CLAUDE.md` first if this is a new session. M1–M5 are closed and confirmed on hardware. **This
> milestone also includes the Deployment Region patch** (`docs/QuantumOS-Patch-DeploymentRegion-v1_0.md`)
> — do that first if it isn't done yet; it's small and unrelated to the rest of this brief. **This is
> another large milestone — checkpoint `BUILD_LOG.md` and split across sessions if needed,** same as M5.

## Goal
Replace every placeholder left over from earlier milestones with the real thing: real fonts, the
real boot-splash ceremony, real AGSL CRT shaders (judged on actual hardware for the first time),
and a real synthesized sound pass — plus two small settings that need to actually persist now.

---

## Step 0 — Deployment Region patch
If not already done, complete `docs/QuantumOS-Patch-DeploymentRegion-v1_0.md` now. **One addition
to that patch's original scope:** the Deployment Region choice must **persist across app restarts**
(simple local storage — `SharedPreferences` is enough, no need for anything heavier). It was
specified as a runtime toggle only; this milestone is where settings start needing to survive a
real cold boot, so close that gap here.

**Verify:** switch the region, fully close and reopen the app, confirm it's remembered.

## Step 1 — Real fonts
- Bundle **Chakra Petch** (Google Font, `.ttf` in `res/font/`) and replace `FontFamily.Monospace`
  everywhere it's currently used as a placeholder (the terminal readout, STATUS, LOG, the Assistant
  View) — this has been flagged as deferred since M0; this is where it gets resolved.
- Bundle **Monoton** for **one place only**: the boot-splash wordmark stamp (Step 3). Never use it
  as a system or body text face — that's Chakra Petch's job everywhere else.

**Verify:** after the swap, re-check the APPS grid and STATUS/LOG text — Chakra Petch's character
widths differ from Monospace, so confirm nothing wraps oddly or breaks the adaptive grid.

## Step 2 — Boot Pace toggle (STATUS)
- Add a tap-to-cycle `BOOT PACE: DELIBERATE / SNAPPY` row to STATUS, same interaction pattern as
  the existing Cycle-phosphor and Deployment Region rows. **Default Deliberate** — this is the
  shipped default; the current hardcoded `BootPace.SNAPPY` in the ViewModel was a dev-only
  convenience and should be replaced by this real setting.
- **This must also persist** across restarts, same as Step 0's region setting — handle both in the
  same small persistence pass rather than two separate mechanisms.

**Verify:** switch pace, restart the app, confirm boot actually runs at the chosen speed and the
choice survived the restart.

## Step 3 — The boot-splash sequence
The cold-boot logic (`executeColdBootSequence()`) has existed since M0 — this step makes it the
real, full-screen ceremony rather than background log lines:

- **CRT power-on** flash, opening the sequence.
- **Stepped boot log** (CORE → PHOSPHOR DRIVER → SENSOR ARRAY → BIOMETRICS → QUARK), each step
  paired with the relay-tick sound from Step 4.
- **QUARK online** — the power-up sweep sound, her iris visibly opening, and her canon online line
  from the Scripted-Line Library §6 (rotate its 2 variants), filled with real live data
  (power/signal/readiness/operator) at boot time.
- **Wordmark stamp** — Monoton, the one ceremonial use from Step 1.
- **PLEASE STANDBY**, resolving to **Home**.
- **Trigger discipline (already correct by construction — verify, don't rebuild):** this fires only
  on a true cold boot. A plain Home-press just resumes the existing activity/ViewModel and the
  engine's own guard (`bootLifecycle != UNINITIALIZED`) skips it — confirm this holds by force-
  stopping the app and relaunching (true process restart) vs. just pressing Home (should never replay).

**One spec nuance to flag, not resolve:** the original decision text mentions the sequence
resolving to "**Lock (cold) / Home (warm)**" — the exact intended meaning of "cold" vs "warm" here
relative to the Lock state isn't fully unambiguous. **Resolve to Home in all cases for this
milestone** and note the open question in `BUILD_LOG.md` rather than guessing at a persisted-lock
mechanic that isn't clearly specified.

**Verify:** a true cold boot (device restart, or force-stop + relaunch) plays the full sequence
described above and ends on Home; a plain Home-press from another app never replays it.

## Step 4 — Sound pass (synthesized, not "final masters")
**Scope this honestly:** implement **procedurally synthesized** cues — the same spirit as the
Web-Audio synthesis already used in the HTML prototypes — not professionally produced audio files.
That's a future identity/polish refinement if ever wanted, not this milestone's job.

- The engine has been emitting audio-cue tokens (`SND_POWER_UP_SWEEP`, etc.) since M0 — **this is
  the first milestone where they actually need to produce sound.** Wire playback for all of them.
- Cover the **four signature sounds**: boot/power-up sweep, access-denied buzz, access-granted
  two-note + sub, keypad relay tick.
- Cover the **supporting cues** needed by features already built: UI-select clunk, the
  phosphor-retune sweep (used by Cycle Phosphor and reused by the Deployment Region patch),
  stealth down/up, beacon blip ×3, device-secured latch, the PLEASE STANDBY processing pulse, and
  QUARK's three chirps (scan/happy/warn).

**Verify:** trigger a sample of these (boot, lock, beacon, a QUARK chirp) and confirm each produces
a distinct, audible cue — not silence, not a placeholder beep standing in for all of them.

## Step 5 — Real CRT shaders
Replace the cheap non-shader scanline/vignette overlay (the cloud-spike-era fallback, used because
the cloud emulator couldn't reliably render shaders) with real **AGSL** (`RuntimeShader`/
`RenderEffect`, API 33+ — already covered by minSdk) scanline, vignette, phosphor-glow, and flicker
effects. **Keep the existing cheap overlay as an automatic fallback** if shader compilation ever
fails, rather than deleting the safety net.

**Verify:** this is the first time the real CRT look gets judged on actual hardware rather than
argued about in chat or seen through a software-rendered emulator — look at it on the Fold 6 and
confirm it actually reads as the phosphor look we've been designing toward.

## Step 6 — Stepped-motion tuning
A lighter pass: review existing transitions (boot steps, the Vitality panel roll-down, hue
switches) for a consistently mechanical, stepped cadence, and tune timing constants for feel. This
is tuning what already exists, not new construction.

## Step 7 — Session close
Update `BUILD_LOG.md`: mark items done/not-done, note the open "Lock (cold)" question from Step 3
for later, confirm both new STATUS settings persist correctly, and write the "resume here" line
pointing at **M7 — ship + field-test**.

---

## Hard stops — do not do these in M6
- **Do not** try to source or produce professionally mastered audio — synthesized cues are the
  correct scope now.
- **Do not** try to resolve the "Lock (cold)" boot-resolution nuance — flag it, resolve to Home, move on.
- **Do not** use Monoton anywhere except the single wordmark-stamp moment.
- **Do not** touch M7 (signing/shipping) scope.

## What "M6 done" looks like to the Director
A true cold boot (restart the phone, or force-stop and relaunch) plays the full ceremony — real
fonts, the real CRT shader look, real synthesized sound, QUARK's online line with live data — and
resolves to Home. A plain Home-press never replays it. STATUS now has working, persistent Boot
Pace and Deployment Region toggles. Everything built in M0–M5 still works correctly with the new
fonts and shaders in place.

---
*End of M6 Task Brief v1.0. Report back to Clara per the Step 0/3/5 checkpoints — particularly how
the real CRT shader actually looks on the Fold 6 — before the Bible is bumped to mark M6 closed.*
