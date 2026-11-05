package com.tonywww.deeprealm4th.platform.recipe;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Player context for the mod's special crafting-grid recipes, which vanilla evaluates without one. */
public final class CraftingGridContext {
    private static final ThreadLocal<Player> GRID_PLAYER = new ThreadLocal<>();

    private CraftingGridContext() {}

    /** Called by the crafting-menu mixin around vanilla's server-side result update. */
    public static void begin(Player player) { GRID_PLAYER.set(player); }
    public static void end() { GRID_PLAYER.remove(); }

    /** The loader's crafting player while a result is taken, else the player of the grid update. */
    public static Player player() {
        //? if forge {
        Player crafting = net.minecraftforge.common.ForgeHooks.getCraftingPlayer();
        //?} else {
        /*Player crafting = net.neoforged.neoforge.common.CommonHooks.getCraftingPlayer();
        *///?}
        return crafting != null ? crafting : GRID_PLAYER.get();
    }

    /**
     * Puts stacks into the given remainder slots in order; the rest go to the player's inventory and
     * drop when it is full. A recipe must not match when stacks would overflow without a player.
     */
    public static void returnStacks(NonNullList<ItemStack> remainders, List<Integer> slots,
            List<ItemStack> stacks, Player player) {
        int next = 0;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            ItemStack copy = stack.copy();
            if (next < slots.size()) {
                remainders.set(slots.get(next++), copy);
            } else if (player != null && !player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
    }
}
