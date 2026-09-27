"""Draw the Astral Body's native 16x16 empty Curios slot icon."""

from pathlib import Path

from generate_astral_expansion_sprites import write_png


ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "art/source/slot/base_container.png"
TEXTURE = ROOT / "src/main/resources/assets/deeprealm_4th/textures/slot/base_container.png"
PREVIEW = ROOT / "docs/assets/astral-curios-slot-preview.png"

TRANSPARENT = (0, 0, 0, 0)
SHADOW = (76, 75, 91, 255)
RIM = (113, 111, 131, 255)
LIGHT = (157, 155, 176, 255)
STAR = (190, 189, 207, 255)


def draw() -> list[list[tuple[int, int, int, int]]]:
    pixels = [[TRANSPARENT for _ in range(16)] for _ in range(16)]

    # Open outline echoes the equipped item's short neck and broad vessel.
    for x in range(6, 10):
        pixels[2][x] = LIGHT if x < 8 else RIM
    for y in (3, 4):
        pixels[y][5] = LIGHT
        pixels[y][10] = SHADOW
    pixels[3][6] = RIM
    pixels[3][9] = RIM
    pixels[5][4], pixels[5][11] = LIGHT, SHADOW
    for y in range(6, 12):
        pixels[y][3] = RIM if y < 9 else SHADOW
        pixels[y][12] = SHADOW
    pixels[12][4], pixels[12][11] = RIM, SHADOW
    for x in range(5, 11):
        pixels[13][x] = RIM if x < 8 else SHADOW

    # The empty center stays transparent; the small star identifies the slot.
    for x, y in ((7, 7), (6, 8), (7, 8), (8, 8), (7, 9)):
        pixels[y][x] = STAR if (x, y) == (7, 8) else LIGHT
    return pixels


def main() -> None:
    pixels = draw()
    write_png(SOURCE, pixels)
    write_png(TEXTURE, pixels)

    # Review image only; the shipped Curios texture remains exactly 16x16.
    scale = 12
    background = (86, 86, 91, 255)
    preview = [[background for _ in range(16 * scale)] for _ in range(16 * scale)]
    for y, row in enumerate(pixels):
        for x, color in enumerate(row):
            if color[3]:
                for dy in range(scale):
                    for dx in range(scale):
                        preview[y * scale + dy][x * scale + dx] = color
    write_png(PREVIEW, preview)
    print("Generated Astral Body Curios slot: 16x16, transparent pixel art")


if __name__ == "__main__":
    main()
