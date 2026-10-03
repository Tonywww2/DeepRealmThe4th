package com.tonywww.deeprealm4th.platform.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
//? if !forge {
/*import net.minecraft.core.component.DataComponents;
*///?}

/** The ItemStack custom-name setter changed between supported game versions. */
public final class AstralItemDisplayNamePlatform {
    private AstralItemDisplayNamePlatform() {}

    public static void set(ItemStack stack, Component name) {
        //? if forge {
        stack.setHoverName(name);
        //?} else {
        /*stack.set(DataComponents.CUSTOM_NAME, name);
        *///?}
    }
}
