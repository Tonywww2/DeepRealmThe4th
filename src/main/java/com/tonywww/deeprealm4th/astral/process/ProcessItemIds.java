package com.tonywww.deeprealm4th.astral.process;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Item ID validation and lookup shared by astral crafting processes. */
public final class ProcessItemIds {
    private ProcessItemIds() {}

    public static String requireValid(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
            throw new IllegalArgumentException("Expected namespaced item id: " + id);
        }
        return id;
    }

    public static ItemStack output(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null || !BuiltInRegistries.ITEM.containsKey(location)
                ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(location));
    }
}
