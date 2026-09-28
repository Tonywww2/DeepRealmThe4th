package com.tonywww.deeprealm4th.platform.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

//? if !forge {
/*import net.minecraft.world.ItemInteractionResult;
*///?}

/** Adapts the changed block interaction method to one shared collection callback. */
public abstract class VersionedStarSlurrySeepBlock extends Block {
    protected VersionedStarSlurrySeepBlock(Properties properties) {
        super(properties);
    }

    protected abstract boolean accepts(ItemStack held);
    protected abstract void collect(Level level, BlockPos pos, Player player, InteractionHand hand);

    //? if forge {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!accepts(player.getItemInHand(hand))) return InteractionResult.PASS;
        collect(level, pos, player, hand);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!accepts(held)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        collect(level, pos, player, hand);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    *///?}
}
