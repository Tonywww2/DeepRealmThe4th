package com.tonywww.deeprealm4th.platform.recipe;

import com.tonywww.deeprealm4th.platform.data.ProjectionFrameData;

import com.tonywww.deeprealm4th.item.ProjectionFrameItem;
import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;
import com.tonywww.deeprealm4th.platform.registry.AstralProcessRegistration;
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

/** A shapeless, 3x3-only reset that returns every absorbed stack and the empty frame. */
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
        if (input.getWidth() < 3 || input.getHeight() < 3) return false;
        int size = input.getContainerSize();
        //?} else {
        /*if (input.width() < 3 || input.height() < 3) return false;
        int size = input.size();
        *///?}
        int frames = 0;
        for (int i = 0; i < size; i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ProjectionFrameItem)
                    || ProjectionFrameData.read(stack).isEmpty()) return false;
            frames++;
        }
        return frames == 1;
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
        for (int i = 0; i < size; i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof ProjectionFrameItem) {
                var absorbed = ProjectionFrameData.read(stack);
                for (int j = 0; j < absorbed.size(); j++) {
                    remainders.set(j, absorbed.get(j).stack().copyWithCount(1));
                }
                break;
            }
        }
        return remainders;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width >= 3 && height >= 3; }
    @Override public RecipeSerializer<?> getSerializer() { return AstralProcessRegistration.DISASSEMBLY_SERIALIZER.get(); }
}
