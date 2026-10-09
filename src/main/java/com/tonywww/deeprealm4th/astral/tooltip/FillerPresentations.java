package com.tonywww.deeprealm4th.astral.tooltip;

import com.tonywww.deeprealm4th.astral.node.FillerResultSummary;
import com.tonywww.deeprealm4th.platform.item.AstralItemDisplayNamePlatform;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Stack-data-aware name and tooltip providers for a shared physical filler item. */
public final class FillerPresentations {
    private static final Map<String, Provider> PROVIDERS = new ConcurrentHashMap<>();
    private static final Map<String, Predicate<ItemStack>> FOIL_PROVIDERS = new ConcurrentHashMap<>();
    private FillerPresentations() {}

    public interface Provider {
        /** Return null to retain the item's normal translated name. */
        Component name(ItemStack stack);
        default List<Component> summary(ItemStack stack) { return List.of(); }
        default List<Component> tooltip(ItemStack stack, FillerResultSummary result) { return List.of(); }
    }

    public static void register(String itemId, Provider provider) {
        if (itemId == null || !itemId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"))
            throw new IllegalArgumentException("Expected a namespaced item ID");
        if (PROVIDERS.putIfAbsent(itemId, Objects.requireNonNull(provider)) != null)
            throw new IllegalArgumentException("Presentation already registered for " + itemId);
    }

    public static void registerName(String itemId, Function<ItemStack, Component> name) {
        Objects.requireNonNull(name, "name");
        register(itemId, new Provider() {
            @Override public Component name(ItemStack stack) { return name.apply(stack); }
        });
    }

    /** Adds an enchantment glint based on stack data, without adding an enchantment. */
    public static void registerFoil(String itemId, Predicate<ItemStack> foil) {
        if (itemId == null || !itemId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"))
            throw new IllegalArgumentException("Expected a namespaced item ID");
        if (FOIL_PROVIDERS.putIfAbsent(itemId, Objects.requireNonNull(foil)) != null)
            throw new IllegalArgumentException("Foil provider already registered for " + itemId);
    }

    /** Additive: false retains the item's native glint behavior. */
    public static boolean foil(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Predicate<ItemStack> foil = FOIL_PROVIDERS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (foil == null) return false;
        try { return foil.test(stack.copy()); }
        catch (RuntimeException ignored) { return false; }
    }

    public static Provider find(ItemStack stack) {
        return stack.isEmpty() ? null : PROVIDERS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static Component name(ItemStack stack) {
        Provider provider = find(stack);
        if (provider == null) return null;
        try { return provider.name(stack.copy()); }
        catch (RuntimeException ignored) { return null; }
    }

    /** For plain KubeJS items, writes the current data-derived name onto an output copy. */
    public static ItemStack applyNameCopy(ItemStack stack) {
        ItemStack copy = stack.copy();
        Component dynamic = name(copy);
        if (dynamic != null) AstralItemDisplayNamePlatform.set(copy, dynamic);
        return copy;
    }

    public static List<Component> summary(ItemStack stack) {
        Provider provider = find(stack);
        if (provider == null) return List.of();
        try { return List.copyOf(provider.summary(stack.copy())); }
        catch (RuntimeException ignored) { return List.of(); }
    }

    public static List<Component> tooltip(ItemStack stack, FillerResultSummary result) {
        Provider provider = find(stack);
        if (provider == null) return List.of();
        try { return List.copyOf(provider.tooltip(stack.copy(), result)); }
        catch (RuntimeException ignored) { return List.of(); }
    }
}
