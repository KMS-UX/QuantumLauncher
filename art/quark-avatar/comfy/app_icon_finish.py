"""
Turn the re-paletted icon render into a real Android adaptive icon -- decision 60's badge.

The generated art (see app_icon_repalette.py) is the right composition in the right colour family,
but it is a PICTURE OF AN ICON, not an icon. Three things have to be fixed deterministically rather
than by asking the model again, because they are exactness problems and diffusion is not exact:

* **It draws its own rounded-square badge.** Android's adaptive icon supplies the mask itself, so a
  source carrying its own badge shape gets double-masked -- a rounded square inside a rounded square,
  with the corners of the inner one clipped. The frame is cropped off here.
* **The colour is approximately phosphor.** Measured on the d88 render, the greens land around
  #3FDD4F, not the locked `#00FF00`. Every pixel is re-derived as luminance x the exact token, so the
  badge is on-palette by construction rather than by eye -- the same discipline the rest of the app
  follows by never hardcoding a hue.
* **The artwork is not in the safe zone.** Only the central ~66dp of the 108dp adaptive canvas is
  guaranteed visible; the render fills the frame and sits high in it. The lockup is re-cropped to its
  own ink bounds and rescaled into the safe zone.

Output is a foreground PNG on transparency plus the existing CRT-ground background, which is what
`mipmap-anydpi-v26/ic_launcher.xml` already composes.

Usage:
    python app_icon_finish.py [--hue=green] [--src=<render.png>]
"""
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.normpath(os.path.join(HERE, ".."))
ROOT = os.path.normpath(os.path.join(ART, "..", ".."))
RENDERS = os.path.join(ART, "renders", "icon")
EXPORT = os.path.join(ART, "export", "icon")
RES = os.path.join(ROOT, "app", "src", "main", "res")

# The locked tokens (CLAUDE.md). GREEN is the shipped default and the one that becomes the badge.
TOKENS = {"green": (0x00, 0xFF, 0x00), "amber": (0xFF, 0xB0, 0x00), "cyan": (0x00, 0xE5, 0xFF)}

CANVAS = 432          # 108dp at xxxhdpi
SAFE = 0.62           # fraction of the canvas the artwork may occupy (66/108)
LUMA = (0.299, 0.587, 0.114)
INK = 0.10            # any ink at all -- used for EXTENT, so nothing gets clipped
INK_BRIGHT = 0.60     # ink the eye actually reads -- used for CENTRING, see fit_safe_zone()


def crop_frame(im):
    """Drop the render's own rounded-square badge and keep what is inside it."""
    a = np.asarray(im.convert("RGB")).astype(np.float32)
    lum = a[:, :, 0] * LUMA[0] + a[:, :, 1] * LUMA[1] + a[:, :, 2] * LUMA[2]
    h, w = lum.shape
    # The frame is the outermost lit ring. Walk in from each edge until a row/col carries real ink,
    # then walk PAST that ring until the art proper starts -- measured, not a fixed inset.
    prof_r = lum.max(axis=1)
    prof_c = lum.max(axis=0)
    thr = lum.max() * 0.12
    top = int(np.argmax(prof_r > thr))
    bot = h - int(np.argmax(prof_r[::-1] > thr))
    left = int(np.argmax(prof_c > thr))
    right = w - int(np.argmax(prof_c[::-1] > thr))
    # Step inside the frame stroke itself by a small margin of the detected box.
    inset = int(0.045 * max(bot - top, right - left))
    return im.crop((left + inset, top + inset, right - inset, bot - inset))


def to_phosphor(im, token):
    """Luminance -> exact token colour, near-black -> transparent. On-palette by construction."""
    a = np.asarray(im.convert("RGB")).astype(np.float32)
    lum = (a[:, :, 0] * LUMA[0] + a[:, :, 1] * LUMA[1] + a[:, :, 2] * LUMA[2]) / 255.0
    # Gentle floor so the CRT ground does not survive as a grey haze, and a slight lift so the
    # faint hexagon texture is not lost entirely.
    alpha = np.clip((lum - 0.06) / 0.55, 0.0, 1.0) ** 0.85
    v = np.clip(lum / max(lum.max(), 1e-6), 0.0, 1.0) ** 0.75
    rgb = np.dstack([v * token[0], v * token[1], v * token[2]])
    return Image.fromarray(np.dstack([rgb, alpha * 255.0]).astype(np.uint8), "RGBA")


def fit_safe_zone(im):
    """
    Scale by the artwork's full extent, but CENTRE on the part of it the eye can actually see.

    Why the two thresholds. Centring on the ink bounding box alone is what the first badge did, and
    it looked wrong on device: the Director reported the Q sitting high with roughly 20% dead space
    beneath it. Measured on that file, the bounding box was centred *perfectly* -- rows 83..348 in a
    432 canvas, centre 216, exactly the canvas centre. What it did not account for is that the
    bottom third of that box is near-invisible: faint low-alpha residue in the region the wordmark
    and planet used to occupy, which the re-palette left behind as texture. The BRIGHT mark spans
    only rows 90..276, centre 183 -- 33px above where it looked centred by the numbers.

    So extent and position are measured separately: the full-ink box sets the SCALE (nothing gets
    clipped, faint arcs and hexagons included), and the bright-ink centroid sets the POSITION (the
    lockup lands where the eye expects it). The offset is clamped so the full artwork still fits the
    canvas, which matters because pushing the mark down also pushes the faint field down with it.
    """
    a = np.asarray(im)[:, :, 3].astype(np.float32) / 255.0
    ys, xs = np.where(a > INK)
    x0, y0, x1, y1 = int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1)

    art = im.crop((x0, y0, x1, y1))
    target = int(CANVAS * SAFE)
    scale = target / max(art.width, art.height)
    art = art.resize((max(1, int(art.width * scale)), max(1, int(art.height * scale))), Image.LANCZOS)

    # The bright set is measured AFTER the resize, on the pixels that actually ship. Measuring it on
    # the full-size render gives a different -- and wrong -- answer: downscaling by ~3x averages the
    # faint hexagon field toward black, so most of it drops below the threshold, while the ring
    # survives intact. Measured before the resize the "bright" region looked centred; measured on
    # what ships, it sits high, which is what the eye was reporting.
    sa = np.asarray(art)[:, :, 3].astype(np.float32) / 255.0
    bys, bxs = np.where(sa > INK_BRIGHT)
    if len(bys) == 0:                       # no bright mark at all -- fall back to the whole crop
        bys, bxs = np.where(sa > INK)
    bx = (bxs.min() + bxs.max()) / 2.0
    by = (bys.min() + bys.max()) / 2.0
    bcx, bcy = bx, by

    left = int(round(CANVAS / 2.0 - bx))
    top = int(round(CANVAS / 2.0 - by))
    # Keep the whole artwork on the canvas. When the art is smaller than the canvas the offset may
    # run from 0 to the slack; when it is larger the slack is negative and the range flips.
    def clamp(v, extent):
        lo, hi = (0, CANVAS - extent) if extent <= CANVAS else (CANVAS - extent, 0)
        return max(lo, min(v, hi))

    left = clamp(left, art.width)
    top = clamp(top, art.height)

    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    canvas.paste(art, (left, top))
    print(f"[icon] art {art.width}x{art.height}  bright centre in art ({bcx:.0f},{bcy:.0f}) "
          f"[art centre {art.width/2:.0f},{art.height/2:.0f}]  placed at ({left},{top})")
    return canvas


def main():
    hue = next((a.split("=", 1)[1] for a in sys.argv if a.startswith("--hue=")), "green")
    src = next((a.split("=", 1)[1] for a in sys.argv if a.startswith("--src=")), None)
    if src is None:
        cands = sorted(f for f in os.listdir(RENDERS) if f.startswith(f"quantum_icon_{hue}_"))
        if not cands:
            sys.exit(f"no render for {hue} in {RENDERS}")
        src = os.path.join(RENDERS, cands[-1])
    os.makedirs(EXPORT, exist_ok=True)

    im = Image.open(src)
    print(f"[icon] source {os.path.basename(src)} {im.size}")
    im = crop_frame(im)
    print(f"[icon] frame cropped -> {im.size}")
    im = to_phosphor(im, TOKENS[hue])
    out = fit_safe_zone(im)

    a = np.asarray(out).astype(np.float32)
    lit = a[:, :, 3] > 25
    print(f"[icon] ink coverage {100 * lit.mean():.1f}% of canvas  (safe zone {SAFE:.0%})")
    uniq = np.unique(a[:, :, :3][lit].astype(np.uint8).reshape(-1, 3), axis=0)
    off = [c for c in uniq if not (c[0] <= TOKENS[hue][0] and c[1] <= TOKENS[hue][1] and c[2] <= TOKENS[hue][2])]
    print(f"[icon] off-token pixels: {len(off)}")

    dst = os.path.join(EXPORT, f"ic_launcher_foreground_{hue}.png")
    out.save(dst)
    print(f"[icon] wrote {dst}")
    if hue == "green":
        installed = os.path.join(RES, "drawable-nodpi", "ic_launcher_foreground.png")
        out.save(installed)
        print(f"[icon] installed {installed}")


if __name__ == "__main__":
    main()
