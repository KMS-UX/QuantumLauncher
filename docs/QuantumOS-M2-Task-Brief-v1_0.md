# QuantumOS — M2 Task Brief: Status + Log Channels
*For Claude Code. Companion to the Launcher Build Spec (v1.1), `CLAUDE.md`, and the M1 Task Brief. Version 1.0.*

> Read `CLAUDE.md` first if this is a new session. M1 is closed and confirmed on hardware. This
> brief has two parts: **(0) resolve the carried-over layout decision** before building on top of
> it, then **(1)–(3) build the STATUS and LOG channels.** Work in order; verify each step.

## Step 0 — Resolve the container + grid decision (carried over from M1 review)
The current code uses a fixed 9:19.5 letterboxed container. Change this:

- **Container:** switch from the fixed-aspect-ratio letterbox to **fill-and-adapt** — the surface
  fills the real available screen size; do not force a phone aspect ratio. Framing comes from the
  existing CRT vignette/falloff treatment, not from black bars.
- **APPS grid columns:** switch from any fixed column count to **adaptive** — size columns from a
  target cell width (roughly 88–96dp) so the column count is computed from available width rather
  than hardcoded. (In Compose, this is the `GridCells.Adaptive(minSize = ...)` pattern.)

**Verify:** build, install, and check on the Fold 6 unfolded — the surface should use the real
screen with no large dead black margins, and the APPS grid should show more columns on the wider
unfolded screen than it would on a narrow one. If you can also check it folded (cover screen), note
what you see in `BUILD_LOG.md` — don't block on it.

## Step 1 — STATUS channel: real vitals
Wire the existing `NavigationChannel.STATUS` into a real screen, feeding the existing
`engine.incomingTelemetryUpdate(...)` call (already in `QuantumState.kt` — use it, don't duplicate it):

- **Battery % and charging state** — read from `BatteryManager` (or the `ACTION_BATTERY_CHANGED`
  sticky broadcast). No special permission needed.
- **Uptime** — `SystemClock.elapsedRealtime()`, formatted as `HH:MM:SS`.
- **Connectivity** — keep this simple for M2: a basic connected/not-connected + transport type
  (Wi-Fi / cellular) via `ConnectivityManager`. **Do not** request `READ_PHONE_STATE` or other
  sensitive permissions to get precise signal-strength bars — that's a later refinement, not needed now.
- Poll on a sane interval (e.g. every few seconds) from the ViewModel's coroutine scope — not on every recomposition.
- Render using the existing terminal-readout style (monospace, phosphor color, the strip pattern
  already used in the M0 surface) — not a default Material list or card.

**Verify:** open STATUS, see real battery % change as expected (e.g. plug in / unplug and watch it
update), uptime counting up, and a connectivity line that reflects actual Wi-Fi/cellular state.

## Step 2 — LOG channel: the console reel
Wire `NavigationChannel.LOG` to render `engine.systemLogs` (already populated by the engine) as a
scrollable list:

- `LazyColumn`, monospace, phosphor color — match the console-reel look already used in the M0 readout.
- Plain rows, no Material dividers/cards.
- Cap what's rendered to a reasonable recent window (e.g. last 100 entries — the engine already caps storage at 150).

**Verify:** open LOG, see real recent events (boot steps, nav changes, etc.) scrolling, most-recent visible.

## Step 3 — Navigation check
Confirm all four channels (HOME / APPS / STATUS / LOG) are reachable from the existing channel
selector via the existing `transitionNavigation(...)` call — don't add a second navigation mechanism.

**Verify:** can reach and return from every channel without dead ends.

## Step 4 — Session close
Update `BUILD_LOG.md`: mark this milestone's items done/not-done, note anything deferred (precise
signal bars, storage breakdown if skipped) as a known issue rather than silently dropping it, and
write the exact "resume here" line pointing at **M3 — Vitality panel** for next session.

---

## Hard stops — do not do these in M2
- **No new permission prompts beyond basic connectivity checks** — signal-strength precision waits.
- **Do not build the Vitality roll-down panel** — that's M3.
- **Do not touch the floating QUARK trigger/overlay** — that's M4.
- **Do not invent new QUARK dialogue** — M2 has no QUARK-facing surfaces; if any text needs her voice, it isn't this milestone.

## What "M2 done" looks like to the Director
STATUS shows real, live battery/uptime/connectivity. LOG shows a real, scrolling event console. The
screen now properly fills the Fold 6's display instead of sitting in a letterboxed strip, and the
APPS grid adapts its column count to the available width.

---
*End of M2 Task Brief v1.0. Report back to Clara per the Step 0/4 checkpoints — particularly the on-device container check — before the Bible is bumped to mark M2 closed.*
