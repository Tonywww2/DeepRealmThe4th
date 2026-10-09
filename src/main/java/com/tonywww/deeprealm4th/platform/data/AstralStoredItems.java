package com.tonywww.deeprealm4th.platform.data;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

//? if forge {
import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
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
        //? if forge {
        return projected(body, cellCount);
        //?} else {
        /*NonNullList<ItemStack> items = NonNullList.withSize(cellCount, ItemStack.EMPTY);
        body.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
        *///?}
    }

    //? if forge {
    private static final class StoredItems extends NonNullList<ItemStack> {
        private final List<ItemStack> overflow;

        private StoredItems(NonNullList<ItemStack> items, List<ItemStack> overflow) {
            super(new ArrayList<>(items), ItemStack.EMPTY);
            this.overflow = overflow;
        }
    }

    /** A read-only projection; client previews and score evaluation never rewrite the stack. */
    private static StoredItems projected(ItemStack body, int cellCount) {
        NonNullList<ItemStack> items = NonNullList.withSize(cellCount, ItemStack.EMPTY);
        CompoundTag root = body.getTagElement(KEY);
        if (root == null) return new StoredItems(items, List.of());
        String id = BuiltInRegistries.ITEM.getKey(body.getItem()).toString();
        int oldSize = root.getInt("Size");
        int oldWidth = 0, offsetX = 0, offsetY = 0, width = 0, height = 0;
        if (id.equals("deeprealm_4th:base_container") && oldSize == 108 && cellCount == 20) {
            oldWidth = 12; offsetX = 4; offsetY = 2; width = 4; height = 5;
        } else if (id.equals("kubejs:astral_rune") && oldSize == 64 && cellCount == 36) {
            oldWidth = 8; offsetX = 1; offsetY = 1; width = 6; height = 6;
        }
        List<ItemStack> pending = new ArrayList<>();
        ListTag entries = root.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            ItemStack stack = ItemStack.of(entry.getCompound("Stack"));
            if (stack.isEmpty()) continue;
            stack.setCount(1);
            int index = entry.getInt("Slot");
            if (oldWidth != 0) {
                int x = index % oldWidth - offsetX, y = index / oldWidth - offsetY;
                index = x >= 0 && x < width && y >= 0 && y < height ? y * width + x : -1;
            }
            if (index >= 0 && index < cellCount && items.get(index).isEmpty()) items.set(index, stack);
            else pending.add(stack);
        }
        ListTag savedOverflow = root.getList("Overflow", Tag.TAG_COMPOUND);
        for (int i = 0; i < savedOverflow.size(); i++) {
            ItemStack stack = ItemStack.of(savedOverflow.getCompound(i));
            if (!stack.isEmpty()) pending.add(stack.copyWithCount(1));
        }
        int recovered = 0;
        for (int index = 0; index < cellCount && recovered < pending.size(); index++) {
            if (items.get(index).isEmpty()) items.set(index, pending.get(recovered++));
        }
        return new StoredItems(items, List.copyOf(pending.subList(recovered, pending.size())));
    }
    //?}

    public static void write(ItemStack body, List<ItemStack> items) {
        NonNullList<ItemStack> copy = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) copy.set(i, items.get(i).copyWithCount(1));
        }
        //? if forge {
        // Retain the overflow belonging to this particular read, even across repeated menu edits.
        List<ItemStack> overflow = items instanceof StoredItems stored
                ? stored.overflow : projected(body, copy.size()).overflow;
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
        if (!overflow.isEmpty()) {
            ListTag savedOverflow = new ListTag();
            for (ItemStack stack : overflow) savedOverflow.add(stack.save(new CompoundTag()));
            root.put("Overflow", savedOverflow);
        }
        body.getOrCreateTag().put(KEY, root);
        //?} else {
        /*body.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(copy));
        *///?}
    }
}
