"""Draw the approved expansion items as deterministic, native 16x16 pixel sprites."""

from __future__ import annotations

import json
from pathlib import Path
import struct
import zlib


ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "art/source/item"
TEXTURES = ROOT / "src/main/resources/assets/deeprealm_4th/textures/item"
MODELS = ROOT / "src/main/resources/assets/deeprealm_4th/models/item"
PREVIEW = ROOT / "docs/assets/astral-expansion-16x-preview.png"

# Outline, shadow, body, light, glint. A small fixed palette keeps every sprite crisp.
PALETTES = {
    "red": ((55, 12, 27), (124, 17, 35), (203, 35, 54), (248, 75, 85), (255, 171, 156)),
    "teal": ((9, 49, 49), (17, 102, 95), (29, 166, 146), (88, 222, 191), (177, 255, 227)),
    "blue": ((10, 30, 75), (19, 65, 149), (31, 112, 221), (81, 170, 255), (184, 225, 255)),
    "amber": ((76, 39, 14), (153, 83, 18), (219, 145, 32), (255, 198, 67), (255, 238, 151)),
    "lavender": ((52, 24, 83), (103, 48, 153), (159, 96, 221), (204, 148, 255), (238, 207, 255)),
    "magenta": ((61, 12, 67), (120, 21, 136), (186, 40, 204), (237, 95, 240), (255, 181, 253)),
    "gold": ((69, 42, 17), (124, 72, 23), (191, 128, 37), (245, 192, 74), (255, 237, 159)),
    "white": ((42, 48, 60), (88, 105, 124), (151, 181, 199), (208, 230, 239), (255, 255, 238)),
}

SHAPES = {
    "hex": {2: (6, 9), 3: (4, 11), 4: (3, 12), 5: (3, 12), 6: (3, 12),
            7: (3, 12), 8: (3, 12), 9: (3, 12), 10: (3, 12), 11: (4, 11), 12: (6, 9)},
    "diamond": {2: (7, 8), 3: (6, 9), 4: (5, 10), 5: (4, 11), 6: (3, 12),
                7: (3, 12), 8: (3, 12), 9: (4, 11), 10: (5, 10), 11: (6, 9), 12: (7, 8)},
    "square": {2: (5, 10), 3: (4, 11), 4: (3, 12), 5: (3, 12), 6: (3, 12),
               7: (3, 12), 8: (3, 12), 9: (3, 12), 10: (3, 12), 11: (4, 11), 12: (5, 10)},
    "tall": {2: (6, 9), 3: (5, 10), 4: (4, 11), 5: (4, 11), 6: (4, 11),
             7: (4, 11), 8: (4, 11), 9: (4, 11), 10: (4, 11), 11: (5, 10), 12: (6, 9)},
    "shield": {2: (5, 10), 3: (3, 12), 4: (3, 12), 5: (3, 12), 6: (3, 12),
               7: (3, 12), 8: (4, 11), 9: (4, 11), 10: (5, 10), 11: (6, 9), 12: (7, 8)},
    "round": {2: (6, 9), 3: (4, 11), 4: (3, 12), 5: (3, 12), 6: (2, 13),
              7: (2, 13), 8: (2, 13), 9: (3, 12), 10: (3, 12), 11: (4, 11), 12: (6, 9)},
    "shard": {2: (8, 9), 3: (7, 10), 4: (6, 11), 5: (5, 11), 6: (4, 12),
              7: (3, 12), 8: (3, 11), 9: (4, 11), 10: (4, 10), 11: (5, 9), 12: (6, 8)},
}

# Each mark is a 7x7 stamp centered on the gem. X uses the accent's light shade;
# o uses its body shade. The base outline and silhouette remain visible at 16x.
MARKS = {
    "bridge": [".......", ".X...X.", ".X...X.", ".XoooX.", ".X...X.", ".......", "......."],
    "twin": [".......", ".XX.XX.", ".Xo.Xo.", ".......", ".XX.XX.", ".Xo.Xo.", "......."],
    "stripe": ["...X...", "..X....", ".X.....", "..X....", "...X...", "....X..", ".....X."],
    "embers": [".......", ".X...o.", ".......", "...X...", ".......", ".o...X.", "......."],
    "balance": ["...X...", "...X...", ".ooX...", "...X...", "...Xoo.", "...X...", "...X..."],
    "ray": [".XXXX..", "....X..", "....X..", "..XXX..", "..X....", "..XXXX.", "......."],
    "edge": ["....X..", "...X...", "..X....", ".X.....", "..X....", "...X...", "......."],
    "anchor": [".XXXXX.", "...X...", "...X...", "...X...", ".X.X.X.", "..XXX..", "......."],
    "arrow": ["...X...", "..XXX..", ".X.X.X.", "...X...", "...X...", "...X...", "......."],
    "ward": [".XXXXX.", ".X...X.", ".X.o.X.", "..X.X..", "...X...", ".......", "......."],
    "widen": [".X.....", ".XX....", ".XXX...", ".XXXX..", ".XXX...", ".XX....", ".X....."],
    "shadow": [".X.....", "..X.o..", "...Xo..", "....X..", ".....X.", ".......", "......."],
    "reflect": [".XXXX..", "....X..", "...X...", "..X....", ".X.....", ".XXXX..", "......."],
    "link": [".X...X.", ".XX.XX.", "..XXX..", "...X...", "..XXX..", ".X...X.", "......."],
    "cluster": [".XX.XX.", ".Xo.Xo.", ".......", "..XXX..", "..XoX..", ".......", "......."],
    "loop": ["..XXX..", ".X...X.", ".X...X.", ".X...X.", "..XXX..", "....X..", "......."],
    "steps": [".XX....", "..X....", "..XXX..", "....X..", "....XXX", "......X", "......."],
    "breath": ["..X.X..", ".X...X.", ".X...X.", "..X.X..", "...X...", ".......", "......."],
    "crack": ["...X...", "..X....", "...XX..", ".....X.", "....X..", "...X...", "......."],
    "three": [".XXXXX.", ".......", ".XXXXX.", ".......", ".XXXXX.", ".......", "......."],
    "eye": [".......", "..XXX..", ".X...X.", ".X.o.X.", "..XXX..", ".......", "......."],
}

SPECS = [
    ("facet_bridge_gem", "red", "lavender", "hex", "bridge"),
    ("twin_mirror_gem", "blue", "magenta", "square", "twin"),
    ("wandering_stripe_gem", "teal", "lavender", "diamond", "stripe"),
    ("ember_remnant_gem", "amber", "red", "shield", "embers"),
    ("balance_shift_gem", "red", "amber", "diamond", "balance"),
    ("returning_ray_gem", "magenta", "blue", "tall", "ray"),
    ("split_edge_gem", "red", "amber", "shard", "edge"),
    ("steady_anchor_gem", "amber", "teal", "hex", "anchor"),
    ("wayfarer_medal", "gold", "teal", "round", "arrow"),
    ("warden_medal", "gold", "amber", "round", "ward"),
    ("vein_amplitude_gem", "red", "red", "hex", "widen"),
    ("wandering_shadow_gem", "teal", "lavender", "diamond", "shadow"),
    ("folded_reflection_gem", "blue", "magenta", "square", "reflect"),
    ("linked_vein_gem", "amber", "red", "tall", "link"),
    ("cluster_mirror_gem", "lavender", "blue", "diamond", "cluster"),
    ("looped_trace_gem", "magenta", "magenta", "tall", "loop"),
    ("etched_step_gem", "blue", "white", "square", "steps"),
    ("full_breath_gem", "amber", "white", "round", "breath"),
    ("last_edge_gem", "red", "white", "shard", "crack"),
    ("well_fed_glow_gem", "teal", "white", "hex", "three"),
    ("nightglow_gem", "lavender", "gold", "round", "eye"),
]


def png_chunk(tag: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data))


def write_png(path: Path, pixels: list[list[tuple[int, int, int, int]]]) -> None:
    height, width = len(pixels), len(pixels[0])
    raw = b"".join(b"\0" + bytes(channel for pixel in row for channel in pixel) for row in pixels)
    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", header)
                     + png_chunk(b"IDAT", zlib.compress(raw, 9)) + png_chunk(b"IEND", b""))


def draw(primary: str, accent: str, shape: str, mark: str) -> list[list[tuple[int, int, int, int]]]:
    pixels = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    spans = SHAPES[shape]
    mask = {(x, y) for y, (left, right) in spans.items() for x in range(left, right + 1)}
    base = PALETTES[primary]
    other = PALETTES[accent]
    for x, y in mask:
        if any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            color = base[0]
        elif y >= 10 or x >= 11:
            color = base[1]
        elif y <= 4 or x <= 4:
            color = base[3]
        else:
            color = base[2]
        pixels[y][x] = (*color, 255)
    # A short glint and a shaded cut make the stone read as faceted, not flat.
    for x, y in ((5, 4), (6, 4), (7, 3), (4, 5)):
        if (x, y) in mask and pixels[y][x][:3] != base[0]:
            pixels[y][x] = (*base[4], 255)
    for y, row in enumerate(MARKS[mark], 4):
        for x, symbol in enumerate(row, 4):
            if symbol != "." and (x, y) in mask:
                pixels[y][x] = (*(other[4] if symbol == "X" else other[2]), 255)
    return pixels


def preview(sprites: list[list[list[tuple[int, int, int, int]]]]) -> None:
    scale, tile, columns = 4, 72, 5
    rows = (len(sprites) + columns - 1) // columns
    sheet = [[(35, 35, 46, 255) for _ in range(columns * tile)] for _ in range(rows * tile)]
    for index, sprite in enumerate(sprites):
        ox = (index % columns) * tile + 4
        oy = (index // columns) * tile + 4
        for y in range(16):
            for x in range(16):
                for dy in range(scale):
                    for dx in range(scale):
                        px = sprite[y][x]
                        if px[3]:
                            sheet[oy + y * scale + dy][ox + x * scale + dx] = px
                        else:
                            checker = 72 if (x + y) % 2 else 86
                            sheet[oy + y * scale + dy][ox + x * scale + dx] = (
                                checker, checker, checker + 9, 255)
    write_png(PREVIEW, sheet)


def main() -> None:
    sprites = []
    for item_id, primary, accent, shape, mark in SPECS:
        pixels = draw(primary, accent, shape, mark)
        sprites.append(pixels)
        write_png(SOURCE / f"{item_id}.png", pixels)
        write_png(TEXTURES / f"{item_id}.png", pixels)
        model = {"parent": "minecraft:item/generated",
                 "textures": {"layer0": f"deeprealm_4th:item/{item_id}"}}
        MODELS.mkdir(parents=True, exist_ok=True)
        (MODELS / f"{item_id}.json").write_text(
            json.dumps(model, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    preview(sprites)
    print(f"Generated {len(sprites)} independent 16x16 item sprites and models")


if __name__ == "__main__":
    main()
