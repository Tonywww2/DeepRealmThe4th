package com.tonywww.deeprealm4th.platform.curios;

import com.tonywww.deeprealm4th.item.AstralContainerItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Sorted access to real equipped containers, shared across Curios versions. */
public final class AstralEquippedContainers {
    public record Equipped(String slot, int index, ItemStack stack) {}
    private AstralEquippedContainers() {}

    public static List<Equipped> list(Player player) {
        //? if forge {
        var inventory = CuriosApi.getCuriosInventory(player).resolve().orElse(null);
        //?} else {
        /*var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        *///?}
        if (inventory == null) return List.of();
        List<Equipped> found = new ArrayList<>();
        inventory.getCurios().forEach((slot, handler) -> {
            for (int i = 0; i < handler.getStacks().getSlots(); i++) {
                ItemStack stack = handler.getStacks().getStackInSlot(i);
                if (stack.getItem() instanceof AstralContainerItem) found.add(new Equipped(slot, i, stack));
            }
        });
        found.sort(Comparator.comparing(Equipped::slot).thenComparingInt(Equipped::index));
        return List.copyOf(found);
    }
}
