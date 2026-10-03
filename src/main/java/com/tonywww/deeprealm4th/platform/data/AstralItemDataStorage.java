package com.tonywww.deeprealm4th.platform.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

//? if !forge {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
*///?}

/** The only version-specific part of astral payload storage. */
public final class AstralItemDataStorage {
    private AstralItemDataStorage() {}

    public static CompoundTag read(ItemStack stack, String key) {
        if (stack.isEmpty()) return null;
        //? if forge {
        CompoundTag data = stack.getTagElement(key);
        return data == null ? null : data.copy();
        //?} else {
        /*CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag root = data.copyTag();
        return root.contains(key, 10) ? root.getCompound(key).copy() : null;
        *///?}
    }

    public static ItemStack writeCopy(ItemStack stack, String key, CompoundTag value) {
        ItemStack copy = stack.copy();
        //? if forge {
        copy.getOrCreateTag().put(key, value.copy());
        //?} else {
        /*CustomData.update(DataComponents.CUSTOM_DATA, copy, root -> root.put(key, value.copy()));
        *///?}
        return copy;
    }

    public static ItemStack removeCopy(ItemStack stack, String key) {
        ItemStack copy = stack.copy();
        //? if forge {
        if (copy.hasTag()) copy.getTag().remove(key);
        //?} else {
        /*CustomData data = copy.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag root = data.copyTag();
            root.remove(key);
            copy.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        *///?}
        return copy;
    }
}
