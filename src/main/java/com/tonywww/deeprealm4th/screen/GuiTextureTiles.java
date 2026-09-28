package com.tonywww.deeprealm4th.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Native-resolution nine-slice panel assembled from a 3×3 texture atlas. */
final class GuiTextureTiles {
    private static final int TILE = 16;
    private static final int ATLAS = 48;

    private GuiTextureTiles() {}

    static void panel(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        if (width < TILE * 2 || height < TILE * 2) throw new IllegalArgumentException("Panel too small");
        piece(graphics, texture, x, y, 0, 0, TILE, TILE);
        piece(graphics, texture, x + width - TILE, y, 32, 0, TILE, TILE);
        piece(graphics, texture, x, y + height - TILE, 0, 32, TILE, TILE);
        piece(graphics, texture, x + width - TILE, y + height - TILE, 32, 32, TILE, TILE);

        for (int dx = TILE; dx < width - TILE; dx += TILE) {
            int partWidth = Math.min(TILE, width - TILE - dx);
            piece(graphics, texture, x + dx, y, 16, 0, partWidth, TILE);
            piece(graphics, texture, x + dx, y + height - TILE, 16, 32, partWidth, TILE);
        }
        for (int dy = TILE; dy < height - TILE; dy += TILE) {
            int partHeight = Math.min(TILE, height - TILE - dy);
            piece(graphics, texture, x, y + dy, 0, 16, TILE, partHeight);
            piece(graphics, texture, x + width - TILE, y + dy, 32, 16, TILE, partHeight);
            for (int dx = TILE; dx < width - TILE; dx += TILE) {
                int partWidth = Math.min(TILE, width - TILE - dx);
                piece(graphics, texture, x + dx, y + dy, 16, 16, partWidth, partHeight);
            }
        }
    }

    private static void piece(GuiGraphics graphics, ResourceLocation texture,
                              int x, int y, int u, int v, int width, int height) {
        graphics.blit(texture, x, y, u, v, width, height, ATLAS, ATLAS);
    }
}
