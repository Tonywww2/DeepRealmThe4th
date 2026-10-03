package com.tonywww.deeprealm4th.astral.process;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/** Named stack-data predicates referenced by datapack process recipes. */
public final class AstralDataPredicates {
    private static final Map<String, Predicate<ItemStack>> PREDICATES = new ConcurrentHashMap<>();
    private AstralDataPredicates() {}

    public static void register(String id, Predicate<ItemStack> predicate) {
        ProcessItemIds.requireValid(id);
        if (PREDICATES.putIfAbsent(id, predicate) != null)
            throw new IllegalArgumentException("Astral data predicate already registered: " + id);
    }
    public static boolean test(String id, ItemStack stack) {
        Predicate<ItemStack> predicate = PREDICATES.get(id);
        if (predicate == null) return false;
        try { return predicate.test(stack.copy()); }
        catch (RuntimeException ignored) { return false; }
    }
    public static boolean contains(String id) { return PREDICATES.containsKey(id); }
}
