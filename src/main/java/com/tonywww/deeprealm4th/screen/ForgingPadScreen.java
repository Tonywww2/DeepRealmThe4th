package com.tonywww.deeprealm4th.screen;

import com.tonywww.deeprealm4th.menu.ForgingPadMenu;
import com.tonywww.deeprealm4th.platform.PlatformIds;
import com.tonywww.deeprealm4th.platform.recipe.CombinationForgingRecipe;
import com.tonywww.deeprealm4th.platform.screen.VersionedMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Native-resolution texture background with a separate hammer button atlas. */
public final class ForgingPadScreen extends VersionedMenuScreen<ForgingPadMenu> {
    private static final int BUTTON_X = 173;
    private static final int BUTTON_Y = 49;
    private static final ResourceLocation BACKGROUND = PlatformIds.id("textures/gui/forging_pad_background.png");
    private static final ResourceLocation HAMMER_BUTTON = PlatformIds.id("textures/gui/forging_hammer_button.png");

    public ForgingPadScreen(ForgingPadMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 236;
        imageHeight = 205;
        titleLabelX = 9;
        titleLabelY = 6;
        inventoryLabelX = 39;
        inventoryLabelY = 109;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        CombinationForgingRecipe recipe = menu.matchingRecipe();
        if (recipe == null) return;
        Component name = recipe.nameKey().isEmpty() ? recipe.output().getHoverName()
                : Component.translatable(recipe.nameKey());
        String display = font.plainSubstrByWidth(name.getString(), 112);
        if (display.length() < name.getString().length()) display += "...";
        graphics.drawString(font, display, x + 113, y + 26, 0xFF272027, false);
        graphics.renderItem(recipe.output(), x + 125, y + 52);
        boolean hovered = mouseX >= x + BUTTON_X && mouseX < x + BUTTON_X + 25
                && mouseY >= y + BUTTON_Y && mouseY < y + BUTTON_Y + 25;
        graphics.blit(HAMMER_BUTTON, x + BUTTON_X, y + BUTTON_Y,
                hovered ? 25 : 0, 0, 25, 25, 50, 25);
        graphics.drawString(font, Component.translatable("screen.deeprealm_4th.forging.progress",
                        menu.clicksDone(), recipe.clicks()), x + 113, y + 83, 0xFF272027, false);
        graphics.drawString(font, Component.translatable("screen.deeprealm_4th.forging.levels",
                        recipe.levels()), x + 113, y + 94, 0xFF573B32, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderVersionedBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.matchingRecipe() != null
                && mouseX >= leftPos + BUTTON_X && mouseX < leftPos + BUTTON_X + 25
                && mouseY >= topPos + BUTTON_Y && mouseY < topPos + BUTTON_Y + 25) {
            if (Minecraft.getInstance().gameMode != null)
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
