"""Create crisp, deterministic 16x16 progression sprites and their item models."""

from __future__ import annotations

import json
from pathlib import Path
import generate_astral_expansion_sprites as base


ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "art/source/item"
TEXTURES = ROOT / "src/main/resources/assets/deeprealm_4th/textures/item"
MODELS = ROOT / "src/main/resources/assets/deeprealm_4th/models/item"
BLOCK = ROOT / "src/main/resources/assets/deeprealm_4th/textures/block"
PREVIEW = ROOT / "docs/assets/astral-progression-16x-preview.png"
CLEAR = (0, 0, 0, 0)

base.MARKS.update({
    "cross": ["...X...", "...X...", ".XXXXX.", "...X...", "...X...", ".......", "......."],
    "scope": [".XXX...", "...XX..", "...X.X.", "..X....", ".X.....", ".X.....", "......."],
    "triangle": ["...X...", "..X.X..", ".X...X.", ".XXXXX.", ".......", ".......", "......."],
    "core": ["..XXX..", ".X...X.", ".X.o.X.", ".X...X.", "..XXX..", ".......", "......."],
    "six": [".X.X.X.", ".......", ".X.X.X.", ".......", ".......", ".......", "......."],
})

SPECS = [
    ("star_slurry_blank", "magenta", "white", "round", "core"),
    ("astral_lens", "white", "magenta", "round", "eye"),
    ("stabilized_star_slurry", "magenta", "gold", "hex", "core"),
    ("crux_projection", "white", "red", "square", "cross"),
    ("telescopium_projection", "white", "blue", "square", "scope"),
    ("triangulum_australe_projection", "white", "teal", "square", "triangle"),
    ("crux_fragment", "red", "white", "shard", "cross"),
    ("telescopium_fragment", "blue", "white", "shard", "scope"),
    ("triangulum_australe_fragment", "teal", "white", "shard", "triangle"),
    ("unfinished_etched_blank", "blue", "magenta", "square", "steps"),
    ("unfinished_breath_blank", "amber", "magenta", "round", "breath"),
    ("convergent_facet_gem", "red", "lavender", "hex", "cross"),
    ("gathered_radiance_gem", "magenta", "blue", "hex", "scope"),
    ("balance_crystal_gem", "amber", "teal", "diamond", "triangle"),
    ("etched_step_core", "blue", "gold", "square", "steps"),
    ("full_breath_core", "amber", "white", "round", "breath"),
    ("convergent_facet_core", "red", "gold", "hex", "core"),
    ("gathered_radiance_core", "magenta", "gold", "hex", "core"),
    ("balance_core", "teal", "amber", "diamond", "triangle"),
    ("reflected_radiance_core", "magenta", "white", "round", "twin"),
    ("sixfold_balance_core", "gold", "white", "round", "six"),
]


def empty():
    return [[CLEAR for _ in range(16)] for _ in range(16)]


def pixel(p, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        p[y][x] = (*color, 255)


def bottle(liquid):
    p = empty()
    dark, glass, highlight = (35, 37, 59), (139, 163, 187), (232, 239, 245)
    for y in range(3, 14):
        span = (6, 9) if y < 5 else (4, 11) if y < 7 else (3, 12) if y < 11 else (4, 11) if y < 13 else (5, 10)
        for x in range(span[0], span[1] + 1):
            edge = x in span or y in (3, 13)
            color = dark if edge else glass if y < 7 else liquid[1] if y > 10 else liquid[2]
            pixel(p, x, y, color)
    for x in range(5, 11): pixel(p, x, 2, dark)
    pixel(p, 5, 5, highlight)
    pixel(p, 5, 8, liquid[4])
    pixel(p, 9, 9, liquid[4])
    return p


def frame():
    p = empty()
    brass = base.PALETTES["gold"]
    for y in range(2, 14):
        for x in range(2, 14):
            if x in (2, 13) or y in (2, 13): pixel(p, x, y, brass[0])
            elif x in (3, 12) or y in (3, 12): pixel(p, x, y, brass[3])
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        pixel(p, x, y, brass[4])
    return p


def seep():
    p = empty()
    shell = (48, 41, 64)
    fill = (109, 39, 143)
    glint = (216, 140, 243)
    for y in range(16):
        for x in range(16):
            n = (x * 19 + y * 37 + x * y * 7) % 11
            pixel(p, x, y, (56 + n, 51 + n, 66 + n))
    for y in range(4, 12):
        for x in range(3, 13):
            if (x - 8) ** 2 + (y - 8) ** 2 < 22:
                pixel(p, x, y, shell if x in (3, 12) or y in (4, 11) else fill)
    for x, y in ((5, 6), (6, 5), (7, 5), (9, 8), (10, 9), (7, 10)):
        pixel(p, x, y, glint)
    return p


def save_item(item_id, pixels):
    for directory in (SOURCE, TEXTURES):
        base.write_png(directory / f"{item_id}.png", pixels)
    MODELS.mkdir(parents=True, exist_ok=True)
    (MODELS / f"{item_id}.json").write_text(json.dumps({
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"deeprealm_4th:item/{item_id}"}}, indent=2) + "\n", encoding="utf-8")


def main():
    sprites = []
    specials = [
        ("star_slurry", bottle(base.PALETTES["magenta"])),
        ("mimetic_star_slurry", bottle(base.PALETTES["teal"])),
        ("projection_frame", frame()),
    ]
    for item_id, pixels in specials:
        save_item(item_id, pixels)
        sprites.append(pixels)
    save_item("projection_frame_shell", specials[2][1])
    (MODELS / "projection_frame.json").write_text(
        json.dumps({"parent": "minecraft:builtin/entity"}, indent=2) + "\n", encoding="utf-8")
    for item_id, primary, accent, shape, mark in SPECS:
        pixels = base.draw(primary, accent, shape, mark)
        save_item(item_id, pixels)
        sprites.append(pixels)
    BLOCK.mkdir(parents=True, exist_ok=True)
    base.write_png(BLOCK / "star_slurry_seep.png", seep())
    # Separate preview from the previous expansion sheet.
    old = base.PREVIEW
    base.PREVIEW = PREVIEW
    base.preview(sprites)
    base.PREVIEW = old
    print(f"Generated {len(sprites)} independent 16x16 item sprites and one 16x16 block texture")


if __name__ == "__main__":
    main()
