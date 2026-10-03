package com.tonywww.deeprealm4th.screen;

import com.tonywww.deeprealm4th.astral.ScoreType;
import com.tonywww.deeprealm4th.astral.container.ContainerLayout;
import com.tonywww.deeprealm4th.astral.score.AstralMedalFormulas;
import com.tonywww.deeprealm4th.astral.AstralFillers;
import com.tonywww.deeprealm4th.astral.node.FillerResultSummary;
import com.tonywww.deeprealm4th.astral.score.AstralCurioAttributes;
import com.tonywww.deeprealm4th.astral.score.AstralNumber;
import com.tonywww.deeprealm4th.astral.score.AstralScoreEngine;
import com.tonywww.deeprealm4th.astral.tooltip.FillerPresentations;
import com.tonywww.deeprealm4th.astral.score.AstralScoreColors;
import com.tonywww.deeprealm4th.astral.score.WarriorMedalFormula;
import com.tonywww.deeprealm4th.astral.tooltip.AstralTooltips;
import com.tonywww.deeprealm4th.menu.AstralMenu;
import com.tonywww.deeprealm4th.platform.PlatformIds;
import com.tonywww.deeprealm4th.platform.screen.VersionedMenuScreen;

import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Texture-backed panels and cells support all configured container sizes. */
public final class AstralScreen extends VersionedMenuScreen<AstralMenu> {
    private static final int SCORE_PANEL_WIDTH = 124;
    private static final int SCORE_PANEL_GAP = 8;
    private static final ResourceLocation PANEL = PlatformIds.id("textures/gui/astral_panel.png");
    private static final ResourceLocation OPEN_SLOT = PlatformIds.id("textures/gui/astral_slot_open.png");
    private static final ResourceLocation DISABLED_SLOT = PlatformIds.id("textures/gui/astral_slot_disabled.png");
    private static final ResourceLocation PLAYER_SLOT = PlatformIds.id("textures/gui/astral_slot_player.png");
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
        GuiTextureTiles.panel(graphics, PANEL, leftPos, topPos, imageWidth, imageHeight);
        ContainerLayout layout = menu.layout();
        int gridX = leftPos + AstralMenu.gridX(layout);
        for (int y = 0; y < layout.height(); y++) {
            for (int x = 0; x < layout.width(); x++) {
                int left = gridX + x * 18;
                int top = topPos + 18 + y * 18;
                graphics.blit(layout.isOpen(x, y) ? OPEN_SLOT : DISABLED_SLOT,
                        left - 1, top - 1, 0, 0, 18, 18, 18, 18);
            }
        }
        int playerX = leftPos + Math.max(8, (imageWidth - 162) / 2);
        int playerY = topPos + 18 + Math.max(layout.height() * 18, 108) + 16;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) playerCell(graphics, playerX + col * 18, playerY + row * 18);
        }
        for (int col = 0; col < 9; col++) playerCell(graphics, playerX + col * 18, playerY + 58);
        int panelX = leftPos + imageWidth + SCORE_PANEL_GAP;
        int panelY = topPos + 8;
        GuiTextureTiles.panel(graphics, PANEL, panelX, panelY, SCORE_PANEL_WIDTH, 116);
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

    private static void playerCell(GuiGraphics graphics, int x, int y) {
        graphics.blit(PLAYER_SLOT, x - 1, y - 1, 0, 0, 18, 18, 18, 18);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderVersionedBackground(graphics, mouseX, mouseY, partialTick);
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
                        case 3 -> "invalid_cell";
                        case 4 -> "effect_group_cell";
                        case 5 -> "disabled_cell";
                        default -> null;
                    };
        }
        boolean dynamicNode = hoveredMenuIndex >= 0 && hoveredMenuIndex < menu.layout().size()
                && hoveredSlot != null && hoveredSlot.hasItem()
                && AstralFillers.find(hoveredSlot.getItem()) != null
                && AstralFillers.find(hoveredSlot.getItem()).nodeResolver() != null;
        boolean medalPreview = key == null && hoveredMenuIndex >= 0
                && hoveredMenuIndex < menu.layout().size() && hoveredSlot != null
                && (hoveredSlot.getItem().is(AstralItemCatalog.WARRIOR_MEDAL.get())
                    || hoveredSlot.getItem().is(AstralItemCatalog.WAYFARER_MEDAL.get())
                    || hoveredSlot.getItem().is(AstralItemCatalog.WARDEN_MEDAL.get()));
        if ((key != null || medalPreview || dynamicNode) && hoveredSlot != null && hoveredSlot.hasItem()) {
            // Keep the item's own tooltip visible and add the cell state as its last line.
            var lines = new ArrayList<>(getTooltipFromContainerItem(hoveredSlot.getItem()));
            if (key != null) {
                lines.add(Component.translatable("screen.deeprealm_4th." + key)
                        .withStyle(ChatFormatting.RED));
            }
            if (medalPreview) {
                if (hoveredSlot.getItem().is(AstralItemCatalog.WARRIOR_MEDAL.get())) {
                    double percent = WarriorMedalFormula.attackFraction(menu.score(ScoreType.STRENGTH)) * 100;
                    lines.add(Component.translatable("tooltip.deeprealm_4th.warrior_medal.preview",
                            AstralTooltips.number(percent)).withStyle(ChatFormatting.GOLD));
                } else if (hoveredSlot.getItem().is(AstralItemCatalog.WAYFARER_MEDAL.get())) {
                    double percent = AstralMedalFormulas.movementFraction(menu.score(ScoreType.AGILITY)) * 100;
                    lines.add(Component.translatable("tooltip.deeprealm_4th.wayfarer_medal.preview",
                            AstralTooltips.number(percent)).withStyle(ChatFormatting.GOLD));
                } else {
                    double health = AstralMedalFormulas.maxHealthBonus(menu.score(ScoreType.CONSTITUTION));
                    lines.add(Component.translatable("tooltip.deeprealm_4th.warden_medal.preview",
                            AstralTooltips.number(health)).withStyle(ChatFormatting.GOLD));
                }
            }
            if (dynamicNode) {
                FillerResultSummary summary = AstralCurioAttributes.nodeSnapshot(menu.owner())
                        .get(hoveredMenuIndex);
                if (summary == null && minecraft != null && minecraft.player != null) {
                    try {
                        var contents = new ArrayList<net.minecraft.world.item.ItemStack>();
                        for (int cell = 0; cell < menu.layout().size(); cell++)
                            contents.add(menu.slots.get(cell).getItem());
                        var result = AstralScoreEngine.calculate(minecraft.player, menu.owner(),
                                menu.layout(), contents).nodeResults().get(hoveredMenuIndex);
                        if (result != null) summary = FillerResultSummary.from(result);
                    } catch (RuntimeException ignored) {
                        // Server-authoritative summary may be unavailable for an unequipped preview.
                    }
                }
                if (summary != null) {
                    lines.add(Component.translatable("tooltip.deeprealm_4th.node.totals",
                            summary.baseTotal().format(AstralNumber.Format.COMPACT),
                            summary.finalTotal().format(AstralNumber.Format.COMPACT))
                            .withStyle(ChatFormatting.GRAY));
                    lines.addAll(FillerPresentations.tooltip(hoveredSlot.getItem(), summary));
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
