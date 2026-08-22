"""
Chroma-key the hologram reference art -- Phase 8 of the QUARK avatar track.

`reference/QUARK_BUST_HOLOGRAM.png` is delivered on a flat magenta key, which is a far better
matte source than anything a model can infer: the subject is entirely blue/cyan and the background
is pure magenta, so R-G separates them outright. Measured on the file:

    background   R-G = +235 .. +238
    face         R-G = -52
    hair         R-G = -48
    base rings   R-G = -35

A threshold anywhere in the middle of that gap is exact. This replaces the BiRefNet matting the
TripoSplat pipeline uses for the photographic references -- on this art it is not an approximation.

Two things this does beyond thresholding:

* **Despill.** Even a clean key leaves a magenta fringe on semi-transparent edges (hair, the
  projection streaks). Clamping R to G removes it without touching the subject, because the subject
  never has R > G anywhere in this image.
* **Drops the base rings.** The art includes the projection housing's own concentric floor rings.
  They are correct for the ART and wrong for a RECONSTRUCTION -- a flat disc seen from one angle
  becomes a smear of junk gaussians hanging under the bust. The rings are cropped off here and the
  housing is drawn live instead by `ui/scene/QuarkHologramOverlay.kt`, where it stays crisp, sits in
  the active phosphor hue, and costs nothing.

Usage:
    python chroma_key.py ../reference/QUARK_BUST_HOLOGRAM.png out.png [--keep-base]
"""
import sys

import numpy as np
from PIL import Image

# Midpoint of a gap that runs from about -80 (subject) to +235 (key). Nowhere near either edge.
KEY_THRESHOLD_LO = 40
KEY_THRESHOLD_HI = 120

# Where the projection base begins, as a fraction of the keyed subject's height. Set by LOOKING at
# the keyed matte (`renders/splat/holo_base_crop.png`), not by a rule: the art deliberately dissolves
# the torso INTO the ring base, so there is no coverage discontinuity to detect -- an automatic scan
# was tried first and found nothing, because the rings are narrower than the shoulders. Override
# with --base-cut=<fraction> if the framing of a future plate differs.
BASE_CUT_FRACTION = 0.915


def key_magenta(img: Image.Image, drop_base: bool = True, box=None) -> Image.Image:
    a = np.asarray(img.convert("RGB")).astype(np.float32)
    r, g, b = a[:, :, 0], a[:, :, 1], a[:, :, 2]

    key = r - g
    alpha = np.clip((KEY_THRESHOLD_HI - key) / (KEY_THRESHOLD_HI - KEY_THRESHOLD_LO), 0.0, 1.0)

    # Despill: this subject never has R > G, so clamping is safe and removes the edge fringe.
    r = np.minimum(r, g)

    if drop_base:
        cut = int(alpha.shape[0] * BASE_CUT_FRACTION)
        alpha[cut:, :] = 0.0
        print(f"[key] projection base cropped at row {cut} of {alpha.shape[0]}")

    out = np.dstack([r, g, b, alpha * 255.0]).astype(np.uint8)
    im = Image.fromarray(out, "RGBA")

    # Tight-crop to the subject so TripoSplat's fixed point budget is spent on the figure and not
    # on empty frame -- crop tightness is the ONLY lever that sharpens the reconstruction.
    # A caller can FORCE the crop box. That matters for the expression state plates: keyed
    # independently they each get their own tight bbox, and the avatar then jumps by a few pixels
    # every time the state changes. A shared box makes the set register.
    if box is None:
        ys, xs = np.where(alpha > 0.5)
        box = (int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1))
    print(f"[key] subject bbox {box} of {im.size}, coverage {(alpha > 0.5).mean() * 100:.1f}%")
    return im.crop(box)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    src, dst = sys.argv[1], sys.argv[2]
    for arg in sys.argv:
        if arg.startswith("--base-cut="):
            BASE_CUT_FRACTION = float(arg.split("=", 1)[1])
    forced = next((a.split("=", 1)[1] for a in sys.argv if a.startswith("--box=")), None)
    box = tuple(int(v) for v in forced.split(",")) if forced else None
    keyed = key_magenta(Image.open(src), drop_base="--keep-base" not in sys.argv, box=box)
    keyed.save(dst)
    print(f"[key] wrote {dst} {keyed.size}")
