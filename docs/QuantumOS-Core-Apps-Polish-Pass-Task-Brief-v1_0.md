# QuantumOS — Core Apps Polish Pass Task Brief v1.0
*For: Claude Code, on the ROG Strix build machine. Scoped from three items flagged-but-deferred across decisions 60, 88, and the SIGNAL+CONFIG brief. File to `docs/` once accepted.*
*Mission: close three long-standing, well-defined gaps across the now-complete eight-instrument set — no new design decisions required, everything here was already decided, just not yet executed.*

## 0. Read this first — why these three, together

All eight core instruments are docked and confirmed. Each item below was explicitly logged as *deferred, not missed* at the point it was flagged — the direction was already locked, execution was postponed to avoid drawing things twice or coupling unfinished threads. That's no longer true for any of the three:

1. **Icon direction was locked at decision 60** ("original SVG line-icons in the house stroke language ship with each app") — just never executed, stock Material icons shipped instead as the accepted interim fallback.
2. **Phosphor-hue sync** is a real bug-shaped gap, not a design question — the launcher's live hue selection and the docked modules' rendering have simply never been wired together.
3. **SIGNAL's decoder → QUARK brain** was explicitly flagged as a fast-follow the moment `AiAssistBridge` reached production (decision 91) — that's now true.

None of the three touches app identity, layout, or interaction design. This is a polish pass, not a redesign — **COMMS' identity drift is deliberately excluded**, it needs its own interactive design lab first, not execution work.

Zero idle redraw remains locked throughout.

## 1. Scope at a glance

| # | Deliverable | Acceptance |
|---|---|---|
| 1 | House line-icon set, drawn once, used everywhere | Every icon slot below replaced; zero stock Material icons remain in shipped screens |
| 2 | Phosphor-hue live sync | Changing hue anywhere updates every docked module + QUARK + launcher chrome live, no restart |
| 3 | SIGNAL decoder wired to `AiAssistBridge` | Decode action calls the real production brain instead of the offline stub |

## 2. Item 1 — House line-icon library

**Direction (locked, decision 60):** original SVG line-icons, consistent stroke weight, themeable with the `Phosphor` token so they recolor with hue switches like everything else in the shared shell — no platform emoji, no stock Material glyphs.

**Inventory — every icon slot currently running a stock fallback:**
- **Shared shell chrome:** back arrow, the four channel-selector icons (HOME/APPS/STATUS/LOG).
- **Vitality panel quick actions:** Stealth, Phosphor cycle, Beacon, Lock.
- **QUARK Assistant View command rail:** Status report, Engage stealth, Cycle phosphor, Light beacon, Say something, Trigger warn.
- **COMMS:** channel icon, transmit/live-pulse indicator.
- **FILES:** the four category icons (FIELD-LOGS / CAPTURES / COMMS-CACHE / MAPS).
- **AUDIO:** record, play, pause, stop.
- **CAM:** shutter, PHOTO/VIDEO mode toggle.
- **MAPS:** waypoint pin, you-marker.
- **RADIO:** band selector (FM/AM/WX), preset slots.
- **SIGNAL:** the four link gauges (cellular/wifi/GPS/Bluetooth), RUN SCAN.
- **CONFIG:** one icon per settings row (Phosphor, Boot Pace, Deployment Region).

**Build once, ship everywhere:** icons live in the shared `app-shell`/`core` module (or a dedicated `core-icons` if that's cleaner), same pattern as `Phosphor`/`Fonts` — not redrawn per app.

**Explicitly out of scope:** the pixel-level icon *masters* and the Atom-Lockup app-badge treatment (decision 60's other deferred half) — that's launcher-icon/app-store-badge work, a separate later identity pass, not this brief.

## 3. Item 2 — Phosphor-hue live sync

- Establish a single source of truth for the active hue (CONFIG's Phosphor setting, per the new SIGNAL+CONFIG brief, should already be that source — confirm, don't duplicate).
- All eight docked modules, the shared shell chrome, the Vitality panel, and the QUARK presence must observe that source and recolor live on change, no app restart or re-entry required.
- Verify the Vitality panel's own Phosphor quick action and CONFIG's Phosphor setting write to the same state rather than drifting into two independent toggles — this exact ambiguity was flagged as a risk when CONFIG was scoped.

## 4. Item 3 — SIGNAL decoder → production QUARK brain

- Replace SIGNAL's offline decode stub with a real call through `AiAssistBridge` (now production per decision 91).
- Keep the interaction shape SIGNAL already ships (RUN SCAN-style trigger, Scan reactive state, result surfaced in SIGNAL's own UI) — this is a backend swap, not a UI change.
- Respect the same first-run/offline-fallback behavior QUARK's brain already has elsewhere: if the brain is unavailable, SIGNAL should degrade honestly (e.g. "link unavailable" style messaging) rather than fail silently or crash.
- No change to SIGNAL's no-network-dependency posture beyond this — the call goes to the on-device brain, not out to any external service.

## 5. Explicitly not in this brief

- COMMS redesign (needs a design lab, not execution — separate thread).
- Monofonto typography swap (deliberately parked, cross-cutting, its own future thread).
- Icon masters / Atom-Lockup app badges (later identity/polish stage).
- Any Kiosk/device-owner work (unrelated critical-path item, still open per the Bible's own recommended next step).

## 6. Acceptance criteria (must all hold on the Fold 6, production build)

1. Zero stock Material icons remain in any of the slots listed in §2's inventory.
2. Icons recolor correctly across all three phosphor hues (green/amber/cyan) and read clearly at their smallest deployed size (command rail, gauges).
3. Changing phosphor hue from CONFIG updates every docked module + QUARK + shell chrome live, with no restart.
4. The Vitality panel's Phosphor quick action and CONFIG's Phosphor setting are confirmed to share one state, not two.
5. SIGNAL's decode action returns a real response from `AiAssistBridge` in a production (non-debug) build.
6. SIGNAL's offline-fallback messaging is honest and in-character, not a silent failure.
7. Zero new idle-redraw cost introduced by any of the three items — profile-confirmed.
8. CI green.

## 7. Process note

Standard handoff to `docs/`. Report back per-criterion in §6, plus a flag if the icon inventory in §2 missed any slot currently running a stock icon that Claude Code encounters during the pass.
