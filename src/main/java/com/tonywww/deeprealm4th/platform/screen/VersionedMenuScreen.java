package com.tonywww.deeprealm4th.platform.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Adapts the changed vanilla background signature for both custom menus. */
public abstract class VersionedMenuScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    protected VersionedMenuScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected final void renderVersionedBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if forge {
        renderBackground(graphics);
        //?} else {
        /*renderBackground(graphics, mouseX, mouseY, partialTick);
        *///?}
    }
}
