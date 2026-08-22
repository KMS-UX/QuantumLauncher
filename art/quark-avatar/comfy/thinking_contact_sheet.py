"""
Build the THINKING-candidate comparison sheet the Director judges from.

Head-crop only, at a consistent crop box, because that is the whole decision: every candidate is
the same plate with a different mouth, and at full-bust size the difference between them is a few
dozen pixels that a side-by-side of whole figures actively hides. The shipped SCAN plate is the
leftmost tile in every row so each candidate is read against what it would replace, not in
isolation.

Usage:
    python thinking_contact_sheet.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
STATE = os.path.join(HERE, "..", "renders", "state")
REF = os.path.join(HERE, "..", "reference", "QUARK_HOLOGRAMBUST_FRONT.png")

# The face occupies roughly this fraction of the 1024x1024 plate. Fixed, not per-image, so a shifted
# feature shows up as a shift rather than being re-centred away by the crop.
CROP = (0.34, 0.20, 0.66, 0.52)   # l, t, r, b as fractions
TILE = 420
LABEL_H = 34
PAD = 10
BG = (16, 16, 20)
FG = (235, 235, 240)


def head(path):
    im = Image.open(path).convert("RGB")
    w, h = im.size
    box = (int(CROP[0] * w), int(CROP[1] * h), int(CROP[2] * w), int(CROP[3] * h))
    return im.crop(box).resize((TILE, TILE), Image.LANCZOS)


def font(size):
    for name in ("consola.ttf", "arial.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def sheet(tiles, out):
    cols = len(tiles)
    W = cols * TILE + (cols + 1) * PAD
    H = TILE + LABEL_H + 2 * PAD
    canvas = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(canvas)
    f = font(17)
    for i, (label, path) in enumerate(tiles):
        x = PAD + i * (TILE + PAD)
        canvas.paste(head(path), (x, PAD))
        draw.text((x + 4, PAD + TILE + 7), label, fill=FG, font=f)
    canvas.save(out)
    print(f"wrote {out}  ({W}x{H})")
    return out


if __name__ == "__main__":
    p = lambda n: os.path.join(STATE, n)
    sheet(
        [
            ("REF / IDLE (source)", REF),
            ("A  pressed line  d30", p("quark_state_think_a.png")),
            ("B  half-smirk  d30", p("quark_state_think_b.png")),
            ("B  half-smirk  d42", p("quark_state_think_b_d42.png")),
            ("B  half-smirk  d52", p("quark_state_think_b_d52.png")),
            ("C  tongue/cheek  d55", p("quark_state_think_c2_d55.png")),
        ],
        os.path.join(STATE, "thinking_candidates_sheet.png"),
    )
