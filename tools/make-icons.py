#!/usr/bin/env python3
"""ÖZKEY premium ikonka generatori (PIL). Ishga tushirish: python3 tools/make-icons.py
Natija: mipmap-*/ic_launcher(.png|_round.png), drawable-nodpi/{ozkey_launcher,logo_app,ic_launcher_foreground,ic_launcher_monochrome}.png, store/icon-512.png"""
import math, os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app/src/main/res")
FONT = "/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf"
S = 1024
NAVY_T, NAVY_B = (14, 34, 86), (5, 12, 36)
BLUE, CYAN = (47, 128, 255), (56, 208, 255)
FLAG_BLUE, FLAG_GREEN = (0, 120, 220), (0, 170, 90)

def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def vgrad(size, top, bot):
    im = Image.new("RGBA", size)
    px = im.load()
    for y in range(size[1]):
        c = lerp(top, bot, y / (size[1] - 1))
        for x in range(size[0]): px[x, y] = c + (255,)
    return im

def glow(layer, radius, strength=1.0):
    g = layer.filter(ImageFilter.GaussianBlur(radius))
    if strength != 1.0:
        a = g.getchannel("A").point(lambda v: min(255, int(v * strength)))
        g.putalpha(a)
    return g

def flag_ribbon(size, y0, amp, thick):
    """Egilgan bayroq lentasi (ko'k / oq / yashil) — milliy ruh, minimal."""
    w, h = size
    lay = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    cols = [FLAG_BLUE + (255,), (245, 248, 255, 255), FLAG_GREEN + (255,)]
    for i, col in enumerate(cols):
        top, bot = [], []
        for x in range(0, w + 8, 8):
            t = x / w
            off = math.sin(t * math.pi * 1.6 + 0.4) * amp
            top.append((x, y0 + off + i * thick))
            bot.append((x, y0 + off + (i + 1) * thick))
        d.polygon(top + bot[::-1], fill=col)
    return lay

def keyboard_glyph(size, box, color=(255, 255, 255, 255), lw=None):
    x0, y0, x1, y1 = box
    lw = lw or int((x1 - x0) * 0.045)
    lay = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    d.rounded_rectangle(box, radius=int((y1 - y0) * 0.2), outline=color, width=lw)
    w, h = x1 - x0, y1 - y0
    kw = w * 0.085
    for r in range(2):
        n = 7 if r == 0 else 6
        for i in range(n):
            cx = x0 + w * (0.5 + (i - (n - 1) / 2) * 0.115)
            cy = y0 + h * (0.34 + r * 0.2)
            d.rounded_rectangle((cx - kw / 2, cy - kw / 2, cx + kw / 2, cy + kw / 2), radius=int(kw * 0.25), fill=color)
    d.rounded_rectangle((x0 + w * 0.27, y0 + h * 0.66, x1 - w * 0.27, y0 + h * 0.66 + kw * 0.8), radius=int(kw * 0.3), fill=color)
    return lay

def text_layer(size, text, cx, cy, width, color=(255, 255, 255, 255)):
    px = 10
    while True:
        f = ImageFont.truetype(FONT, px)
        bb = f.getbbox(text)
        if bb[2] - bb[0] >= width: break
        px += 4
    lay = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    bb = d.textbbox((0, 0), text, font=f)
    d.text((cx - (bb[2] - bb[0]) / 2 - bb[0], cy - (bb[3] - bb[1]) / 2 - bb[1]), text, font=f, fill=color)
    return lay

def content(size, scale=1.0, with_ribbon=True):
    """Ikonka markazi (shaffof fonda): klaviatura + ÖZKEY (+ bayroq lentasi)."""
    n = size[0]
    lay = Image.new("RGBA", size, (0, 0, 0, 0))
    c = n / 2
    k = scale * n
    if with_ribbon:
        rib = flag_ribbon(size, c - k * 0.40, k * 0.035, k * 0.045)
        m = Image.new("L", size, 0)
        ImageDraw.Draw(m).rounded_rectangle((c - k * 0.30, c - k * 0.45, c + k * 0.30, c - k * 0.22), radius=int(k * 0.06), fill=255)
        rib.putalpha(Image.composite(rib.getchannel("A"), Image.new("L", size, 0), m))
        lay.alpha_composite(rib)
    gl = keyboard_glyph(size, (c - k * 0.26, c - k * 0.20, c + k * 0.26, c + k * 0.08), (255, 255, 255, 255))
    lay.alpha_composite(glow(keyboard_glyph(size, (c - k * 0.26, c - k * 0.20, c + k * 0.26, c + k * 0.08), CYAN + (255,)), k * 0.02, 1.4))
    lay.alpha_composite(gl)
    t = text_layer(size, "ÖZKEY", c, c + k * 0.255, k * 0.56)
    lay.alpha_composite(glow(text_layer(size, "ÖZKEY", c, c + k * 0.255, k * 0.56, CYAN + (255,)), k * 0.015, 1.1))
    lay.alpha_composite(t)
    return lay

def full_icon(shape):
    bg = vgrad((S, S), NAVY_T, NAVY_B)
    # yengil elektr-ko'k yorug'lik
    spot = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(spot).ellipse((S * 0.15, S * 0.05, S * 0.85, S * 0.65), fill=BLUE + (70,))
    bg.alpha_composite(spot.filter(ImageFilter.GaussianBlur(120)))
    bg.alpha_composite(content((S, S), 1.12))
    mask = Image.new("L", (S, S), 0)
    md = ImageDraw.Draw(mask)
    if shape == "round": md.ellipse((0, 0, S - 1, S - 1), fill=255)
    else: md.rounded_rectangle((0, 0, S - 1, S - 1), radius=int(S * 0.22), fill=255)
    bg.putalpha(mask)
    # nozik elektr-ko'k chegara
    ring = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rd = ImageDraw.Draw(ring)
    if shape == "round": rd.ellipse((6, 6, S - 7, S - 7), outline=BLUE + (170,), width=10)
    else: rd.rounded_rectangle((6, 6, S - 7, S - 7), radius=int(S * 0.22), outline=BLUE + (170,), width=10)
    bg.alpha_composite(ring)
    return bg

def save(im, path, size):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.resize((size, size), Image.LANCZOS).save(path, optimize=True)

sq, rd = full_icon("square"), full_icon("round")
for d, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    save(sq, f"{RES}/mipmap-{d}/ic_launcher.png", px)
    save(rd, f"{RES}/mipmap-{d}/ic_launcher_round.png", px)
save(sq, f"{RES}/drawable-nodpi/ozkey_launcher.png", 512)
save(sq, f"{RES}/drawable-nodpi/logo_app.png", 512)
save(sq, f"{ROOT}/store/icon-512.png", 512)

# Adaptive: foreground 108dp (432px) — mazmun xavfsiz zonada (markaziy 66dp)
FG = 1728
fg = Image.new("RGBA", (FG, FG), (0, 0, 0, 0))
fg.alpha_composite(content((FG, FG), 0.62))
fg.resize((432, 432), Image.LANCZOS).save(f"{RES}/drawable-nodpi/ic_launcher_foreground.png", optimize=True)
# Monoxrom (Android 13 mavzuli ikonka): faqat shakl, oq
mono = Image.new("RGBA", (FG, FG), (0, 0, 0, 0))
c = FG / 2; k = FG * 0.62
mono.alpha_composite(keyboard_glyph((FG, FG), (c - k * 0.26, c - k * 0.28, c + k * 0.26, c + k * 0.0), (255, 255, 255, 255)))
mono.alpha_composite(text_layer((FG, FG), "ÖZKEY", c, c + k * 0.2, k * 0.56))
mono.resize((432, 432), Image.LANCZOS).save(f"{RES}/drawable-nodpi/ic_launcher_monochrome.png", optimize=True)
print("OK")
