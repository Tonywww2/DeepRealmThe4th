package com.tonywww.deeprealm4th.astral.data;

import com.tonywww.deeprealm4th.platform.data.AstralItemDataStorage;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Copy-only data access for Java addons and KubeJS scripts. */
public final class AstralItemData {
    private AstralItemData() {}

    /** Returns an independent compound, or null when this key is missing. */
    public static CompoundTag read(ItemStack stack, String namespaceKey) {
        requireKey(namespaceKey);
        return AstralItemDataStorage.read(Objects.requireNonNull(stack, "stack"), namespaceKey);
    }

    public static ItemStack writeCopy(ItemStack stack, String namespaceKey, CompoundTag data) {
        requireKey(namespaceKey);
        if (stack.isEmpty()) throw new IllegalArgumentException("Cannot write data to an empty stack");
        return AstralItemDataStorage.writeCopy(stack, namespaceKey, Objects.requireNonNull(data, "data"));
    }

    public static ItemStack removeCopy(ItemStack stack, String namespaceKey) {
        requireKey(namespaceKey);
        return AstralItemDataStorage.removeCopy(Objects.requireNonNull(stack, "stack"), namespaceKey);
    }

    public static CompoundTag deepCopy(CompoundTag data) {
        return Objects.requireNonNull(data, "data").copy();
    }

    public static ValidationResult validate(CompoundTag data, DataSchema schema) {
        if (data == null) return ValidationResult.error("missing_data");
        return Objects.requireNonNull(schema, "schema").validate(data.copy());
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128)
            throw new IllegalArgumentException("Data key must contain 1..128 characters");
    }

    @FunctionalInterface
    public interface DataSchema {
        ValidationResult validate(CompoundTag data);
    }

    public record ValidationResult(boolean valid, String reason) {
        public static ValidationResult ok() { return new ValidationResult(true, ""); }
        public static ValidationResult error(String reason) { return new ValidationResult(false, reason); }
    }
}
