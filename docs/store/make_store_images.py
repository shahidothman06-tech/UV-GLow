"""Regenerates the UV Glow Google Play graphics from the raw screenshots and the app icon.

    py -3 -m venv .venv
    .venv\\Scripts\\pip install pillow
    .venv\\Scripts\\python docs\\store\\make_store_images.py

Inputs (see README.md in this folder for how the screenshots are captured):
    docs/store/screenshots/raw/01-home.png … 06-settings.png   1080x2400 phone screenshots
    docs/icon-512.png                                          the Play Store icon
Outputs:
    docs/store/screenshots/framed/*.png   1080x1920 store screenshots with captions
    docs/store/feature-graphic.png        1024x500 feature graphic

The script also checks every output: exact size, 24-bit PNG (no transparency, as Play
requires), under 8 MB, and no caption or tagline that claims real UV / ultraviolet light.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = Path(__file__).resolve().parent
DOCS = HERE.parent
RAW = HERE / "screenshots" / "raw"
FRAMED = HERE / "screenshots" / "framed"
ICON = DOCS / "icon-512.png"
FEATURE = HERE / "feature-graphic.png"

# The app's colours.
BACKGROUND = (0x20, 0x1E, 0x1D)   # icon background
VIOLET = (0x8C, 0x58, 0xCE)       # icon beams
VIOLET_BRIGHT = (0xA8, 0x55, 0xF7)  # in-app accent
LAVENDER = (0xD9, 0xC8, 0xF7)
WHITE = (0xF3, 0xF2, 0xF2)

SCREENS = [
    ("01-home", "Instant violet glow screen light"),
    ("02-effects", "6 glowing light effects"),
    ("03-themes", "Pick your colour theme"),
    ("04-presets", "Save your favourite presets"),
    ("05-timer", "Auto-off timer saves battery"),
    ("06-settings", "Simple, private, no account"),
]
TITLE = "UV Glow"
TAGLINE = "Violet screen light & flashlight"

# The app is a screen light effect: store text must never claim it emits UV light.
FORBIDDEN = re.compile(r"\b(uv light|real uv|ultraviolet|uv lamp|uv torch|black ?light (lamp|torch))\b", re.I)

FONTS = Path("C:/Windows/Fonts")


def font(bold: bool, size: int) -> ImageFont.FreeTypeFont:
    names = ["segoeuib.ttf", "arialbd.ttf"] if bold else ["seguisb.ttf", "segoeui.ttf", "arial.ttf"]
    for name in names:
        path = FONTS / name
        if path.exists():
            return ImageFont.truetype(str(path), size)
    return ImageFont.load_default(size)


def glow(size: tuple[int, int], box: tuple[int, int, int, int], colour, alpha: int, blur: int) -> Image.Image:
    """A soft coloured ellipse on a transparent layer."""
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    ImageDraw.Draw(layer).ellipse(box, fill=(*colour, alpha))
    return layer.filter(ImageFilter.GaussianBlur(blur))


def rounded(image: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, image.width - 1, image.height - 1), radius, fill=255)
    out = image.convert("RGBA")
    out.putalpha(mask)
    return out


def wrap(draw: ImageDraw.ImageDraw, text: str, fnt, max_width: int) -> list[str]:
    """One line if it fits, else the two-line split whose longer line is shortest (balanced)."""
    if draw.textlength(text, font=fnt) <= max_width:
        return [text]
    words = text.split()
    splits = [(" ".join(words[:i]), " ".join(words[i:])) for i in range(1, len(words))]
    return list(min(splits, key=lambda s: max(draw.textlength(s[0], font=fnt), draw.textlength(s[1], font=fnt))))


def framed(raw_path: Path, caption: str, out_path: Path) -> None:
    w, h = 1080, 1920
    canvas = Image.new("RGBA", (w, h), (*BACKGROUND, 255))
    # Soft violet glow behind the caption and behind the phone.
    canvas.alpha_composite(glow((w, h), (90, -260, 990, 520), VIOLET, 120, 120))
    canvas.alpha_composite(glow((w, h), (60, 700, 1020, 1900), VIOLET_BRIGHT, 95, 160))

    draw = ImageDraw.Draw(canvas)
    fnt = font(True, 82)
    lines = wrap(draw, caption, fnt, 900)
    line_h = 98
    top = 120 + (2 - len(lines)) * line_h // 2  # one- and two-line captions sit at the same optical height
    for i, line in enumerate(lines):
        draw.text((w // 2, top + i * line_h), line, font=fnt, fill=WHITE, anchor="ma")

    # Screenshot: full screen, rounded corners, a thin edge and a violet halo.
    shot = Image.open(raw_path).convert("RGB")
    shot_h = 1400
    shot_w = round(shot.width * shot_h / shot.height)
    shot = shot.resize((shot_w, shot_h), Image.LANCZOS)
    x, y = (w - shot_w) // 2, h - shot_h - 60
    halo = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(halo).rounded_rectangle((x - 14, y - 14, x + shot_w + 14, y + shot_h + 14), 62, fill=(*VIOLET_BRIGHT, 150))
    canvas.alpha_composite(halo.filter(ImageFilter.GaussianBlur(28)))
    frame = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(frame).rounded_rectangle((x - 3, y - 3, x + shot_w + 3, y + shot_h + 3), 51, fill=(0x4A, 0x40, 0x5C, 255))
    canvas.alpha_composite(frame)
    canvas.alpha_composite(rounded(shot, 48), (x, y))

    canvas.convert("RGB").save(out_path, "PNG", optimize=True)


def beam(size, origin, angle_deg: float, length: int, width: int, colour, alpha: int) -> Image.Image:
    """A soft violet beam: a rounded bar from origin, rotated about it, blurred."""
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    ox, oy = origin
    ImageDraw.Draw(layer).rounded_rectangle((ox - width // 2, oy - length, ox + width // 2, oy), width // 2, fill=(*colour, alpha))
    return layer.rotate(angle_deg, center=origin, resample=Image.BICUBIC).filter(ImageFilter.GaussianBlur(18))


def feature_graphic(out_path: Path) -> None:
    w, h = 1024, 500
    canvas = Image.new("RGBA", (w, h), (*BACKGROUND, 255))
    # Beams fan up and out behind everything, from just below the icon.
    origin = (215, 470)
    for angle, length, width, alpha in [(52, 700, 120, 70), (28, 760, 140, 85), (4, 640, 130, 80),
                                        (-22, 820, 150, 90), (-48, 900, 170, 75), (-70, 900, 150, 55)]:
        canvas.alpha_composite(beam((w, h), origin, angle, length, width, VIOLET_BRIGHT, alpha))
    canvas.alpha_composite(glow((w, h), (40, 70, 400, 430), VIOLET, 140, 70))

    icon = Image.open(ICON).convert("RGB").resize((300, 300), Image.LANCZOS)
    canvas.alpha_composite(rounded(icon, 66), (70, 100))

    draw = ImageDraw.Draw(canvas)
    text_x, max_w = 430, 1024 - 430 - 70  # keep text 70 px from the right edge
    title_font = font(True, 112)
    draw.text((text_x, 255), TITLE, font=title_font, fill=WHITE, anchor="ls")
    size = 44
    while draw.textlength(TAGLINE, font=font(False, size)) > max_w and size > 28:
        size -= 1
    draw.text((text_x, 330), TAGLINE, font=font(False, size), fill=LAVENDER, anchor="ls")

    canvas.convert("RGB").save(out_path, "PNG", optimize=True)


def check(path: Path, size: tuple[int, int]) -> str:
    with Image.open(path) as im:
        problems = []
        if im.format != "PNG":
            problems.append(f"format {im.format}")
        if im.size != size:
            problems.append(f"size {im.size}")
        if im.mode != "RGB":
            problems.append(f"mode {im.mode} (Play wants 24-bit, no alpha)")
    mb = path.stat().st_size / 1_000_000
    if mb >= 8:
        problems.append(f"{mb:.1f} MB")
    status = "OK" if not problems else "FAIL: " + ", ".join(problems)
    return f"{status:4} {size[0]}x{size[1]} {mb:5.2f} MB  {path.relative_to(DOCS.parent)}"


def main() -> int:
    for text in [c for _, c in SCREENS] + [TITLE, TAGLINE]:
        if FORBIDDEN.search(text):
            print(f"Refusing: store text claims real UV light: {text!r}")
            return 1
    FRAMED.mkdir(parents=True, exist_ok=True)
    report = []
    for name, caption in SCREENS:
        raw = RAW / f"{name}.png"
        if not raw.exists():
            print(f"Missing screenshot: {raw}")
            return 1
        with Image.open(raw) as im:  # adb screencap saves RGBA; keep sources 24-bit like everything else
            if im.mode != "RGB":
                im.convert("RGB").save(raw, "PNG", optimize=True)
        framed(raw, caption, FRAMED / f"{name}.png")
        report.append(check(FRAMED / f"{name}.png", (1080, 1920)))
    feature_graphic(FEATURE)
    report.append(check(FEATURE, (1024, 500)))
    for name, _ in SCREENS:
        report.append(check(RAW / f"{name}.png", (1080, 2400)) + "  (source only: 9:20 is too tall for Play)")
    print("\n".join(report))
    return 0 if all(line.startswith("OK") for line in report) else 1


if __name__ == "__main__":
    sys.exit(main())
