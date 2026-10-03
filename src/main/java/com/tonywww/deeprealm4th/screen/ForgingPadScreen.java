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
import net.minecraft.world.item.ItemStack;

/** Native-resolution texture background with a separate hammer button atlas. */
public final class ForgingPadScreen extends VersionedMenuScreen<ForgingPadMenu> {
    private static final int BUTTON_X = 131;
    private static final int BUTTON_Y = 47;
    private static final int RESULT_SLOT_X = 194;
    private static final int RESULT_SLOT_Y = 45;
    private static final ResourceLocation BACKGROUND = PlatformIds.id("textures/gui/forging_pad_background.png");
    private static final ResourceLocation HAMMER_BUTTON = PlatformIds.id("textures/gui/forging_hammer_button.png");

    public ForgingPadScreen(ForgingPadMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 286;
        imageHeight = 205;
        titleLabelX = 45;
        titleLabelY = 6;
        inventoryLabelX = 64;
        inventoryLabelY = 109;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        CombinationForgingRecipe recipe = menu.matchingRecipe();
        if (recipe == null) return;
        ItemStack output = menu.previewOutput();
        if (output.isEmpty()) return;
        Component name = recipe.nameKey().isEmpty() ? output.getHoverName()
                : Component.translatable(recipe.nameKey());
        String display = font.plainSubstrByWidth(name.getString(), 75);
        if (display.length() < name.getString().length())
            display = font.plainSubstrByWidth(name.getString(), 75 - font.width("...")) + "...";
        graphics.drawString(font, display, x + 165, y + 25, 0xFF272027, false);
        graphics.renderItem(output, x + RESULT_SLOT_X + 1, y + RESULT_SLOT_Y + 1);
        boolean hovered = mouseX >= x + BUTTON_X && mouseX < x + BUTTON_X + 25
                && mouseY >= y + BUTTON_Y && mouseY < y + BUTTON_Y + 25;
        graphics.blit(HAMMER_BUTTON, x + BUTTON_X, y + BUTTON_Y,
                hovered ? 25 : 0, 0, 25, 25, 50, 25);
        graphics.drawString(font, Component.translatable("screen.deeprealm_4th.forging.progress",
                        menu.clicksDone(), recipe.clicks()), x + 165, y + 67, 0xFF272027, false);
        graphics.drawString(font, Component.translatable("screen.deeprealm_4th.forging.levels",
                        recipe.levels()), x + 165, y + 78, 0xFF573B32, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderVersionedBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + RESULT_SLOT_X && mouseX < leftPos + RESULT_SLOT_X + 18
                && mouseY >= topPos + RESULT_SLOT_Y && mouseY < topPos + RESULT_SLOT_Y + 18) {
            ItemStack output = menu.previewOutput();
            if (!output.isEmpty()) graphics.renderTooltip(font, output, mouseX, mouseY);
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !menu.previewOutput().isEmpty()
                && mouseX >= leftPos + BUTTON_X && mouseX < leftPos + BUTTON_X + 25
                && mouseY >= topPos + BUTTON_Y && mouseY < topPos + BUTTON_Y + 25) {
            if (Minecraft.getInstance().gameMode != null)
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
