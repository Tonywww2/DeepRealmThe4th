package com.tonywww.deeprealm4th.platform.item;

import com.google.common.collect.Multimap;
import com.tonywww.deeprealm4th.platform.curios.CurioAttributePlatform;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

//? if forge {
import java.util.UUID;
//?} else {
/*import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
*///?}

/** Curios modifier and tooltip signatures; container behavior stays in the item package. */
public abstract class VersionedCurioItem extends Item implements ICurioItem {
    protected VersionedCurioItem(Properties properties) {
        super(properties);
    }

    protected abstract void appendAstralTooltip(ItemStack stack, List<Component> tooltip);

    //? if forge {
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        appendAstralTooltip(stack, tooltip);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, UUID slotId, ItemStack stack) {
        return CurioAttributePlatform.modifiers(slotContext, slotId, stack);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        appendAstralTooltip(stack, tooltip);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, ResourceLocation slotId, ItemStack stack) {
        return CurioAttributePlatform.modifiers(slotContext, slotId, stack);
    }
    *///?}
}
