"""Draw native-resolution pixel-art GUI textures without image dependencies."""

from pathlib import Path
from generate_astral_expansion_sprites import write_png

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "art/source/gui"
TEXTURES = ROOT / "src/main/resources/assets/deeprealm_4th/textures/gui"


def canvas(width, height, color=(0, 0, 0, 0)):
    return [[color for _ in range(width)] for _ in range(height)]


def rgb(value):
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255, 255)


def rect(pixels, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(len(pixels), y1)):
        for x in range(max(0, x0), min(len(pixels[0]), x1)):
            pixels[y][x] = color


def save(name, pixels):
    for folder in (SOURCE, TEXTURES):
        write_png(folder / f"{name}.png", pixels)


def astral_panel():
    pixels = canvas(48, 48)
    edge, rim, body, glint = map(rgb, (0x172034, 0x786C87, 0x33344D, 0x494A66))
    for tile_y in range(3):
        for tile_x in range(3):
            x0, y0 = tile_x * 16, tile_y * 16
            rect(pixels, x0, y0, x0 + 16, y0 + 16, body)
            if tile_x == 0:
                rect(pixels, x0, y0, x0 + 3, y0 + 16, edge)
                rect(pixels, x0 + 3, y0, x0 + 4, y0 + 16, rim)
            if tile_x == 2:
                rect(pixels, x0 + 13, y0, x0 + 16, y0 + 16, edge)
                rect(pixels, x0 + 12, y0, x0 + 13, y0 + 16, rim)
            if tile_y == 0:
                rect(pixels, x0, y0, x0 + 16, y0 + 3, edge)
                rect(pixels, x0, y0 + 3, x0 + 16, y0 + 4, rim)
            if tile_y == 2:
                rect(pixels, x0, y0 + 13, x0 + 16, y0 + 16, edge)
                rect(pixels, x0, y0 + 12, x0 + 16, y0 + 13, rim)
            if tile_x == 1 and tile_y == 1:
                pixels[y0 + 6][x0 + 11] = glint
                pixels[y0 + 12][x0 + 4] = glint
    return pixels


def astral_slot(outer, inner, top, bottom):
    pixels = canvas(18, 18, rgb(outer))
    rect(pixels, 1, 1, 17, 17, rgb(inner))
    rect(pixels, 1, 1, 17, 2, rgb(top))
    rect(pixels, 1, 1, 2, 17, rgb(top))
    rect(pixels, 1, 16, 17, 17, rgb(bottom))
    rect(pixels, 16, 1, 17, 17, rgb(bottom))
    return pixels


def forging_background():
    p = canvas(236, 205, rgb(0x292131))
    rect(p, 3, 3, 233, 202, rgb(0xB6A58B))
    rect(p, 5, 5, 231, 7, rgb(0xD3BE9A))
    rect(p, 5, 199, 231, 201, rgb(0x8A765F))
    rect(p, 8, 18, 98, 94, rgb(0x665346))
    rect(p, 105, 18, 230, 105, rgb(0x665346))
    rect(p, 108, 21, 227, 102, rgb(0xDAC8A7))
    for x in (9, 225):
        for y in (9, 195):
            rect(p, x, y, x + 3, y + 3, rgb(0x674735))
            p[y][x] = rgb(0xE4C984)
    for x in range(10, 95, 6):
        p[20][x] = rgb(0x9F8063)
        p[91][x] = rgb(0x9F8063)
    for row in range(3):
        for col in range(4):
            forging_slot(p, 15 + col * 18, 29 + row * 18)
    for row in range(3):
        for col in range(9):
            forging_slot(p, 39 + col * 18, 121 + row * 18)
    for col in range(9):
        forging_slot(p, 39 + col * 18, 179)
    return p


def forging_slot(p, x, y):
    rect(p, x - 1, y - 1, x + 17, y + 17, rgb(0x3A2F35))
    rect(p, x, y, x + 16, y + 16, rgb(0xDDD4C2))
    rect(p, x + 1, y + 1, x + 15, y + 2, rgb(0xF2E7D1))
    rect(p, x + 1, y + 14, x + 15, y + 15, rgb(0xBCA98D))


def hammer_button():
    p = canvas(50, 25)
    for state in range(2):
        x = state * 25
        outline = rgb(0x40374E if state == 0 else 0x625878)
        face = rgb(0xD2B77E if state == 0 else 0xE4C984)
        rect(p, x, 0, x + 25, 25, outline)
        rect(p, x + 2, 2, x + 23, 23, face)
        rect(p, x + 5, 5, x + 18, 11, rgb(0x2C3036))
        rect(p, x + 6, 6, x + 17, 10, rgb(0xB4BCC4))
        rect(p, x + 13, 11, x + 16, 15, rgb(0x3B2B22))
        rect(p, x + 14, 11, x + 16, 15, rgb(0x9B6B42))
        rect(p, x + 15, 15, x + 18, 20, rgb(0x3B2B22))
        rect(p, x + 16, 15, x + 18, 20, rgb(0x9B6B42))
    return p


def jei_process_icons():
    p = canvas(64, 16)
    blue = rgb(0x9CB9CF)
    for y in range(7, 10):
        for x in range(2, 21):
            p[y][x] = blue
    for tip in range(6):
        x = 19 + tip
        for y in range(7 - tip, 10 + tip):
            if 0 <= y < 16:
                p[y][x] = blue
    for y in range(16):
        for x in range(25):
            p[y][25 + x] = p[y][24 - x]
    outline, body, pressed = map(rgb, (0x20232B, 0xE4E6EA, 0x555B68))
    rect(p, 52, 0, 57, 1, outline)
    rect(p, 51, 1, 58, 10, outline)
    rect(p, 52, 10, 57, 11, outline)
    rect(p, 52, 1, 57, 9, body)
    rect(p, 55, 1, 57, 5, pressed)
    rect(p, 54, 1, 55, 5, outline)
    rect(p, 53, 3, 54, 5, outline)
    return p


def jei_hammer():
    p = canvas(16, 16)
    dark, steel, light, wood = map(rgb, (0x2C3036, 0x8998A4, 0xD8E0E2, 0x9B6B42))
    rect(p, 1, 2, 11, 7, dark)
    rect(p, 2, 3, 10, 6, steel)
    rect(p, 2, 3, 9, 4, light)
    for x, y in ((8, 7), (9, 8), (10, 9), (11, 10), (12, 11), (13, 12)):
        rect(p, x, y, x + 3, y + 3, dark)
        p[y + 1][x + 1] = wood
    return p


if __name__ == "__main__":
    save("astral_panel", astral_panel())
    save("astral_slot_open", astral_slot(0x8E82A8, 0x292D43, 0xA49BB8, 0x584E71))
    save("astral_slot_disabled", astral_slot(0x51475B, 0x211E2C, 0x675D70, 0x302A3A))
    save("astral_slot_player", astral_slot(0x786C87, 0x292D43, 0x9489A0, 0x4D425C))
    save("forging_pad_background", forging_background())
    save("forging_hammer_button", hammer_button())
    save("jei_process_icons", jei_process_icons())
    save("jei_hammer", jei_hammer())
