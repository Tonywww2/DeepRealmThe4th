package com.tonywww.deeprealm4th.platform.data;

import com.tonywww.deeprealm4th.platform.recipe.ProjectionRecipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

//? if !forge {
/*import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.CustomData;
*///?}

/** Stored material stacks remain intact for lossless shapeless disassembly. */
public final class ProjectionFrameData {
    private static final String KEY = "DeepRealmProjection";
    public record Absorbed(ItemStack stack, ProjectionRecipe.Direction direction) {}

    private ProjectionFrameData() {}

    public static List<Absorbed> read(ItemStack frame) {
        if (frame.isEmpty()) return List.of();
        List<Absorbed> result = new ArrayList<>();
        //? if forge {
        CompoundTag root = frame.getTagElement(KEY);
        if (root == null) return List.of();
        ListTag list = root.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(9, list.size()); i++) {
            CompoundTag entry = list.getCompound(i);
            ItemStack material = ItemStack.of(entry.getCompound("Stack"));
            int direction = entry.getInt("Direction");
            if (!material.isEmpty() && direction >= 0 && direction < ProjectionRecipe.Direction.values().length) {
                result.add(new Absorbed(material.copyWithCount(1), ProjectionRecipe.Direction.values()[direction]));
            }
        }
        //?} else {
        /*CompoundTag root = frame.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(KEY);
        int[] directions = root.getIntArray("Directions");
        NonNullList<ItemStack> materials = NonNullList.withSize(9, ItemStack.EMPTY);
        frame.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(materials);
        for (int i = 0; i < Math.min(9, directions.length); i++) {
            int direction = directions[i];
            if (!materials.get(i).isEmpty() && direction >= 0 && direction < ProjectionRecipe.Direction.values().length) {
                result.add(new Absorbed(materials.get(i).copyWithCount(1), ProjectionRecipe.Direction.values()[direction]));
            }
        }
        *///?}
        return List.copyOf(result);
    }

    public static ItemStack write(ItemStack frame, List<Absorbed> absorbed) {
        if (absorbed.size() > 9) throw new IllegalArgumentException("Projection frame holds at most nine materials");
        ItemStack copy = frame.copyWithCount(1);
        //? if forge {
        CompoundTag root = new CompoundTag();
        ListTag items = new ListTag();
        for (Absorbed step : absorbed) {
            CompoundTag entry = new CompoundTag();
            entry.put("Stack", step.stack().copyWithCount(1).save(new CompoundTag()));
            entry.putInt("Direction", step.direction().ordinal());
            items.add(entry);
        }
        root.put("Items", items);
        copy.getOrCreateTag().put(KEY, root);
        //?} else {
        /*NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        int[] directions = new int[absorbed.size()];
        for (int i = 0; i < absorbed.size(); i++) {
            items.set(i, absorbed.get(i).stack().copyWithCount(1));
            directions[i] = absorbed.get(i).direction().ordinal();
        }
        copy.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        CustomData.update(DataComponents.CUSTOM_DATA, copy, tag -> {
            CompoundTag root = new CompoundTag();
            root.putIntArray("Directions", directions);
            tag.put(KEY, root);
        });
        *///?}
        return copy;
    }

    public static List<ProjectionRecipe.Step> steps(List<Absorbed> absorbed) {
        return absorbed.stream().map(step -> new ProjectionRecipe.Step(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(step.stack().getItem()).toString(),
                step.direction())).toList();
    }
}
