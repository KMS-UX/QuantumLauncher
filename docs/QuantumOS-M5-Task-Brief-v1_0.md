# QuantumOS — M5 Task Brief: QUARK Assistant View
*For Claude Code. Companion to the Launcher Build Spec (v1.1), `CLAUDE.md`, the QUARK Scripted-Line
Library (v1.1), and the M1–M4 Task Briefs. Version 1.0.*

> Read `CLAUDE.md` first if this is a new session. M1–M4 are closed and confirmed on hardware,
> including the floating trigger surviving app termination. **This is the largest milestone in the
> sequence — it's fine to checkpoint `BUILD_LOG.md` partway through and finish in a later session
> rather than rushing it into one sitting.** Work the steps in order; verify each one.

## Goal
Replace M4's placeholder stub with the real QUARK Assistant View: her four reactive states, a
six-action command rail, free-text entry, a scrolling conversation log, and — critically — her
**real, locked dialogue** from the Scripted-Line Library, wired in verbatim rather than the
placeholder lines used since M0.

## Read this before writing any dialogue logic
**`docs/QUARK-Scripted-Line-Library-v1_1.md` is the source of every line QUARK speaks in this
milestone.** Do not invent new lines, do not paraphrase her existing ones, and do not write
dialogue from scratch even for small gaps — if something seems missing, note it in `BUILD_LOG.md`
and ask, rather than improvising in her voice.

---

## Step 0 — Wire the real Scripted-Line Library content
Replace every placeholder line currently in `ScriptedLineLibrary` (in `QuantumState.kt`) with the
actual content from the library doc:

- The six command-rail intents (§3.1–3.6): Status report, Engage stealth, Cycle phosphor, Light
  beacon, Say something, Trigger warn.
- The keyword free-text intents (§4): Greeting/wake, Return/welcome-back, Hard time/harbor,
  Distress/crisis (see Step 1 — handle this one with care), Fallback.
- Boot/session lines (§6): assistant-opened, assistant-stowed.
- Each intent has **2–3 rotating variants** — implement "don't repeat the last variant used for
  this intent in this session" exactly as the doc specifies (§0.3).
- Fill `{slot}` tokens (power, readiness, temp, signal, uptime, phosphor, operator) from the real
  engine state already wired since M2/M3 — no new sensors needed.

**Verify:** trigger the same intent twice in a row and confirm you get two different variants, not a repeat.

## Step 1 — The crisis-tier safety requirement (non-negotiable, read carefully)
The library deliberately separates two things that must **stay** separate:

- **Hard time / harbor** — everyday low mood, stress, loneliness. QUARK stays present and warm.
  **Idle only. No Warn. No sound effect. No escalation, no redirect.**
- **Distress / crisis** — genuine danger-to-self only, matched narrowly. **Also Idle only — never
  Warn-red/shake, never a sound effect.** Her line here is deliberately free of any specific number.

**The library's own build note for this milestone is a hard requirement, not a suggestion:**
when the Distress/crisis intent fires, the screen must show a **real, concrete crisis resource as
plain UI text beneath her line** — not spoken by her, not part of her scripted dialogue, just
visible. This ships from first boot; it is not optional and not deferrable.

- Implement this resource as a **Config-settable string, empty by default.**
- If it's empty, show a **safe generic fallback** (pointing to local emergency services or a trusted
  person) rather than nothing — the feature must work even before a specific resource is configured.
- **Do not pick a specific number or hotline yourself.** Which real, region-appropriate resource to
  use is a Director decision — leave it as the empty/fallback state and note in `BUILD_LOG.md` that
  it's waiting on that input.
- Match the Distress/crisis trigger narrowly, exactly as the library describes — everyday sadness
  must keep routing to the harbor intent above, never here.

**Verify:** trigger the harbor intent — confirm Idle, no sound, no escalation. Separately, trigger
the Distress/crisis intent once — confirm Idle (not Warn), no sound, her line appears, and the
fallback resource text renders beneath it since none is configured yet.

## Step 2 — The central presence + reactive states
- Scale up the placeholder mark from M4 into a larger central presence on this full-screen view.
- Implement all four reactive states:
  - **Idle** — static, neutral, antenna still.
  - **Scan** — iris contracts, a scan-line sweep, sound-rings pulse.
  - **Happy** — hop/tilt, iris pulse, sound-rings.
  - **Warn** — turns `--warn` red, shakes, alert iris. (Used for real alerts and the Trigger-warn
    drill in Step 4 — **never** for Distress/crisis or Hard-time, per Step 1.)
- Add a one-line state caption (e.g. "SCANNING…", "STANDING BY") distinct from the actual response
  text in the conversation log.
- These are short, discrete, triggered bursts — consistent with the existing stepped/static-at-rest
  motion language, not a new exception to it.

**Verify:** each of the four states is visually distinct and fires at the right moment (Scan on
open/processing, Happy/Warn/Idle per the intents above).

## Step 3 — The conversation log (new — separate from the LOG channel)
- Add a **new** state list to the engine — e.g. `conversationLog: StateFlow<List<ConversationEntry>>`
  — distinct from `systemLogs` (which stays the general event console used by M2's LOG channel).
  Don't conflate the two; they serve different purposes.
- Each entry: what triggered it (a rail-button label, or the user's typed text) + QUARK's resulting
  line + a timestamp.
- Render as a scrolling list, most-recent visible, in the existing console aesthetic — monospace,
  phosphor color, no Material chat bubbles or cards. A simple prefix (e.g. `>`) to distinguish user
  input from QUARK's reply is fine; stay within the phosphor palette, no new colors.

**Verify:** every rail tap and every typed exchange appears in this log; the LOG channel (M2) is unaffected.

## Step 4 — The command rail (six actions)
In order: **Status report · Engage stealth · Cycle phosphor · Light beacon · Say something · Trigger warn.**

- **Status report, Engage stealth, Cycle phosphor, Light beacon** — call the **existing** functions
  already built in M0–M3. Do not duplicate this logic; reuse it directly, including Beacon's
  existing auto-drop-Stealth rule.
- **Say something** — new: QUARK volunteers one of the §3.5 lines (rotate widely, per the doc). No
  state change, no permission, just presence.
- **Trigger warn** — new: fires the real **Warn** state with the §3.6 drill line, which explicitly
  acknowledges it's a test. Never implies a real threat.
- Every rail action logs into the conversation log from Step 3.

**Verify:** all six buttons work; the four reused ones behave identically to their M3 originals (no
regressions); the two new ones match their documented behavior.

## Step 5 — Free-text entry
- A text field whose submissions route through the **existing** `QuarkParser.parseInput(...)`,
  extended to also match the Step 0/1 keyword categories (greeting, return, harbor, crisis,
  fallback) alongside what it already handles.
- Match generously, longest/most-specific intent wins, exactly as the library's §0 logic describes.
- Every exchange lands in the conversation log too.

**Verify:** a handful of varied test phrases each land on a sensible intent; something nonsensical
correctly falls through to Fallback rather than erroring or going silent.

## Step 6 — Boot/session lines
- "Assistant opened" (Scan → Idle) fires when this view opens via the M4 trigger — this is what
  replaces M4's placeholder stub.
- "Assistant stowed" (Idle) fires on close/back.
- The boot-complete "Online" line is a nice-to-have hook into the existing M0 boot sequence if it's
  a quick fit — not required for M5 if it isn't; note either way in `BUILD_LOG.md`.

**Verify:** opening and closing the assistant plays the right line each time.

## Step 7 — Confirm theme + Stealth carry over
- Phosphor hue should recolor this screen live automatically (same token source as everywhere else)
  — verify, don't rebuild.
- **Check carefully:** if this view is a separate Activity (as M4's stub was), Stealth's window-level
  dim from M3 may **not** automatically carry over to a new Activity's window. If so, re-apply the
  current Stealth state (read from engine state) in this view's `onCreate`/`onResume`, the same way
  the main Activity does. This is an easy gap to miss between milestones — check it explicitly.

**Verify:** switch phosphor hue and toggle Stealth from the Vitality panel, then open the assistant —
both should already reflect the current state, not reset to default.

## Step 8 — Session close
Update `BUILD_LOG.md`: mark items done/not-done, explicitly note the crisis-resource string is
empty/fallback pending Director input, note any Stealth-carryover fix made in Step 7, and write the
"resume here" line pointing at **M6 — splash, sound, and polish**.

---

## Hard stops — do not do these in M5
- **No real LLM brain** — this milestone is scripted-only; Chat 04 is later and separate.
- **No invented dialogue** — every line comes from the locked library; gaps get flagged, not improvised.
- **No shortcuts on the crisis-tier requirement** — it is non-optional and ships from first boot, even in its fallback form.
- **Never pair Distress/crisis or Hard-time with Warn, shake, or sound** — Idle only, exactly as documented.
- **Do not touch M6 (splash/sound/polish) or M7 (signing/shipping) scope.**

## What "M5 done" looks like to the Director
Tapping the floating trigger opens the real assistant — not the old placeholder. QUARK visibly
reacts (Scan on open, Happy/Warn/Idle as appropriate). All six rail actions work, with the four
reused ones behaving exactly as they did in M3. Typed messages get sensible in-character replies,
with a graceful fallback for anything unmatched. The conversation log scrolls with real exchanges.
Phosphor and Stealth both carry over correctly from wherever they were left. And — checked once,
deliberately — the crisis-tier safety behavior is present: it stays calm (no Warn, no sound) and
shows a real resource line beneath QUARK's words, even before a specific one has been configured.

---
*End of M5 Task Brief v1.0. Report back to Clara per the Step 0/1/8 checkpoints — particularly the
crisis-tier verification and the Stealth-carryover check — before the Bible is bumped to mark M5 closed.*
