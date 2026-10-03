package com.tonywww.deeprealm4th.astral;

import com.tonywww.deeprealm4th.astral.container.ContainerLayout;
import com.tonywww.deeprealm4th.astral.score.AstralCurioAttributes;
import com.tonywww.deeprealm4th.astral.score.AstralScoreEngine;
import com.tonywww.deeprealm4th.item.AstralContainerItem;
import com.tonywww.deeprealm4th.platform.curios.AstralEquippedContainers;
import com.tonywww.deeprealm4th.platform.data.AstralStoredItems;
import java.util.function.UnaryOperator;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/** Server-side cell editing and explicit invalidation for script and Java processors. */
public final class AstralRuntime {
    private static final ThreadLocal<Boolean> CALCULATING = ThreadLocal.withInitial(() -> false);
    private AstralRuntime() {}

    public static boolean calculating() { return CALCULATING.get(); }
    public static void beginCalculation() {
        if (calculating()) throw new IllegalStateException("Astral calculation reentered");
        CALCULATING.set(true);
    }
    public static void endCalculation() { CALCULATING.set(false); }

    public static void invalidate(Player player) {
        requireServer(player);
        for (AstralEquippedContainers.Equipped equipped : AstralEquippedContainers.list(player))
            AstralCurioAttributes.clearSnapshot(equipped.stack());
    }
    public static void invalidate(Player player, ItemStack container) {
        requireServer(player);
        AstralCurioAttributes.clearSnapshot(container);
    }

    public static ItemStack updateCell(Player player, ItemStack container, int index,
            UnaryOperator<ItemStack> transform) {
        requireServer(player);
        if (calculating()) throw new IllegalStateException("Cannot edit a cell during score calculation");
        if (!(container.getItem() instanceof AstralContainerItem item))
            throw new IllegalArgumentException("Expected an astral container");
        ContainerLayout layout = item.layout(container);
        if (!layout.isOpen(index)) throw new IllegalArgumentException("Cell is closed or out of bounds: " + index);
        NonNullList<ItemStack> stored = AstralStoredItems.read(container, layout.size());
        ItemStack changed = transform.apply(stored.get(index).copy());
        if (changed == null || changed.getCount() > 1 || !changed.isEmpty()
                && AstralFillers.find(changed) == null
                && !AstralFillers.isDisabled(BuiltInRegistries.ITEM.getKey(changed.getItem()).toString()))
            throw new IllegalArgumentException("Cell transform returned an invalid filler stack");
        stored.set(index, changed.isEmpty() ? ItemStack.EMPTY : changed.copy());
        AstralStoredItems.write(container, stored);
        invalidate(player, container);
        return changed.copy();
    }

    public static AstralScoreEngine.Result recalculate(Player player, ItemStack container) {
        requireServer(player);
        if (!(container.getItem() instanceof AstralContainerItem item))
            throw new IllegalArgumentException("Expected an astral container");
        ContainerLayout layout = item.layout(container);
        return AstralScoreEngine.calculate(player, container, layout,
                AstralStoredItems.read(container, layout.size()));
    }

    private static void requireServer(Player player) {
        if (player == null || player.level().isClientSide)
            throw new IllegalStateException("Astral runtime edits require a server-side player");
    }
}
