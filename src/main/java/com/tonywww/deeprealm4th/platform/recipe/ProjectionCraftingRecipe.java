package com.tonywww.deeprealm4th.platform.recipe;

import com.tonywww.deeprealm4th.astral.process.ProjectionCombining;
import com.tonywww.deeprealm4th.item.ProjectionFrameItem;
import com.tonywww.deeprealm4th.platform.registry.AstralProcessRegistration;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

//? if forge {
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.CraftingInput;
*///?}

/**
 * One projection step in a crafting grid: exactly one frame and one other material. In grid order
 * (left to right, top to bottom) a material before the frame equals frame-on-material, and a material
 * after it equals material-on-frame. The result is computed by the same code as inventory stacking.
 */
public final class ProjectionCraftingRecipe extends CustomRecipe {
    /** Level from the preceding {@code matches} call, for grids evaluated without a player. */
    private static final ThreadLocal<WeakReference<Level>> MATCHED_LEVEL = new ThreadLocal<>();

    private record Step(int frameSlot, int materialSlot, ItemStack frame, ItemStack material,
            ProjectionRecipe.Direction direction) {}

    //? if forge {
    public ProjectionCraftingRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }
    //?} else {
    /*public ProjectionCraftingRecipe(CraftingBookCategory category) { super(category); }
    *///?}

    @Override public boolean matches(
            //? if forge {
            CraftingContainer input,
            //?} else {
            /*CraftingInput input,
            *///?}
            Level level) {
        MATCHED_LEVEL.set(new WeakReference<>(level));
        return combine(input, level) != null;
    }

    @Override public ItemStack assemble(
            //? if forge {
            CraftingContainer input, RegistryAccess registries
            //?} else {
            /*CraftingInput input, HolderLookup.Provider registries
            *///?}
            ) {
        ProjectionCombining.Result result = combine(input, null);
        return result == null ? ItemStack.EMPTY : result.frameOrOutput();
    }

    /** Recipe-returned stacks go back like stacking returns them; ordinary crafting remainders do not. */
    @Override public NonNullList<ItemStack> getRemainingItems(
            //? if forge {
            CraftingContainer input
            //?} else {
            /*CraftingInput input
            *///?}
            ) {
        int size = size(input);
        NonNullList<ItemStack> remainders = NonNullList.withSize(size, ItemStack.EMPTY);
        Step step = step(input);
        ProjectionCombining.Result result = combine(input, null);
        if (step == null || result == null) return remainders;
        // Slots that are empty once this craft removes one frame and one material.
        List<Integer> free = new ArrayList<>();
        free.add(step.frameSlot());
        for (int i = 0; i < size; i++) {
            if (i == step.frameSlot()) continue;
            if (input.getItem(i).isEmpty() || i == step.materialSlot() && input.getItem(i).getCount() == 1)
                free.add(i);
        }
        CraftingGridContext.returnStacks(remainders, free, result.returned(), CraftingGridContext.player());
        return remainders;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() {
        return AstralProcessRegistration.PROJECTION_CRAFTING_SERIALIZER.get();
    }

    /** Null when the grid is not exactly one acceptable projection step. */
    private static ProjectionCombining.Result combine(
            //? if forge {
            CraftingContainer input,
            //?} else {
            /*CraftingInput input,
            *///?}
            Level matchedLevel) {
        Step step = step(input);
        if (step == null) return null;
        Player player = CraftingGridContext.player();
        Level level = matchedLevel != null ? matchedLevel : player != null ? player.level() : lastMatchedLevel();
        if (level == null) return null;
        ProjectionCombining.Result result = ProjectionCombining.apply(player, level,
                step.frame(), step.material(), step.direction());
        // Without a player the returned stacks could not be handed back, so the step is not offered.
        if (!result.accepted() || player == null && !result.returned().isEmpty()) return null;
        return result;
    }

    private static Step step(
            //? if forge {
            CraftingContainer input
            //?} else {
            /*CraftingInput input
            *///?}
            ) {
        int frameSlot = -1, materialSlot = -1;
        for (int i = 0; i < size(input); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ProjectionFrameItem) {
                if (frameSlot >= 0) return null;
                frameSlot = i;
            } else {
                if (materialSlot >= 0) return null;
                materialSlot = i;
            }
        }
        if (frameSlot < 0 || materialSlot < 0) return null;
        // The later stack goes on top: material first means the frame is placed on the material.
        ProjectionRecipe.Direction direction = materialSlot < frameSlot
                ? ProjectionRecipe.Direction.FRAME_ON_MATERIAL
                : ProjectionRecipe.Direction.MATERIAL_ON_FRAME;
        return new Step(frameSlot, materialSlot, input.getItem(frameSlot).copy(),
                input.getItem(materialSlot).copy(), direction);
    }

    private static int size(
            //? if forge {
            CraftingContainer input
            //?} else {
            /*CraftingInput input
            *///?}
            ) {
        //? if forge {
        return input.getContainerSize();
        //?} else {
        /*return input.size();
        *///?}
    }

    private static Level lastMatchedLevel() {
        WeakReference<Level> reference = MATCHED_LEVEL.get();
        return reference == null ? null : reference.get();
    }
}
