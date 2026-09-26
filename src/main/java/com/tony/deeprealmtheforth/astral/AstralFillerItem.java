package com.tony.deeprealmtheforth.astral;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A filler with translated description lines and shared activation/conflict hints. */
public class AstralFillerItem extends Item {
    private final List<String> descriptionKeys;

    /**
     * Each key is a full translation key, in display order. Subclasses may override
     * {@link #appendAstralTooltip} to add live values or other context.
     */
    public AstralFillerItem(Properties properties, String... descriptionKeys) {
        super(properties);
        this.descriptionKeys = List.of(descriptionKeys);
    }

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

    protected void appendAstralTooltip(ItemStack stack, List<Component> tooltip) {
        AstralTooltips.appendFiller(stack, tooltip, descriptionKeys);
    }
}
