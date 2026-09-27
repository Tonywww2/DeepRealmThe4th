package com.tony.deeprealmtheforth.astral;

import com.google.common.collect.Multimap;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

//? if forge {
import java.util.UUID;
//?} else {
/*import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
*///?}

/** Extend this item to define a container with a custom layout or insertion rule. */
public class AstralContainerItem extends Item implements ICurioItem {
    public AstralContainerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Addons may return a different size and mask for their own container item. */
    public ContainerLayout layout(ItemStack stack) {
        return AstralConfig.baseLayout();
    }

    /** Addons can impose extra rules without replacing the persistence or menu layer. */
    public boolean mayInsert(AstralInventory inventory, int index, ItemStack filler) {
        return inventory.layout().isOpen(index)
                && !(filler.getItem() instanceof AstralContainerItem)
                && AstralFillers.find(filler) != null;
    }

    //? if forge {
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        AstralTooltips.appendContainer(stack, this, tooltip);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        AstralTooltips.appendContainer(stack, this, tooltip);
    }
    *///?}

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        // Using the item opens its own container instead of auto-equipping it.
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            int inventorySlot = hand == InteractionHand.MAIN_HAND
                    ? player.getInventory().selected : 40;
            AstralMenuOpener.open(serverPlayer, inventorySlot);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        AstralCurioAttributes.refresh(slotContext, stack);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!slotContext.entity().level().isClientSide && newStack.getItem() != stack.getItem()) {
            AstralCurioAttributes.clearSnapshot(stack);
        }
    }

    //? if forge {
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, UUID slotId, ItemStack stack) {
        return AstralCurioAttributes.modifiers(slotContext, slotId, stack);
    }
    //?} else {
    /*@Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, ResourceLocation slotId, ItemStack stack) {
        return AstralCurioAttributes.modifiers(slotContext, slotId, stack);
    }
    *///?}
}
