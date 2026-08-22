"""
C1 -- the field-measurement harness for QUARK's presentation.

**Why this exists.** Every performance number on the QUARK avatar track, Phases 15 through 22, came
from a desktop x86 emulator. C1 is the item that closes that, and it has stayed open because the
Fold 6 has never been connected. This script reduces the whole C1 pass to one command, so that when
the device IS plugged in the measurement is a single run rather than an afternoon of remembering how
it was done last time.

It measures the two things the house style actually put at risk:

  1. **The AMBIENT loop's battery cost.** QUARK is specified as "static at rest (zero idle redraw)"
     precisely because she sits on an always-visible surface on a battery-as-vitality field tool. The
     AMBIENT carrier is a deliberate, flagged departure from that rule. Its cost is the number that
     decides whether it ships on by default, and it can only be measured on real hardware.
  2. **Frame cost under the reactive effects.** The materialise (B1) and the speaking emitter cadence
     (B3) are the only things that animate. If either misses frames on the real panel it reads as the
     unit struggling, which is the opposite of what QUARK is for.

**It refuses to fabricate.** On an emulator the battery section produces NO numbers at all and says
why. That is not caution for its own sake: this track has already had to correct claims that turned
out to be measurement artefacts, and a battery figure from a desktop VM would be worse than useless
because it would look like an answer.

Usage:
    python c1_field_measure.py                   # full pass, 300 s battery windows
    python c1_field_measure.py --window-s 120    # shorter windows
    python c1_field_measure.py --frames-only     # skip battery entirely
    python c1_field_measure.py --serial R5CT...  # choose a device
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import time
from datetime import datetime

PKG = "com.quantumos.shell"
HERE = os.path.dirname(os.path.abspath(__file__))
REPORT_DIR = os.path.normpath(os.path.join(HERE, "..", "renders", "c1"))

# The kernel's coulomb counter, in microamp-hours. This is the honest source: `dumpsys battery`
# reports whole percent, which over a five-minute window is 0 or 1 and tells you nothing.
CHARGE_COUNTER = "/sys/class/power_supply/battery/charge_counter"


# ==============================================================================================
# device
# ==============================================================================================
class Device:
    def __init__(self, serial=None):
        sdk = os.environ.get("ANDROID_SDK_ROOT") or os.path.expanduser("~/AppData/Local/Android/Sdk")
        candidate = os.path.join(sdk, "platform-tools", "adb.exe")
        self.adb = os.environ.get("ADB") or (candidate if os.path.exists(candidate) else "adb")
        self.serial = serial
        self._probe()

    def run(self, *args, timeout=180):
        cmd = [self.adb] + (["-s", self.serial] if self.serial else []) + list(args)
        # subprocess passes argv straight through, so device paths like /sdcard are NOT mangled the
        # way a shell on Windows mangles them.
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout, errors="replace")
        return r.stdout

    def shell(self, cmd, timeout=180):
        return self.run("shell", cmd, timeout=timeout)

    def prop(self, name):
        return self.shell("getprop " + name).strip()

    def _probe(self):
        attached = [l.split("\t")[0] for l in self.run("devices").splitlines()[1:]
                    if l.strip().endswith("device")]
        if not attached:
            raise SystemExit(
                "C1: no device attached. Connect the Fold 6 with USB debugging on -- or start an "
                "emulator if you only want to smoke-test that this harness still runs.")
        if self.serial is None:
            if len(attached) > 1:
                raise SystemExit("C1: several devices attached %s; pass --serial." % attached)
            self.serial = attached[0]

        self.model = self.prop("ro.product.model")
        self.android = self.prop("ro.build.version.release")
        self.hardware = self.prop("ro.hardware")
        self.characteristics = self.prop("ro.build.characteristics")
        # ranchu/goldfish are the Android emulator's virtual boards and `characteristics` carries
        # "emulator" too. Checking all three survives any one of them changing.
        self.is_emulator = (
            self.hardware in ("ranchu", "goldfish")
            or "emulator" in self.characteristics
            or self.model.startswith("sdk_")
        )
        m = re.search(r"(\d+)x(\d+)", self.shell("wm size"))
        self.size = "%sx%s" % (m.group(1), m.group(2)) if m else "unknown"
        d = re.search(r"(\d+)", self.shell("wm density"))
        self.density = d.group(1) if d else "?"


# ==============================================================================================
# driving the UI
# ==============================================================================================
class Ui:
    """Finds controls by their LABEL, never by coordinates.

    The avatar surface was tuned on one 1080x2424 phone and C1's entire purpose is to run somewhere
    else -- a Fold 6, folded and unfolded, at a different size and density. Hard-coded taps would
    silently hit the wrong row there, and the run would confidently report numbers for the wrong
    thing.
    """

    def __init__(self, dev):
        self.dev = dev

    def dump(self):
        self.dev.shell("uiautomator dump /sdcard/c1_ui.xml")
        return self.dev.shell("cat /sdcard/c1_ui.xml")

    def find(self, needle, xml=None):
        xml = self.dump() if xml is None else xml
        for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
            if needle.lower() in m.group(1).lower():
                return ((int(m.group(2)) + int(m.group(4))) // 2,
                        (int(m.group(3)) + int(m.group(5))) // 2,
                        m.group(1))
        return None

    def tap(self, x, y, settle=1.5):
        self.dev.shell("input tap %d %d" % (x, y))
        time.sleep(settle)

    def tap_label(self, needle, settle=1.5):
        hit = self.find(needle)
        if not hit:
            return False
        self.tap(hit[0], hit[1], settle)
        return True

    def value_of(self, label):
        """Read a control row's value -- the `: SOMETHING` text on the same line as its label."""
        xml = self.dump()
        row = self.find(label, xml)
        if not row:
            return None
        y = row[1]
        found = None
        for m in re.finditer(r'text="(:[^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
            my = (int(m.group(3)) + int(m.group(5))) // 2
            if abs(my - y) <= 14:
                found = m.group(1).lstrip(": ").strip()
        return found

    def set_control(self, label, wanted, tries=6):
        """Tap a cycling control until its value contains `wanted`. Returns False if it never does."""
        for _ in range(tries):
            current = self.value_of(label)
            if current is None:
                return False
            if wanted.lower() in current.lower():
                return True
            if not self.tap_label(label):
                return False
        return False


def open_avatar_surface(ui):
    """Launch the launcher and navigate to the avatar dev screen.

    The dev screen rather than the Assistant View, deliberately: since Phase 22 both draw the SAME
    composable (`QuarkProjection`), but only the dev screen exposes AMBIENT -- so only it can do the
    A/B this measurement is built around. The Assistant View is what ships; the dev screen is the
    instrumented copy of it.
    """
    dev = ui.dev
    dev.shell("am force-stop " + PKG)
    time.sleep(1)
    dev.shell("input keyevent KEYCODE_WAKEUP")
    dev.shell("input swipe 500 1600 500 600 200")     # clear a lock screen if one is up
    time.sleep(1)
    dev.shell("monkey -p %s -c android.intent.category.LAUNCHER 1" % PKG)
    time.sleep(9)
    if not ui.tap_label("CONFIG", settle=4):
        raise SystemExit("C1: could not find the CONFIG tile on HOME.")
    if not ui.tap_label("DEV: QUARK AVATAR", settle=8):
        raise SystemExit(
            "C1: could not find CONFIG's QUARK avatar row. If that row has been retired, this "
            "harness needs another way onto an instrumented surface -- the Assistant View is "
            "exported=false and has no AMBIENT control.")
    if ui.find("RENDER") is None:
        raise SystemExit("C1: navigated somewhere, but it is not the avatar screen.")


# ==============================================================================================
# frame timing
# ==============================================================================================
def gfx_reset(dev):
    dev.shell("dumpsys gfxinfo %s reset" % PKG)


def gfx_read(dev):
    raw = dev.shell("dumpsys gfxinfo %s" % PKG)
    out = {}

    def grab(pattern, key, cast=int):
        m = re.search(pattern, raw)
        if m:
            out[key] = cast(m.group(1))

    grab(r"Total frames rendered:\s*(\d+)", "frames")
    grab(r"Janky frames:\s*(\d+)", "janky")
    grab(r"Janky frames:\s*\d+\s*\(([\d.]+)%\)", "janky_pct", float)
    grab(r"50th percentile:\s*(\d+)ms", "p50_ms")
    grab(r"90th percentile:\s*(\d+)ms", "p90_ms")
    grab(r"95th percentile:\s*(\d+)ms", "p95_ms")
    grab(r"99th percentile:\s*(\d+)ms", "p99_ms")
    grab(r"Number Missed Vsync:\s*(\d+)", "missed_vsync")
    grab(r"Number Slow UI thread:\s*(\d+)", "slow_ui_thread")
    grab(r"Number Slow bitmap uploads:\s*(\d+)", "slow_bitmap_uploads")
    grab(r"Number Slow issue draw commands:\s*(\d+)", "slow_draw_commands")

    # A surface that drew NOTHING still reports percentiles, and they are garbage -- the emulator
    # smoke-test came back with "p50 4950ms" for a scenario in which zero frames were rendered,
    # which reads like a five-second frame instead of like the zero-idle-redraw guarantee holding.
    # Blank them rather than print them: this harness exists to stop exactly that kind of number
    # ending up in the log looking like a result.
    if out.get("frames", 0) == 0:
        for k in ("p50_ms", "p90_ms", "p95_ms", "p99_ms"):
            out.pop(k, None)
        out["note"] = "no frames drawn"
    return out


def measure_frames(ui, name, action, hold_s):
    """Reset the frame counters, perform `action`, let it run for `hold_s`, then read."""
    gfx_reset(ui.dev)
    if action:
        action()
    time.sleep(hold_s)
    stats = gfx_read(ui.dev)
    stats["scenario"] = name
    stats["hold_s"] = hold_s
    return stats


# ==============================================================================================
# battery
# ==============================================================================================
def charge_uah(dev):
    raw = dev.shell("cat %s" % CHARGE_COUNTER).strip()
    return int(raw) if re.fullmatch(r"-?\d+", raw) else None


def battery_window(dev, name, window_s):
    """Charge drawn over a fixed window with the device held virtually unplugged.

    Unplugging matters more than anything else here. The device is on USB because adb is on USB, so
    without `dumpsys battery unplug` it is CHARGING throughout and the run measures the charger
    rather than QUARK. `reset` is always restored in the caller's finally block.
    """
    dev.shell("dumpsys battery unplug")
    time.sleep(3)

    start = charge_uah(dev)
    if start is None:
        return {"scenario": name,
                "error": "no charge_counter at %s on this device" % CHARGE_COUNTER}

    t0 = time.time()
    print("    [%s] holding %ds ..." % (name, window_s), flush=True)
    # Nothing is touched during the window on purpose: an input event would raise a state change and
    # what is being measured is precisely what the surface costs when NOTHING happens.
    time.sleep(window_s)
    elapsed = time.time() - t0

    end = charge_uah(dev)
    drawn = start - end
    return {
        "scenario": name,
        "uah_drawn": drawn,
        "elapsed_s": round(elapsed, 1),
        "ma_avg": round(drawn / (elapsed / 3600.0) / 1000.0, 2) if elapsed > 0 else None,
    }


# ==============================================================================================
# report
# ==============================================================================================
def write_report(dev, frames, battery, battery_note, window_s):
    os.makedirs(REPORT_DIR, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    path = os.path.join(REPORT_DIR, "c1-%s-%s.md" % (re.sub(r"\W+", "_", dev.model), stamp))

    lines = []
    lines.append("# C1 field measurement")
    lines.append("")
    lines.append("| | |")
    lines.append("|---|---|")
    lines.append("| device | `%s` |" % dev.model)
    lines.append("| android | %s |" % dev.android)
    lines.append("| hardware | `%s` |" % dev.hardware)
    lines.append("| screen | %s @ %s dpi |" % (dev.size, dev.density))
    lines.append("| real hardware | **%s** |" % ("NO -- EMULATOR" if dev.is_emulator else "yes"))
    lines.append("| run at | %s |" % datetime.now().isoformat(timespec="seconds"))
    lines.append("")

    if dev.is_emulator:
        lines.append("> **These are not C1 results.** This run was made on an emulator, so it "
                     "proves only that the harness executes. Frame figures from a desktop GPU do "
                     "not predict a phone panel, and the battery section was not run at all.")
        lines.append("")

    lines.append("## Frame cost")
    lines.append("")
    if frames:
        keys = ["scenario", "frames", "janky", "janky_pct", "p50_ms", "p90_ms", "p95_ms", "p99_ms",
                "missed_vsync", "slow_ui_thread", "slow_bitmap_uploads", "slow_draw_commands",
                "note"]
        present = [k for k in keys if any(k in f for f in frames)]
        lines.append("| " + " | ".join(present) + " |")
        lines.append("|" + "---|" * len(present))
        for f in frames:
            lines.append("| " + " | ".join(str(f.get(k, "")) for k in present) + " |")
    else:
        lines.append("_not collected_")
    lines.append("")

    lines.append("## AMBIENT battery cost")
    lines.append("")
    if battery_note:
        lines.append("**Not measured.** %s" % battery_note)
    else:
        lines.append("Windows of %d s each, screen on, device held unplugged, nothing touched." % window_s)
        lines.append("")
        lines.append("| scenario | uAh drawn | elapsed s | avg mA |")
        lines.append("|---|---|---|---|")
        for b in battery:
            if "error" in b:
                lines.append("| %s | _%s_ | | |" % (b["scenario"], b["error"]))
            else:
                lines.append("| %s | %s | %s | %s |" %
                             (b["scenario"], b["uah_drawn"], b["elapsed_s"], b["ma_avg"]))
        on = next((b for b in battery if b["scenario"] == "AMBIENT ON" and "uah_drawn" in b), None)
        off = next((b for b in battery if b["scenario"] == "AMBIENT OFF" and "uah_drawn" in b), None)
        if on and off and off["uah_drawn"]:
            delta = on["uah_drawn"] - off["uah_drawn"]
            pct = 100.0 * delta / off["uah_drawn"]
            lines.append("")
            lines.append("**AMBIENT costs %+d uAh over %d s (%+.1f%%).**" % (delta, window_s, pct))
            lines.append("")
            lines.append("That is the number the house style's zero-idle-redraw rule is protecting, "
                         "and the one that decides whether AMBIENT ships on by default.")
    lines.append("")

    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    return path


# ==============================================================================================
# main
# ==============================================================================================
def main():
    ap = argparse.ArgumentParser(description="C1 field measurement for the QUARK presentation.")
    ap.add_argument("--serial")
    ap.add_argument("--window-s", type=int, default=300, help="battery window per scenario")
    ap.add_argument("--frames-only", action="store_true")
    args = ap.parse_args()

    dev = Device(args.serial)
    print("C1 harness")
    print("  device   %s (%s, android %s)" % (dev.model, dev.hardware, dev.android))
    print("  screen   %s @ %s dpi" % (dev.size, dev.density))
    if dev.is_emulator:
        print("")
        print("  *** EMULATOR DETECTED ***")
        print("  This is a harness smoke-test, NOT a C1 pass. Battery will not be measured at all;")
        print("  frame numbers come from a desktop GPU and do not predict the Fold 6's panel.")
        print("")

    ui = Ui(dev)
    print("  opening the avatar surface ...")
    open_avatar_surface(ui)

    # ---- frames ------------------------------------------------------------------------------
    frames = []

    print("  frames: idle, AMBIENT off (the zero-idle-redraw claim) ...")
    ui.set_control("AMBIENT", "OFF")
    frames.append(measure_frames(ui, "idle, AMBIENT off", None, 10))

    print("  frames: idle, AMBIENT on ...")
    ui.set_control("AMBIENT", "ON")
    frames.append(measure_frames(ui, "idle, AMBIENT on", None, 10))

    print("  frames: materialise ...")
    ui.set_control("AMBIENT", "OFF")
    frames.append(measure_frames(ui, "materialise", lambda: ui.tap_label("MATERIALISE", 0.1), 4))

    print("  frames: state change ...")
    frames.append(measure_frames(ui, "state change", lambda: ui.tap_label("STATE", 0.1), 4))

    # ---- battery -----------------------------------------------------------------------------
    battery = []
    note = None
    if args.frames_only:
        note = "Skipped: --frames-only."
    elif dev.is_emulator:
        note = ("Skipped: this is an emulator. A battery figure from a desktop VM would look like "
                "an answer and would not be one. Run this on the Fold 6.")
    else:
        try:
            for scenario, wanted in (("AMBIENT OFF", "OFF"), ("AMBIENT ON", "ON")):
                if not ui.set_control("AMBIENT", wanted):
                    battery.append({"scenario": scenario, "error": "could not set the AMBIENT control"})
                    continue
                battery.append(battery_window(dev, scenario, args.window_s))
        finally:
            # Always give the battery back to the real charger, whatever happened above. A device
            # left in `unplug` state reports as discharging until it is rebooted.
            dev.shell("dumpsys battery reset")

    path = write_report(dev, frames, battery, note, args.window_s)
    print("")
    print("  report -> %s" % path)
    if dev.is_emulator:
        print("  (harness smoke-test only -- no C1 numbers were produced)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
