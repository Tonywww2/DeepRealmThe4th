package com.tonywww.deeprealm4th.travel;

import com.tonywww.deeprealm4th.worldgen.FourthLayerDimension;
import com.tonywww.deeprealm4th.worldgen.SpiralChunkGenerator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Survival entry from a configured void dimension using mimetic star slurry. */
public final class FourthLayerTravel {
    private FourthLayerTravel() {}

    /** The caller consumes the catalyst only after entry succeeds. */
    public static boolean enterFromVoid(ServerPlayer player) {
        ServerLevel target = player.getServer().getLevel(FourthLayerDimension.KEY);
        if (target == null || !(target.getChunkSource().getGenerator() instanceof SpiralChunkGenerator spiral))
            return false;
        double[] center = spiral.terrain().layout().armCenter(5, 512);
        BlockPos landing = LandingSearch.find((int) Math.floor(center[0]), (int) Math.floor(center[1]), 32,
                (x, z) -> {
                    // Level#getHeight returns minBuildHeight for an unloaded chunk.
                    // Load the destination chunk before probing its actual heightmap.
                    int y = target.getChunk(x >> 4, z >> 4)
                            .getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
                    BlockPos pos = new BlockPos(x, y, z);
                    return safe(target, pos) ? pos : null;
                });
        if (landing == null) return false;
        boolean success = player.teleportTo(target, landing.getX() + 0.5, landing.getY(),
                landing.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot());
        if (!success) return false;
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.sendSystemMessage(Component.translatable("travel.deeprealm_4th.enter_success"));
        return true;
    }

    private static boolean safe(ServerLevel level, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos) || pos.getY() <= level.getMinBuildHeight()
                || pos.getY() + 1 >= level.getMaxBuildHeight()) return false;
        var floor = level.getBlockState(pos.below());
        return floor.isFaceSturdy(level, pos.below(), Direction.UP) && floor.getFluidState().isEmpty()
                && !floor.is(Blocks.MAGMA_BLOCK) && !floor.is(Blocks.CAMPFIRE) && !floor.is(Blocks.SOUL_CAMPFIRE)
                && !floor.is(Blocks.CACTUS) && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }
}
