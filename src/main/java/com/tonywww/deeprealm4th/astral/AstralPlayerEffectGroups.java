package com.tonywww.deeprealm4th.astral;

import com.tonywww.deeprealm4th.astral.container.ContainerLayout;
import com.tonywww.deeprealm4th.item.AstralContainerItem;
import com.tonywww.deeprealm4th.platform.curios.AstralEquippedContainers;
import com.tonywww.deeprealm4th.platform.data.AstralStoredItems;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Stable player-wide winner selection for equipped containers. */
public final class AstralPlayerEffectGroups {
    private AstralPlayerEffectGroups() {}

    public static boolean precedes(Player player, ItemStack current, String group,
            String slotName, int slotIndex) {
        List<AstralEquippedContainers.Equipped> equippedContainers = AstralEquippedContainers.list(player);
        int currentOrder = -1;
        for (int i = 0; i < equippedContainers.size(); i++) {
            AstralEquippedContainers.Equipped equipped = equippedContainers.get(i);
            if (equipped.stack() == current || slotName != null && equipped.slot().equals(slotName)
                    && equipped.index() == slotIndex) { currentOrder = i; break; }
        }
        if (currentOrder < 0) return false;
        for (int order = 0; order < currentOrder; order++) {
            AstralEquippedContainers.Equipped equipped = equippedContainers.get(order);
            if (!(equipped.stack().getItem() instanceof AstralContainerItem item)) continue;
            ContainerLayout layout = item.layout(equipped.stack());
            List<ItemStack> contents = AstralStoredItems.read(equipped.stack(), layout.size());
            Set<String> present = new HashSet<>();
            for (int i = 0; i < contents.size(); i++) {
                if (layout.isOpen(i) && !contents.get(i).isEmpty())
                    present.add(BuiltInRegistries.ITEM.getKey(contents.get(i).getItem()).toString());
            }
            for (int i = 0; i < contents.size(); i++) {
                if (!layout.isOpen(i)) continue;
                ItemStack stack = contents.get(i);
                FillerDefinition definition = AstralFillers.find(stack);
                if (definition == null || definition.effectScope() != EffectScope.PLAYER
                        || !group.equals(definition.effectGroup())
                        || definition.suppressedBy().stream().anyMatch(present::contains)) continue;
                if (definition.nodeResolver() != null) {
                    try {
                        var node = definition.nodeResolver().resolve(new FillerContext(player,
                                equipped.stack(), stack, layout, contents, i));
                        if (node == null || !node.valid()) continue;
                    } catch (RuntimeException ignored) {
                        continue;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
