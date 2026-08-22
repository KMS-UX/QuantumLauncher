"""
Bring the Director's app-icon reference onto the QuantumOS phosphor palette -- decision 60's badge.

`reference/QuantumLauncherIcon_reference.png` is the Director's composition and it is kept: the
Q-ring lockup, the hexagon-textured ground, the bracket ticks, the circuitry inside the ring. What
cannot ship as delivered is the PALETTE and the REGISTER. Measured on the file: mean saturation
0.603, running cyan -> magenta -> purple with chrome/silver metal and an Earth-blue horizon. The
house style is phosphor-only -- GREEN/AMBER/CYAN, one hue at a time, on CRT ground #020402, with red
reserved for --warn -- and the aesthetic is worn "used-future", not app-store gloss. So this is an
EDIT of the Director's art, not a replacement of it.

Three things this fixes beyond colour:

* **The strapline goes.** "QUANTUMOIS PROJECT / BY QUANTUM LAB" is illegible at any real launcher
  size and adaptive-icon masking clips the rounded-square corners it sits near. (It also reads
  QUANTUMOIS, not QUANTUMOS -- flagged to the Director rather than silently corrected in art.)
* **Flat, not glossy.** Specular chrome highlights read as plastic; a phosphor mark is emissive.
* **Square, full-bleed.** The adaptive icon supplies its own mask, so the source must NOT carry its
  own rounded-square badge shape or it gets double-masked.

Same Flux 2 Klein edit graph as the state plates -- imported, not copied, so there is one graph in
this pipeline. Denoise runs higher than the plates use: a full re-palette is a much larger ask than
a mouth adjustment, and there is no facial identity to protect here, only a composition.

Usage:
    python app_icon_repalette.py [--denoise=0.62]
"""
import json
import os
import sys
import time
import urllib.request
import uuid

from comfy_client import COMFY, post, upload
from flux2_state_plates import graph

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.normpath(os.path.join(HERE, ".."))
SOURCE = os.path.join(ART, "reference", "QuantumLauncherIcon_reference.png")
OUTDIR = os.path.join(ART, "renders", "icon")

# One prompt per hue. GREEN is the shipped default (CLAUDE.md) and so the one that becomes the
# badge; the other two exist because a future cosmetic/theme pack will want them, and generating
# them now costs one minute each while the composition is fresh.
HUES = {
    "green": ("phosphor green", "#00FF00"),
    "amber": ("phosphor amber", "#FFB000"),
    "cyan": ("phosphor cyan", "#00E5FF"),
}

PROMPT = (
    "Recolour this icon completely into a single-colour {label} CRT phosphor display. Every metallic, "
    "chrome, silver, white, cyan, blue, magenta and purple element becomes {label} ({hexv}) glowing "
    "line-work on a near-black background. Keep the composition exactly: the large ring-and-tail Q "
    "monogram, the segmented arcs and bracket ticks around it, the hexagonal honeycomb texture in the "
    "background, and the fine circuit traces inside the ring. Render it as a flat, emissive, "
    "monochrome phosphor screen with visible faint scanlines -- matte, not glossy: remove all "
    "specular highlights, metal shading, reflections and gradients so nothing looks like polished "
    "chrome or plastic. Remove the planet and its blue horizon glow entirely. Remove ALL small text "
    "and lettering. Fill the whole square frame edge to edge with the near-black background and do "
    "NOT draw a rounded-square badge, border, plaque or frame around the artwork. Use only shades of "
    "{label} and black -- absolutely no other colour anywhere."
)

SEEDS = {"green": 20_001, "amber": 20_002, "cyan": 20_003}


def run(states, denoise):
    os.makedirs(OUTDIR, exist_ok=True)
    name = upload(SOURCE)
    print(f"[icon] uploaded {name}")
    results = {}
    for hue in states:
        label, hexv = HUES[hue]
        prefix = f"quantum_icon_{hue}_d{int(denoise * 100)}"
        g = graph(name, PROMPT.format(label=label, hexv=hexv), SEEDS[hue], prefix, denoise)
        pid = post("/prompt", {"prompt": g, "client_id": str(uuid.uuid4())})["prompt_id"]
        print(f"[icon] {hue}: queued {pid}")
        t0 = time.time()
        while time.time() - t0 < 900:
            h = json.loads(urllib.request.urlopen(f"{COMFY}/history/{pid}", timeout=30).read())
            if pid in h:
                files = [im["filename"]
                         for out in h[pid].get("outputs", {}).values()
                         for im in out.get("images", [])]
                print(f"[icon] {hue}: {h[pid]['status'].get('status_str')} "
                      f"in {int(time.time() - t0)}s -> {files}")
                for f in files:
                    url = f"{COMFY}/view?filename={f}&type=output"
                    dst = os.path.join(OUTDIR, f)
                    with open(dst, "wb") as fh:
                        fh.write(urllib.request.urlopen(url, timeout=120).read())
                    print(f"[icon] saved {dst}")
                results[hue] = files
                break
            time.sleep(5)
        else:
            raise TimeoutError(hue)
    return results


if __name__ == "__main__":
    den = next((float(a.split("=", 1)[1]) for a in sys.argv if a.startswith("--denoise=")), 0.62)
    wanted = [a.split("=", 1)[1] for a in sys.argv if a.startswith("--hue=")] or list(HUES)
    run(wanted, den)
