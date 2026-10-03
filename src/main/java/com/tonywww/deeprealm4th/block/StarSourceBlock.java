package com.tonywww.deeprealm4th.block;

import com.tonywww.deeprealm4th.platform.block.VersionedStarSourceBlock;
import com.tonywww.deeprealm4th.platform.registry.AstralBlockRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Grows star-slurry crystals with vanilla budding-amethyst timing and attachment rules. */
public final class StarSourceBlock extends VersionedStarSourceBlock {
    private static final Direction[] DIRECTIONS = Direction.values();

    public StarSourceBlock(Properties properties) { super(properties); }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(BuddingAmethystBlock.GROWTH_CHANCE) != 0) return;
        Direction direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        BlockPos crystalPos = pos.relative(direction);
        BlockState crystal = level.getBlockState(crystalPos);
        Block next = null;
        if (BuddingAmethystBlock.canClusterGrowAtState(crystal)) {
            next = AstralBlockRegistration.SMALL_STAR_SLURRY_BUD.get();
        } else if (crystal.getBlock() instanceof StarSlurryCrystalBlock
                && crystal.getValue(AmethystClusterBlock.FACING) == direction) {
            if (crystal.is(AstralBlockRegistration.SMALL_STAR_SLURRY_BUD.get())) {
                next = AstralBlockRegistration.MEDIUM_STAR_SLURRY_BUD.get();
            } else if (crystal.is(AstralBlockRegistration.MEDIUM_STAR_SLURRY_BUD.get())) {
                next = AstralBlockRegistration.LARGE_STAR_SLURRY_BUD.get();
            } else if (crystal.is(AstralBlockRegistration.LARGE_STAR_SLURRY_BUD.get())) {
                next = AstralBlockRegistration.STAR_SLURRY_CLUSTER.get();
            }
        }
        if (next != null) {
            level.setBlockAndUpdate(crystalPos, next.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, direction)
                    .setValue(AmethystClusterBlock.WATERLOGGED, crystal.getFluidState().getType() == Fluids.WATER));
        }
    }
}
