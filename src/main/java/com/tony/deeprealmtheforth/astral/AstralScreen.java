package com.tony.deeprealmtheforth.astral;

import com.tony.deeprealmtheforth.platform.items.AstralItemRegistration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Textured by simple cells so addon layouts can change width and height. */
public final class AstralScreen extends AbstractContainerScreen<AstralMenu> {
    private static final int SCORE_PANEL_WIDTH = 124;
    private static final int SCORE_PANEL_GAP = 8;
    private float layoutScale = 1;

    public AstralScreen(AstralMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = AstralMenu.screenWidth(menu.layout());
        imageHeight = 18 + Math.max(menu.layout().height() * 18, 108) + 96;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = Math.max(8, (imageWidth - 162) / 2);
        inventoryLabelY = 21 + Math.max(menu.layout().height() * 18, 108);
    }

    @Override
    protected void init() {
        super.init();
        // Reserve matching space on the left so the container stays centered.
        int sideRoom = SCORE_PANEL_GAP + SCORE_PANEL_WIDTH + 4;
        layoutScale = Math.max(0.1f, Math.min(1f, Math.min(
                width / (float) (imageWidth + sideRoom * 2), height / (float) (imageHeight + 8))));
        leftPos = Math.round((width / layoutScale - imageWidth) / 2);
        topPos = Math.round((height / layoutScale - imageHeight) / 2);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff172034);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3,
                topPos + imageHeight - 3, 0xff33344d);
        ContainerLayout layout = menu.layout();
        int gridX = leftPos + AstralMenu.gridX(layout);
        for (int y = 0; y < layout.height(); y++) {
            for (int x = 0; x < layout.width(); x++) {
                int left = gridX + x * 18;
                int top = topPos + 18 + y * 18;
                graphics.fill(left - 1, top - 1, left + 17, top + 17,
                        layout.isOpen(x, y) ? 0xff8e82a8 : 0xff51475b);
                graphics.fill(left, top, left + 16, top + 16,
                        layout.isOpen(x, y) ? 0xff292d43 : 0xff211e2c);
            }
        }
        int playerX = leftPos + Math.max(8, (imageWidth - 162) / 2);
        int playerY = topPos + 18 + Math.max(layout.height() * 18, 108) + 16;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) drawPlayerCell(graphics, playerX + col * 18, playerY + row * 18);
        }
        for (int col = 0; col < 9; col++) drawPlayerCell(graphics, playerX + col * 18, playerY + 58);
        int panelX = leftPos + imageWidth + SCORE_PANEL_GAP;
        int panelY = topPos + 8;
        graphics.fill(panelX, panelY, panelX + SCORE_PANEL_WIDTH, panelY + 116, 0xff172034);
        graphics.fill(panelX + 3, panelY + 3, panelX + SCORE_PANEL_WIDTH - 3,
                panelY + 113, 0xff33344d);
        int scoreX = panelX + 8;
        graphics.drawString(font, Component.translatable("screen.deeprealm_4th.scores"),
                scoreX, panelY + 8, 0xfff0daaa, false);
        for (ScoreType type : ScoreType.values()) {
            String value = String.format(Locale.ROOT, "%.2f", menu.score(type));
            graphics.drawString(font, Component.translatable("score.deeprealm_4th." + type.id()),
                    scoreX, panelY + 26 + type.ordinal() * 14, AstralScoreColors.argb(type), false);
            graphics.drawString(font, value, panelX + SCORE_PANEL_WIDTH - 8 - font.width(value),
                    panelY + 26 + type.ordinal() * 14, AstralScoreColors.argb(type), false);
        }
    }

    private static void drawPlayerCell(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff786c87);
        graphics.fill(x, y, x + 16, y + 16, 0xff292d43);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if forge {
        renderBackground(graphics);
        //?} else {
        /*renderBackground(graphics, mouseX, mouseY, partialTick);
        *///?}
        graphics.pose().pushPose();
        graphics.pose().scale(layoutScale, layoutScale, 1);
        int scaledX = Math.round(mouseX / layoutScale);
        int scaledY = Math.round(mouseY / layoutScale);
        super.render(graphics, scaledX, scaledY, partialTick);
        String key = null;
        int hoveredMenuIndex = hoveredSlot == null ? -1 : menu.slots.indexOf(hoveredSlot);
        if (hoveredMenuIndex >= 0 && hoveredMenuIndex < menu.layout().size()) {
            int cell = hoveredMenuIndex;
            key = !menu.layout().isOpen(cell) ? "closed_cell"
                    : switch (menu.inactiveReason(cell)) {
                        case 1 -> "suppressed_cell";
                        case 2 -> "duplicate_cell";
                        default -> null;
                    };
        }
        boolean medalPreview = key == null && hoveredMenuIndex >= 0
                && hoveredMenuIndex < menu.layout().size() && hoveredSlot != null
                && (hoveredSlot.getItem().is(AstralItemRegistration.WARRIOR_MEDAL.get())
                    || hoveredSlot.getItem().is(AstralItemRegistration.WAYFARER_MEDAL.get())
                    || hoveredSlot.getItem().is(AstralItemRegistration.WARDEN_MEDAL.get()));
        if ((key != null || medalPreview) && hoveredSlot != null && hoveredSlot.hasItem()) {
            // Keep the item's own tooltip visible and add the cell state as its last line.
            var lines = new ArrayList<>(getTooltipFromContainerItem(hoveredSlot.getItem()));
            if (key != null) {
                lines.add(Component.translatable("screen.deeprealm_4th." + key)
                        .withStyle(ChatFormatting.RED));
            }
            if (medalPreview) {
                if (hoveredSlot.getItem().is(AstralItemRegistration.WARRIOR_MEDAL.get())) {
                    double percent = WarriorMedalFormula.attackFraction(menu.score(ScoreType.STRENGTH)) * 100;
                    lines.add(Component.translatable("tooltip.deeprealm_4th.warrior_medal.preview",
                            AstralTooltips.number(percent)).withStyle(ChatFormatting.GOLD));
                } else if (hoveredSlot.getItem().is(AstralItemRegistration.WAYFARER_MEDAL.get())) {
                    double percent = AstralMedalFormulas.movementFraction(menu.score(ScoreType.AGILITY)) * 100;
                    lines.add(Component.translatable("tooltip.deeprealm_4th.wayfarer_medal.preview",
                            AstralTooltips.number(percent)).withStyle(ChatFormatting.GOLD));
                } else {
                    double health = AstralMedalFormulas.maxHealthBonus(menu.score(ScoreType.CONSTITUTION));
                    lines.add(Component.translatable("tooltip.deeprealm_4th.warden_medal.preview",
                            AstralTooltips.number(health)).withStyle(ChatFormatting.GOLD));
                }
            }
            graphics.renderTooltip(font, lines, Optional.empty(), scaledX, scaledY);
        } else {
            renderTooltip(graphics, scaledX, scaledY);
            if (key != null) {
                graphics.renderTooltip(font,
                        Component.translatable("screen.deeprealm_4th." + key), scaledX, scaledY);
            }
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX / layoutScale, mouseY / layoutScale, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX / layoutScale, mouseY / layoutScale, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX / layoutScale, mouseY / layoutScale,
                button, dragX / layoutScale, dragY / layoutScale);
    }
}
