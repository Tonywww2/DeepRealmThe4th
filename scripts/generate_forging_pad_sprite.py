"""Draw a crisp 16×16 leather forging pad for block and item textures."""

from pathlib import Path
from generate_astral_expansion_sprites import write_png

ROOT = Path(__file__).resolve().parent.parent
INK = (61, 37, 27, 255)
EDGE = (113, 69, 43, 255)
LEATHER = (165, 107, 61, 255)
LIGHT = (199, 144, 88, 255)
STITCH = (226, 191, 130, 255)
TRANSPARENT = (0, 0, 0, 0)


def sprite():
    pixels = [[TRANSPARENT for _ in range(16)] for _ in range(16)]
    for y in range(2, 14):
        for x in range(2, 14):
            pixels[y][x] = INK if x in (2, 13) or y in (2, 13) else EDGE if x in (3, 12) or y in (3, 12) else LEATHER
    for y in range(5, 11):
        for x in range(5, 11):
            pixels[y][x] = LIGHT
    for x, y in ((4, 4), (7, 4), (10, 4), (4, 7), (11, 7), (4, 10), (11, 10),
                 (5, 11), (8, 11), (11, 11)):
        pixels[y][x] = STITCH
    pixels[6][6] = EDGE
    pixels[9][9] = EDGE
    return pixels


if __name__ == "__main__":
    pixels = sprite()
    for folder in (ROOT / "art/source/item", ROOT / "src/main/resources/assets/deeprealm_4th/textures/item",
                   ROOT / "src/main/resources/assets/deeprealm_4th/textures/block"):
        write_png(folder / "forging_pad.png", pixels)
