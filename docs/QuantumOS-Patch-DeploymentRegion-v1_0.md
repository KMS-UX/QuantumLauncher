# QuantumOS — Patch: Deployment Region (Japan / Hong Kong)
*For Claude Code. Small, focused patch — not a numbered milestone. Refines the M5 crisis-tier
resource text. Version 1.0.*

> Read `CLAUDE.md` first if this is a new session. This replaces the generic-fallback Config-string
> plan from the M5 brief's Step 1 with two real, verified regional presets.

## Goal
A manual "Deployment Region" switch (Japan ⟷ Hong Kong, **default Japan**) that selects which real
crisis resource is shown beneath QUARK's line when the Distress/crisis intent fires (M5, Step 1),
surfaced via a small toggle on STATUS and a status line on HOME, with QUARK acknowledging the switch.

## The two presets (verified — use exactly as given, do not alter the numbers)

**Japan (default):**
- TELL Lifeline (English-language) — Toll-free 0800-300-8355, or 03-5774-0992.
- Yorisoi Hotline (multilingual, government-backed) — 0120-279-338, press 2 for non-Japanese speakers.
- Emergency: 110 for police, 119 for ambulance.

**Hong Kong:**
- The Samaritans (24-hour, multilingual) — 2896 0000.
- Suicide Prevention Services — 2382 0000.
- Emergency: 999.

Format each as 2–3 short plain-text lines beneath QUARK's crisis-tier line — not a wall of text.
Keep the **old generic fallback** ("contact local emergency services or someone you trust") as a
defensive default only, in case the lookup ever fails — it should not normally be seen now that
real presets exist.

## Step 1 — Data + default
- Add a `DeploymentRegion` enum: `JAPAN`, `HONG_KONG`. Default `JAPAN`.
- Store both resource blocks as constants (curated, not free-text Config — these are verified values, not arbitrary user input).
- Wire the M5 crisis-tier UI text to look up the active region's block instead of the empty/generic Config string.

**Verify:** with the default (Japan) active, trigger the Distress/crisis intent once and confirm the Japan block renders correctly beneath QUARK's line.

## Step 2 — The toggle (STATUS channel)
- Add a small row to the existing STATUS channel: `DEPLOYMENT REGION: JAPAN` (or `HONG KONG`), tap to cycle between the two — same interaction pattern as the existing Cycle-phosphor tap-to-cycle control. Don't add this to the Vitality panel's Zone 2 (locked at 4 actions) or the Assistant View's command rail (locked at 6 actions, decision 36) — STATUS is the correct, unlocked surface for this.

**Verify:** tapping cycles Japan → Hong Kong → Japan; the STATUS row reflects the current state correctly.

## Step 3 — HOME status line
- Add a small plain-UI status line to the Home channel reflecting the current region, e.g. `DEPLOYMENT: JAPAN`. Keep it terse and utilitarian (house voice rules) — this is status text, not QUARK speaking.

**Verify:** switching the toggle in STATUS updates the HOME line without needing a restart.

## Step 4 — QUARK's acknowledgement line (new content — use exactly as drafted, pending Director sign-off noted in `BUILD_LOG.md`)
A new intent, fired when the region switches. Posture **Happy**, reuse the existing phosphor-retune-sweep sound (no new sound asset needed). Rotate the variants per the established rule.

**Switching to Hong Kong:**
- "Deployment region set to Hong Kong."
- "Hong Kong, {operator}. Recalibrating the local watch."

**Switching to Japan:**
- "Deployment region set to Japan."
- "Japan, {operator}. Home ground — standing by."

Log the exchange into the M5 conversation log like any other QUARK reflex.

**Verify:** each toggle direction fires the correct line and logs correctly.

## Step 5 — Session close
Update `BUILD_LOG.md`: mark done, note that the acknowledgement lines in Step 4 are new content
pending the Director's final sign-off (not yet folded into the official Scripted-Line Library doc),
and confirm the resume line still points at **M6 — splash, sound, and polish**.

---

## Hard stops
- **Do not** add a third region or a free-text/custom region option — exactly two presets, manual switch only (no GPS/locale auto-detection — this is a deliberate Operator action, not passive detection).
- **Do not** place the toggle in the Vitality panel or the command rail — both are locked-count surfaces.
- **Do not** alter the verified phone numbers in any way.

## What "done" looks like to the Director
STATUS shows a tap-to-cycle Deployment Region row; HOME reflects the current region; switching
plays a short QUARK line and logs it; and triggering the crisis-tier intent shows the correct real
resource block for whichever region is currently active.

---
*End of Deployment Region Patch v1.0. Report back to Clara — particularly whether Step 4's lines
feel right — before they're folded into the Scripted-Line Library as canon.*
