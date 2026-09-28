package com.tonywww.deeprealm4th.platform.blockentity;

import com.tonywww.deeprealm4th.platform.registry.AstralBlockEntityRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//? if !forge {
/*import net.minecraft.core.HolderLookup;
*///?}

/** NBT signature adapter for the pad's persistent twelve-slot inventory. */
public abstract class VersionedForgingPadBlockEntity extends BlockEntity {
    protected VersionedForgingPadBlockEntity(BlockPos pos, BlockState state) {
        super(AstralBlockEntityRegistration.FORGING_PAD.get(), pos, state);
    }

    protected abstract NonNullList<ItemStack> items();

    //? if forge {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items());
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items(), registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items(), registries);
    }
    *///?}
}
