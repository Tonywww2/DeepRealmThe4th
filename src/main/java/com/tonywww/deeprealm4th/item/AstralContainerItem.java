package com.tonywww.deeprealm4th.item;

import com.tonywww.deeprealm4th.astral.AstralFillers;
import com.tonywww.deeprealm4th.platform.config.AstralConfig;
import com.tonywww.deeprealm4th.astral.score.AstralCurioAttributes;
import com.tonywww.deeprealm4th.platform.menu.AstralMenuOpener;

import com.tonywww.deeprealm4th.astral.container.AstralInventory;
import com.tonywww.deeprealm4th.astral.container.ContainerLayout;
import com.tonywww.deeprealm4th.astral.tooltip.AstralTooltips;
import com.tonywww.deeprealm4th.platform.item.VersionedCurioItem;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;

/** Extend this item to define a container with a custom layout or insertion rule. */
public class AstralContainerItem extends VersionedCurioItem {
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

    @Override
    protected void appendAstralTooltip(ItemStack stack, List<Component> tooltip) {
        AstralTooltips.appendContainer(stack, this, tooltip);
    }

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

}
