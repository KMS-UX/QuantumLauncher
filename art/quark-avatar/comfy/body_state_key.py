"""
Fetch the generated BODY state plates and key all four on a shared crop box -- Phase 18.

`flux2_state_plates.py` leaves its outputs in ComfyUI's own output folder and only records the
filenames; this pulls them down, keys them, and installs them as the app's drawables.

**The shared crop box is the whole point.** Keyed independently each plate gets its own tight bbox,
and the avatar then jumps a few pixels every time the state changes -- the defect Phase 12 caught on
the bust set. All four are cropped to the UNION of their four boxes instead, so they register
exactly.

Two things differ from the bust run:

* **`drop_base=False`.** The bust art includes the projection housing's concentric floor rings and
  `chroma_key.py` crops them at 0.915 of the height. The body plate has no rings -- it is cut at
  mid-thigh over flat magenta -- so that crop would simply amputate her legs.
* **IDLE is resized, not regenerated.** The generator runs at ~1 MP, so the three edited plates come
  back smaller than the 1254x1254 reference. IDLE has to be resampled to exactly their size before
  keying or a shared box means nothing.

Usage:
    python body_state_key.py
"""
import os
import urllib.parse
import urllib.request

import numpy as np
from PIL import Image

from comfy_client import COMFY
from chroma_key import key_magenta

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.normpath(os.path.join(HERE, ".."))
ROOT = os.path.normpath(os.path.join(ART, "..", ".."))
OUT = os.path.join(ART, "export", "body_state")
DRAWABLE = os.path.join(ROOT, "quark-avatar", "src", "main", "res", "drawable-nodpi")

IDLE_SOURCE = os.path.join(ART, "reference", "QUARK_HOLOGRAM_FRONT.png")
GENERATED = {
    "scan": "quark_state_scan_00002_.png",
    "happy": "quark_state_happy_00002_.png",
    "warn": "quark_state_warn_00003_.png",
}
STATES = ["idle", "scan", "happy", "warn"]


def fetch(filename, dst):
    url = f"{COMFY}/view?" + urllib.parse.urlencode(
        {"filename": filename, "subfolder": "", "type": "output"})
    data = urllib.request.urlopen(url, timeout=120).read()
    with open(dst, "wb") as f:
        f.write(data)
    return dst


def main():
    os.makedirs(OUT, exist_ok=True)
    raw = {}
    for state, name in GENERATED.items():
        dst = os.path.join(OUT, f"{state}_raw.png")
        fetch(name, dst)
        raw[state] = Image.open(dst).convert("RGB")
        print(f"[body] fetched {name} -> {raw[state].size}")

    size = raw["scan"].size
    for state, im in raw.items():
        assert im.size == size, f"{state} is {im.size}, expected {size}"

    idle = Image.open(IDLE_SOURCE).convert("RGB")
    if idle.size != size:
        print(f"[body] idle {idle.size} -> {size} to match the generated set")
        idle = idle.resize(size, Image.LANCZOS)
    raw["idle"] = idle
    idle.save(os.path.join(OUT, "idle_raw.png"))

    # Pass 1: key each independently just to learn its bbox.
    boxes = []
    for state in STATES:
        a = np.asarray(raw[state])
        alpha = np.clip((120 - (a[:, :, 0].astype(np.float32) - a[:, :, 1])) / 80.0, 0.0, 1.0)
        ys, xs = np.where(alpha > 0.5)
        box = (int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1))
        boxes.append(box)
        print(f"[body] {state:5s} own bbox {box}")

    shared = (min(b[0] for b in boxes), min(b[1] for b in boxes),
              max(b[2] for b in boxes), max(b[3] for b in boxes))
    print(f"[body] SHARED box {shared}  ({shared[2]-shared[0]}x{shared[3]-shared[1]})")

    # Pass 2: key everything to the shared box and install.
    os.makedirs(DRAWABLE, exist_ok=True)
    for state in STATES:
        keyed = key_magenta(raw[state], drop_base=False, box=shared)
        keyed.save(os.path.join(OUT, f"{state}_keyed.png"))
        target = os.path.join(DRAWABLE, f"quark_body_{state}.png")
        keyed.save(target)
        a = np.asarray(keyed)
        spill = float(((a[:, :, 0].astype(np.int16) - a[:, :, 1]) > 20).mean() * 100)
        print(f"[body] {state:5s} {keyed.size}  visible {100 * (a[:,:,3] > 127).mean():.1f}%  "
              f"residual spill {spill:.3f}%  -> {os.path.basename(target)} "
              f"({os.path.getsize(target) / 1048576:.2f} MB)")


if __name__ == "__main__":
    main()
