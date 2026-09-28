package com.tonywww.deeprealm4th.platform.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Bridges the tooltip override signature while items supply their own content. */
public abstract class VersionedTooltipItem extends Item {
    protected VersionedTooltipItem(Properties properties) {
        super(properties);
    }

    protected abstract void appendAstralTooltip(ItemStack stack, List<Component> tooltip);

    //? if forge {
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        appendAstralTooltip(stack, tooltip);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        appendAstralTooltip(stack, tooltip);
    }
    *///?}
}
