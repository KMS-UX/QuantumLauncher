"""
Geometry preview for the APPS-grid themed pack (AppGlyphs.kt).

NOT a Compose render: it re-plots the SAME normalised 0..1 coordinates the Kotlin uses, so it checks
the shapes -- do 26 marks read as 26 different things at the 34dp they deploy at, and do any collapse
at small size. Compose remains the only thing that proves Compose draws them; the Fold 6 is the judge.
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

FG = (0, 255, 0)
BG = (2, 4, 2)


def render(name, S):
    im = Image.new("RGB", (S, S), BG)
    d = ImageDraw.Draw(im)
    sw = max(1, round(S * 0.075))
    tw = max(1, round(S * 0.055))

    def P(x, y):
        return (x * S, y * S)

    def L(a, b, width=None):
        d.line([P(*a), P(*b)], fill=FG, width=width or tw)

    def PATH(pts, width=None, close=False):
        xy = [P(*p) for p in pts]
        if close:
            xy = xy + [xy[0]]
        d.line(xy, fill=FG, width=width or sw, joint="curve")

    def RECT(x, y, w_, h_, width=None):
        d.rectangle([P(x, y), P(x + w_, y + h_)], outline=FG, width=width or sw)

    def CIRC(cx, cy, r, fill=False, width=None):
        b = [P(cx - r, cy - r), P(cx + r, cy + r)]
        if fill:
            d.ellipse(b, fill=FG)
        else:
            d.ellipse(b, outline=FG, width=width or sw)

    def ARC(x, y, w_, h_, a0, a1, width=None):
        d.arc([P(x, y), P(x + w_, y + h_)], a0, a1, fill=FG, width=width or tw)

    if name == "Messages":
        PATH([(.14, .7), (.14, .24), (.86, .24), (.86, .7), (.42, .7), (.26, .86), (.26, .7)], close=True)
        for x in (.34, .5, .66):
            CIRC(x, .47, .035, fill=True)
    elif name == "Mail":
        RECT(.12, .26, .76, .48)
        PATH([(.12, .26), (.5, .56), (.88, .26)], width=tw)
    elif name == "Contacts":
        CIRC(.5, .34, .15)
        PATH([(.2, .84), (.35, .60), (.5, .56), (.65, .60), (.8, .84)])
    elif name == "Calls":
        PATH([(.26, .16), (.42, .3), (.32, .46), (.44, .62), (.56, .7), (.7, .6), (.86, .76), (.6, .9), (.34, .62), (.26, .16)])
    elif name == "Camera":
        RECT(.1, .3, .8, .46)
        CIRC(.5, .53, .15, width=tw)
        L((.34, .3), (.42, .2)); L((.42, .2), (.58, .2)); L((.58, .2), (.66, .3))
    elif name == "Gallery":
        RECT(.12, .24, .76, .52)
        PATH([(.16, .72), (.38, .46), (.54, .62), (.68, .5), (.84, .72)], width=tw)
        CIRC(.68, .34, .05, fill=True)
    elif name == "Music":
        L((.4, .76), (.4, .2), sw); L((.4, .2), (.76, .28), sw); L((.76, .28), (.76, .66), sw)
        CIRC(.3, .76, .1, width=tw); CIRC(.66, .66, .1, width=tw)
    elif name == "Video":
        RECT(.1, .3, .56, .42)
        PATH([(.72, .44), (.9, .32), (.9, .7), (.72, .58)], width=tw, close=True)
    elif name == "Recorder":
        d.rounded_rectangle([P(.38, .12), P(.62, .54)], radius=.12 * S, outline=FG, width=sw)
        ARC(.26, .36, .48, .36, 0, 180)
        L((.5, .72), (.5, .88))
    elif name == "Notes":
        RECT(.14, .14, .56, .72)
        for y in (.36, .5):
            L((.26, y), (.58, y))
        PATH([(.58, .82), (.62, .66), (.9, .38), (.98, .46), (.7, .74)], width=tw, close=True)
    elif name == "Calendar":
        RECT(.13, .22, .74, .64)
        L((.13, .4), (.87, .4), sw)
        L((.32, .12), (.32, .28)); L((.68, .12), (.68, .28))
        d.rectangle([P(.3, .52), P(.42, .64)], fill=FG)
    elif name == "Clock":
        CIRC(.5, .52, .35)
        L((.5, .52), (.5, .3)); L((.5, .52), (.66, .6))
    elif name == "Weather":
        PATH([(.24, .66), (.14, .58), (.16, .5), (.28, .38), (.44, .38), (.56, .28), (.72, .42), (.86, .52), (.84, .66), (.24, .66)])
        for x in (.34, .52, .7):
            L((x, .76), (x - .05, .9))
    elif name == "MapPin":
        PATH([(.5, .88), (.28, .62), (.28, .38), (.38, .2), (.62, .2), (.72, .38), (.72, .62), (.5, .88)])
        CIRC(.5, .42, .09, fill=True)
    elif name == "Browser":
        CIRC(.5, .5, .36)
        L((.14, .5), (.86, .5))
        d.ellipse([P(.32, .14), P(.68, .86)], outline=FG, width=tw)
    elif name == "Store":
        PATH([(.2, .36), (.8, .36), (.72, .86), (.28, .86)], close=True)
        PATH([(.36, .36), (.4, .14), (.5, .1), (.6, .14), (.64, .36)], width=tw)
    elif name == "Calculator":
        RECT(.18, .12, .64, .76)
        L((.28, .32), (.72, .32), sw)
        for x in (.36, .5, .64):
            for y in (.52, .7):
                CIRC(x, y, .035, fill=True)
    elif name == "Settings":
        for y, knob in ((.28, .66), (.5, .36), (.72, .58)):
            L((.14, y), (.86, y), sw)
            d.rectangle([P(knob - .045, y - .11), P(knob + .045, y + .11)], fill=FG)
    elif name == "Wallet":
        RECT(.12, .28, .76, .44)
        L((.12, .42), (.88, .42), sw)
        CIRC(.72, .58, .06, fill=True)
    elif name == "Translate":
        L((.16, .72), (.34, .24), sw); L((.34, .24), (.52, .72), sw); L((.24, .56), (.44, .56))
        PATH([(.5, .44), (.9, .44), (.9, .8), (.66, .8), (.56, .92), (.56, .8), (.5, .8)], width=tw, close=True)
    elif name == "Health":
        PATH([(.5, .84), (.2, .62), (.14, .44), (.28, .28), (.42, .3), (.5, .4),
              (.58, .3), (.72, .28), (.86, .44), (.8, .62), (.5, .84)])
    elif name == "Game":
        d.rounded_rectangle([P(.1, .32), P(.9, .68)], radius=.14 * S, outline=FG, width=sw)
        L((.26, .5), (.4, .5)); L((.33, .43), (.33, .57))
        CIRC(.66, .44, .05, fill=True); CIRC(.75, .55, .05, fill=True)
    elif name == "News":
        RECT(.12, .2, .76, .6)
        L((.2, .34), (.5, .34), sw)
        for y in (.48, .6, .72):
            L((.2, y), (.8, y))
    elif name == "Files":
        PATH([(.12, .78), (.12, .26), (.42, .26), (.5, .36), (.88, .36), (.88, .78)], close=True)
    elif name == "Security":
        PATH([(.5, .12), (.84, .28), (.84, .54), (.7, .78), (.5, .9), (.3, .78), (.16, .54), (.16, .28)], close=True)
        L((.36, .52), (.47, .64), sw); L((.47, .64), (.66, .4), sw)
    return im


NAMES = ["Messages", "Mail", "Contacts", "Calls", "Camera", "Gallery", "Music", "Video", "Recorder",
         "Notes", "Calendar", "Clock", "Weather", "MapPin", "Browser", "Store", "Calculator",
         "Settings", "Wallet", "Translate", "Health", "Game", "News", "Files", "Security"]

BIG, SMALL = 104, 34
COLS = 9
PAD, LAB = 10, 16
cellw = BIG + PAD
cellh = BIG + SMALL + LAB + 2 * PAD
rows = (len(NAMES) + COLS - 1) // COLS
sheet = Image.new("RGB", (PAD + COLS * cellw, PAD + rows * cellh), (22, 22, 26))
sd = ImageDraw.Draw(sheet)
try:
    f = ImageFont.truetype("consola.ttf", 11)
except OSError:
    f = ImageFont.load_default()

for i, n in enumerate(NAMES):
    r, c = divmod(i, COLS)
    x = PAD + c * cellw
    y = PAD + r * cellh
    sheet.paste(render(n, BIG), (x, y))
    sheet.paste(render(n, SMALL), (x + (BIG - SMALL) // 2, y + BIG + PAD // 2))
    sd.text((x + 2, y + BIG + SMALL + PAD), n[:14], fill=(228, 228, 234), font=f)

out = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "..", "renders", "icon", "app_glyph_preview.png")
out = os.path.normpath(out)
sheet.save(out)
print("wrote", out, sheet.size)
