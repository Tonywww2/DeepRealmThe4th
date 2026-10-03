package com.tonywww.deeprealm4th.worldgen.surface;

import com.tonywww.deeprealm4th.platform.PlatformIds;
import com.tonywww.deeprealm4th.platform.registry.AstralBlockRegistration;
import com.tonywww.deeprealm4th.worldgen.SpiralChunkGenerator;
import com.tonywww.deeprealm4th.worldgen.terrain.SeededNoise;
import com.tonywww.deeprealm4th.worldgen.terrain.TerrainProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Places a vanilla geode in isolated void chunks, with 2–4 renewable star sources on its inner wall. */
public final class VoidStarSlurryGeodes {
    private static final long SALT = 0x52A1C4E0D3L;
    // Keep in sync with the configured geode's +/- generation offsets.
    private static final int EXTENT = 8;
    private static final int MIN_Y = -32;
    private static final int MAX_Y = 128;

    private VoidStarSlurryGeodes() {}

    public static void place(WorldGenLevel level, ChunkAccess chunk, SpiralChunkGenerator generator) {
        TerrainProfile terrain = generator.terrain();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        long seed = SeededNoise.hash(terrain.seed() ^ SALT, chunkX, chunkZ);
        if (Math.floorMod(seed, 64) != 0) return;

        int x = chunk.getPos().getMinBlockX() + 8;
        int z = chunk.getPos().getMinBlockZ() + 8;
        // Inner placements may extend one block beyond the configured geode bounds.
        for (int dx = -EXTENT - 1; dx <= EXTENT + 1; dx++) {
            for (int dz = -EXTENT - 1; dz <= EXTENT + 1; dz++) {
                if (terrain.sample(x + dx, z + dz).land()) return;
            }
        }

        int y = MIN_Y + EXTENT + (int) Math.floorMod(SeededNoise.hash(seed, 4, 0),
                MAX_Y - MIN_Y - 2 * EXTENT + 1);
        BlockPos origin = new BlockPos(x, y, z);
        PlacedFeature geode = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .get(PlatformIds.id("void_star_slurry_geode"));
        if (geode == null) throw new IllegalStateException("Missing vanilla geode configuration: deeprealm_4th:void_star_slurry_geode");
        if (!geode.place(level, generator, RandomSource.create(seed), origin)) return;

        List<BlockPos> innerWall = new ArrayList<>();
        for (int dx = -EXTENT; dx <= EXTENT; dx++) {
            for (int dy = -EXTENT; dy <= EXTENT; dy++) {
                for (int dz = -EXTENT; dz <= EXTENT; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    var state = level.getBlockState(pos);
                    if (!state.is(Blocks.AMETHYST_BLOCK)) continue;
                    int distance = dx * dx + dy * dy + dz * dz;
                    for (Direction direction : Direction.values()) {
                        BlockPos neighbor = pos.relative(direction);
                        if (neighbor.distSqr(origin) < distance && level.getBlockState(neighbor).isAir()) {
                            innerWall.add(pos);
                            break;
                        }
                    }
                }
            }
        }
        innerWall.sort(Comparator.comparingLong(pos ->
                SeededNoise.hash(seed ^ pos.getY(), pos.getX(), pos.getZ())));
        int count = Math.min(innerWall.size(), 2 + (int) Math.floorMod(SeededNoise.hash(seed, 5, 0), 3));
        var stages = List.of(AstralBlockRegistration.SMALL_STAR_SLURRY_BUD.get(),
                AstralBlockRegistration.MEDIUM_STAR_SLURRY_BUD.get(),
                AstralBlockRegistration.LARGE_STAR_SLURRY_BUD.get(),
                AstralBlockRegistration.STAR_SLURRY_CLUSTER.get());
        for (int i = 0; i < count; i++) {
            BlockPos source = innerWall.get(i);
            level.setBlock(source, AstralBlockRegistration.STAR_SOURCE.get().defaultBlockState(), 2);
            for (Direction direction : Direction.values()) {
                BlockPos crystal = source.relative(direction);
                if (crystal.distSqr(origin) >= source.distSqr(origin) || !level.getBlockState(crystal).isAir()) continue;
                int stage = (int) Math.floorMod(SeededNoise.hash(seed, i, 6), stages.size());
                level.setBlock(crystal, stages.get(stage).defaultBlockState()
                        .setValue(AmethystClusterBlock.FACING, direction), 2);
                break;
            }
        }
    }
}
