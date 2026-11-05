package com.tonywww.deeprealm4th.platform.recipe;

import com.tonywww.deeprealm4th.platform.data.ProjectionFrameData;

import com.tonywww.deeprealm4th.item.ProjectionFrameItem;
import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;
import com.tonywww.deeprealm4th.platform.registry.AstralProcessRegistration;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
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
 * A shapeless reset that returns the empty frame and every absorbed stack. Absorbed stacks refill the
 * grid in slot order; NeoForge trims the input to the frame alone, so the rest go to the inventory.
 */
public final class FrameDisassemblyRecipe extends CustomRecipe {
    //? if forge {
    public FrameDisassemblyRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }
    //?} else {
    /*public FrameDisassemblyRecipe(CraftingBookCategory category) { super(category); }
    *///?}

    @Override public boolean matches(
            //? if forge {
            CraftingContainer input,
            //?} else {
            /*CraftingInput input,
            *///?}
            Level level) {
        //? if forge {
        int size = input.getContainerSize();
        //?} else {
        /*int size = input.size();
        *///?}
        int frames = 0, absorbed = 0;
        for (int i = 0; i < size; i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ProjectionFrameItem)) return false;
            absorbed = ProjectionFrameData.read(stack).size();
            if (absorbed == 0) return false;
            frames++;
        }
        // Stacks beyond the grid need a player to receive them.
        return frames == 1 && (absorbed <= size || CraftingGridContext.player() != null);
    }

    @Override public ItemStack assemble(
            //? if forge {
            CraftingContainer input, RegistryAccess registries
            //?} else {
            /*CraftingInput input, HolderLookup.Provider registries
            *///?}
            ) { return new ItemStack(AstralItemCatalog.PROJECTION_FRAME.get()); }

    @Override public NonNullList<ItemStack> getRemainingItems(
            //? if forge {
            CraftingContainer input
            //?} else {
            /*CraftingInput input
            *///?}
            ) {
        //? if forge {
        int size = input.getContainerSize();
        //?} else {
        /*int size = input.size();
        *///?}
        NonNullList<ItemStack> remainders = NonNullList.withSize(size, ItemStack.EMPTY);
        // Only the frame occupies the grid, so every slot is empty once it is consumed.
        List<Integer> slots = new ArrayList<>();
        List<ItemStack> returned = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            slots.add(i);
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof ProjectionFrameItem) {
                for (var absorbed : ProjectionFrameData.read(stack)) returned.add(absorbed.stack().copyWithCount(1));
            }
        }
        CraftingGridContext.returnStacks(remainders, slots, returned, CraftingGridContext.player());
        return remainders;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public RecipeSerializer<?> getSerializer() { return AstralProcessRegistration.DISASSEMBLY_SERIALIZER.get(); }
}
