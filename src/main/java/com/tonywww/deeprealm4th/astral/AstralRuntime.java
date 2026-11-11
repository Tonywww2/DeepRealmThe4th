package com.tonywww.deeprealm4th.astral;

import com.tonywww.deeprealm4th.astral.container.ContainerLayout;
import com.tonywww.deeprealm4th.astral.score.AstralCurioAttributes;
import com.tonywww.deeprealm4th.astral.score.AstralScoreEngine;
import com.tonywww.deeprealm4th.item.AstralContainerItem;
import com.tonywww.deeprealm4th.platform.data.AstralStoredItems;
import java.util.function.UnaryOperator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/** Server-side cell editing and explicit invalidation for script and Java processors. */
public final class AstralRuntime {
    private static final ThreadLocal<Round> ROUND = new ThreadLocal<>();

    private static final class Round {
        final Object token = new Object();
        final IdentityHashMap<ItemStack, Integer> namespaces = new IdentityHashMap<>();
        final Map<String, Object> memo = new HashMap<>();
        final IdentityHashMap<ItemStack, ItemStack> containers = new IdentityHashMap<>();
        String stage = "resolve";
        boolean readOnly;
    }

    public static Object roundToken() { return ROUND.get() == null ? null : ROUND.get().token; }
    public static void requireRound(Object token) {
        if (token == null || (ROUND.get() == null || token != ROUND.get().token)) throw new IllegalStateException("Calculation round has ended");
    }
    public static void stage(String stage) { if (ROUND.get() != null) ROUND.get().stage = stage; }
    public static void readOnlyContract(boolean enabled) { if (ROUND.get() != null) ROUND.get().readOnly = enabled; }
    public static boolean readOnlyContract() { return ROUND.get() != null && ROUND.get().readOnly; }
    public static String currentStage() { return ROUND.get() == null ? "outside" : ROUND.get().stage; }
    public static String memoNamespace(ItemStack container) {
        Round round = ROUND.get();
        return round == null ? "outside" : String.valueOf(round.namespaces.computeIfAbsent(container, key -> round.namespaces.size()));
    }
    public static void freezeContainer(ItemStack container) { ROUND.get().containers.put(container, container.copy()); }
    static ItemStack contextContainer(ItemStack container) {
        Round round = ROUND.get();
        return round != null && round.readOnly
                ? round.containers.computeIfAbsent(container, ItemStack::copy) : container.copy();
    }
    public static Object memoGet(String stage, String key) {
        Round round = ROUND.get();
        return round == null ? null : round.memo.get(stage + "\0" + key);
    }
    public static void memoPut(String stage, String key, Object value) {
        Round round = ROUND.get();
        if (round == null) throw new IllegalStateException("Calculation round has ended");
        round.memo.put(stage + "\0" + key, Objects.requireNonNull(value));
    }
    private AstralRuntime() {}

    public static boolean calculating() { return ROUND.get() != null; }
    public static void beginCalculation() {
        if (calculating()) throw new IllegalStateException("Astral calculation reentered");
        ROUND.set(new Round());
    }
    public static void endCalculation() {
        Round round = ROUND.get();
        if (round != null) { round.memo.clear(); round.containers.clear(); round.namespaces.clear(); }
        ROUND.remove();
    }

    public static void invalidate(Player player) {
        requireServer(player);
        AstralCurioAttributes.invalidate(player);
    }
    public static void invalidate(Player player, ItemStack container) {
        requireServer(player);
        AstralCurioAttributes.contentChanged(container);
        AstralCurioAttributes.invalidate(player);
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
