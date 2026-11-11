package com.tonywww.deeprealm4th.astral.process;

import com.tonywww.deeprealm4th.platform.recipe.ProjectionRecipe;

import com.tonywww.deeprealm4th.item.ProjectionFrameItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Right-click material-on-frame and frame-on-material in any container menu. */
public final class ProjectionInventoryClick {
    private ProjectionInventoryClick() {}

    /** Returns true when vanilla right-click handling must be suppressed. */
    public static boolean handle(AbstractContainerMenu menu, int slotId, Player player) {
        if (slotId < 0 || slotId >= menu.slots.size()) return false;
        // Opened menus use ids 1..100 and the player's own inventory menu uses 0. Any other id-0 menu,
        // such as the creative item picker, exists only on the client and its clicks never reach the server.
        if (menu.containerId == 0 && !(menu instanceof InventoryMenu)) return false;
        Slot slot = menu.getSlot(slotId);
        ItemStack carried = menu.getCarried();
        ItemStack target = slot.getItem();
        boolean frameOnTarget = target.getItem() instanceof ProjectionFrameItem;
        boolean frameCarried = carried.getItem() instanceof ProjectionFrameItem;
        if (frameOnTarget == frameCarried || carried.isEmpty() || target.isEmpty()) return false;
        // Result and locked slots keep vanilla handling: editing them directly would skip their take logic.
        if (!slot.allowModification(player)) return false;
        if (player.level().isClientSide) return true;
        ProjectionRecipe.Direction direction = frameOnTarget
                ? ProjectionRecipe.Direction.MATERIAL_ON_FRAME
                : ProjectionRecipe.Direction.FRAME_ON_MATERIAL;
        ItemStack frame = frameOnTarget ? target : carried;
        ItemStack material = frameOnTarget ? carried : target;
        ProjectionCombining.Result result = ProjectionCombining.apply(player, frame, material, direction);
        if (!result.accepted()) return true;
        if (frameOnTarget) {
            // The target slot keeps the updated frame or the finished output, so it must accept it.
            ItemStack placed = result.frameOrOutput();
            if (!slot.mayPlace(placed) || placed.getCount() > slot.getMaxStackSize(placed)) return true;
            slot.set(placed);
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
        for (ItemStack returned : result.returned()) {
            if (returned.isEmpty()) continue;
            player.getInventory().add(returned);
            if (!returned.isEmpty()) player.drop(returned, false);
        }
        menu.broadcastChanges();
        return true;
    }
}
