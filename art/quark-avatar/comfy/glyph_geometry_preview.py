"""
Geometry preview for the new QuantumIcons glyphs.

NOT a Compose render -- it re-plots the SAME normalised 0..1 coordinates the Kotlin uses, so it
checks the SHAPES I designed (do they read at 12dp, does the atom's orbit stack turn to mush) and
not that Compose executes them. On-device is still the judge.
"""
import math
import sys

from PIL import Image, ImageDraw, ImageFont

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

FG = (0, 255, 0)
BG = (2, 4, 2)


def draw(name, d, S, sw, tsw):
    def P(fx, fy):
        return (fx * S, fy * S)

    def line(a, b, width=None):
        d.line([P(*a), P(*b)], fill=FG, width=max(1, int(width or tsw)))

    def poly(pts, fill=True, width=None):
        xy = [P(*p) for p in pts]
        if fill:
            d.polygon(xy, fill=FG)
        else:
            d.line(xy + [xy[0]], fill=FG, width=max(1, int(width or sw)))

    def path(pts, width=None):
        d.line([P(*p) for p in pts], fill=FG, width=max(1, int(width or sw)))

    def circle(cx, cy, r, fill=False, width=None):
        b = [(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S]
        if fill:
            d.ellipse(b, fill=FG)
        else:
            d.ellipse(b, outline=FG, width=max(1, int(width or sw)))

    if name == "FocusMacro":
        circle(.5, .5, .22); circle(.5, .5, .07, fill=True)
        line((.06, .5), (.2, .5)); line((.8, .5), (.94, .5))
    elif name == "FocusPortrait":
        circle(.5, .33, .15)
        path([(.2, .86), (.28, .68), (.5, .60), (.72, .68), (.8, .86)])
    elif name == "FocusMid":
        circle(.5, .3, .1, width=tsw)
        line((.5, .4), (.5, .66)); line((.34, .5), (.66, .5))
        line((.5, .66), (.37, .84)); line((.5, .66), (.63, .84))
    elif name == "FocusLandscape":
        path([(.08, .76), (.34, .36), (.5, .58), (.68, .28), (.92, .76)])
        line((.06, .86), (.94, .86))
    elif name == "Infinity":
        circle(.31, .5, .17); circle(.69, .5, .17)
    elif name == "Tilt":
        path([(.16, .62), (.84, .38)])
        line((.1, .8), (.9, .8)); circle(.5, .5, .09, fill=True)
    elif name == "Develop":
        path([(.34, .16), (.34, .42), (.18, .82), (.82, .82), (.66, .42), (.66, .16)])
        line((.28, .16), (.72, .16)); line((.29, .66), (.71, .66))
    elif name == "Swap":
        line((.34, .18), (.34, .82)); line((.66, .18), (.66, .82))
        poly([(.18, .34), (.34, .14), (.5, .34)])
        poly([(.5, .66), (.66, .86), (.82, .66)])
    elif name == "Cycle":
        b = [.18 * S, .18 * S, .82 * S, .82 * S]
        d.arc(b, start=40, end=320, fill=FG, width=max(1, int(sw)))
        poly([(.66, .58), (.9, .62), (.74, .82)])
    elif name == "Atom":
        circle(.5, .5, .11, fill=True)
        for deg in (0, 60, 120):
            layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
            ld = ImageDraw.Draw(layer)
            ld.ellipse([(.5 - .44) * S, (.5 - .17) * S, (.5 + .44) * S, (.5 + .17) * S],
                       outline=FG + (255,), width=max(1, int(tsw)))
            layer = layer.rotate(-deg, resample=Image.BICUBIC, center=(S / 2, S / 2))
            base.alpha_composite(layer)
    elif name == "Charge":
        poly([(.56, .08), (.28, .54), (.48, .54), (.42, .92), (.72, .44), (.52, .44)])
    elif name == "Crosshair":
        circle(.5, .5, .3)
        line((.5, .06), (.5, .94)); line((.06, .5), (.94, .5))
    elif name == "Diamond":
        poly([(.5, .12), (.88, .5), (.5, .88), (.12, .5)], fill=False)
        circle(.5, .5, .1, fill=True)


NAMES = ["Atom", "Charge", "Swap", "Cycle", "Diamond", "Crosshair",
         "FocusMacro", "FocusPortrait", "FocusMid", "FocusLandscape", "Infinity", "Tilt", "Develop"]
SIZES = [96, 48, 20]

PAD, LAB = 10, 20
cellw = 110
sheet = Image.new("RGB", (PAD + len(NAMES) * cellw, PAD + sum(SIZES) + 3 * PAD + LAB), (20, 20, 24))
sd = ImageDraw.Draw(sheet)
try:
    f = ImageFont.truetype("consola.ttf", 11)
except OSError:
    f = ImageFont.load_default()

for c, n in enumerate(NAMES):
    y = PAD
    for S in SIZES:
        base = Image.new("RGBA", (S, S), BG + (255,))
        d = ImageDraw.Draw(base)
        draw(n, d, S, max(1.0, S * 0.09), max(1.0, S * 0.07))
        x = PAD + c * cellw + (cellw - S) // 2
        sheet.paste(base.convert("RGB"), (x, y))
        y += S + PAD
    sd.text((PAD + c * cellw + 2, y - PAD + 4), n[:13], fill=(225, 225, 230), font=f)

out = r'C:\GitHub\QuantumLauncher\art\quark-avatar\renders\icon\glyph_geometry_preview.png'
sheet.save(out)
print("wrote", out, sheet.size)
