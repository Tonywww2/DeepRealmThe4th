package com.tony.deeprealmtheforth.astral;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

//? if forge {
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
//?} else {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
*///?}

/** Physical cell storage on the container ItemStack, including currently closed cells. */
public final class AstralStoredItems {
    private static final String KEY = "AstralContainer";

    private AstralStoredItems() {}

    public static NonNullList<ItemStack> read(ItemStack body, int cellCount) {
        if (cellCount < 1) throw new IllegalArgumentException("Container must have at least one cell");
        NonNullList<ItemStack> items = NonNullList.withSize(cellCount, ItemStack.EMPTY);
        //? if forge {
        CompoundTag root = body.getTagElement(KEY);
        if (root == null) return items;
        ListTag entries = root.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            int index = entry.getInt("Slot");
            if (index >= 0 && index < items.size()) {
                ItemStack stack = ItemStack.of(entry.getCompound("Stack"));
                if (!stack.isEmpty()) {
                    stack.setCount(1);
                    items.set(index, stack);
                }
            }
        }
        //?} else {
        /*body.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        *///?}
        return items;
    }

    public static void write(ItemStack body, List<ItemStack> items) {
        NonNullList<ItemStack> copy = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                copy.set(i, items.get(i).copyWithCount(1));
            }
        }
        //? if forge {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 1);
        root.putInt("Size", copy.size());
        ListTag entries = new ListTag();
        for (int i = 0; i < copy.size(); i++) {
            if (copy.get(i).isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            entry.put("Stack", copy.get(i).save(new CompoundTag()));
            entries.add(entry);
        }
        root.put("Items", entries);
        body.getOrCreateTag().put(KEY, root);
        //?} else {
        /*body.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(copy));
        *///?}
    }
}
