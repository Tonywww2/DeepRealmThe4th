package com.tonywww.deeprealm4th.astral.process;

import com.tonywww.deeprealm4th.platform.recipe.ProjectionRecipe;

import com.tonywww.deeprealm4th.item.ProjectionFrameItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Inventory-menu implementation of material-on-frame and frame-on-material. */
public final class ProjectionInventoryClick {
    private ProjectionInventoryClick() {}

    /** Returns true when vanilla right-click handling must be suppressed. */
    public static boolean handle(AbstractContainerMenu menu, int slotId, Player player) {
        if (slotId < 0 || slotId >= menu.slots.size()) return false;
        Slot slot = menu.getSlot(slotId);
        if (slot.container != player.getInventory()) return false;
        ItemStack carried = menu.getCarried();
        ItemStack target = slot.getItem();
        boolean frameOnTarget = target.getItem() instanceof ProjectionFrameItem;
        boolean frameCarried = carried.getItem() instanceof ProjectionFrameItem;
        if (frameOnTarget == frameCarried || carried.isEmpty() || target.isEmpty()) return false;
        if (player.level().isClientSide) return true;
        if (!slot.mayPickup(player)) return true;
        ProjectionRecipe.Direction direction = frameOnTarget
                ? ProjectionRecipe.Direction.MATERIAL_ON_FRAME
                : ProjectionRecipe.Direction.FRAME_ON_MATERIAL;
        ItemStack frame = frameOnTarget ? target : carried;
        ItemStack material = frameOnTarget ? carried : target;
        ProjectionCombining.Result result = ProjectionCombining.apply(player.level(), frame, material, direction);
        if (!result.accepted()) return true;
        if (frameOnTarget) {
            slot.set(result.frameOrOutput());
            ItemStack remainder = carried.copy();
            remainder.shrink(1);
            menu.setCarried(remainder);
        } else {
            ItemStack remainder = target.copy();
            remainder.shrink(1);
            slot.set(remainder);
            menu.setCarried(result.frameOrOutput());
        }
        slot.setChanged();
        menu.broadcastChanges();
        return true;
    }
}
