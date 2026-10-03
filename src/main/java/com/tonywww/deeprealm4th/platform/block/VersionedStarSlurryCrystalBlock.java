package com.tonywww.deeprealm4th.platform.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

//? if !forge {
/*import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tonywww.deeprealm4th.block.StarSlurryCrystalBlock;
import net.minecraft.world.ItemInteractionResult;
*///?}

/** Adapts crystal block codecs and bottle-use signatures across the two supported versions. */
public abstract class VersionedStarSlurryCrystalBlock extends AmethystClusterBlock {
    private final int crystalHeight;
    private final int shapeOffset;

    protected VersionedStarSlurryCrystalBlock(int height, int offset, Properties properties) {
        super(height, offset, properties);
        this.crystalHeight = height;
        this.shapeOffset = offset;
    }

    protected abstract boolean accepts(ItemStack held);
    protected abstract void collect(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand);

    //? if forge {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!accepts(player.getItemInHand(hand))) return InteractionResult.PASS;
        collect(state, level, pos, player, hand);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    //?} else {
    /*private static final MapCodec<AmethystClusterBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.INT.fieldOf("height").forGetter(block -> ((VersionedStarSlurryCrystalBlock) block).crystalHeight),
                    Codec.INT.fieldOf("aabb_offset").forGetter(block -> ((VersionedStarSlurryCrystalBlock) block).shapeOffset),
                    propertiesCodec()
            ).apply(instance, StarSlurryCrystalBlock::new));

    @Override
    public MapCodec<AmethystClusterBlock> codec() { return CODEC; }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!accepts(held)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        collect(state, level, pos, player, hand);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    *///?}
}
