package com.tonywww.deeprealm4th.worldgen.surface;

import com.tonywww.deeprealm4th.worldgen.terrain.SeededNoise;
import com.tonywww.deeprealm4th.worldgen.terrain.TerrainProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Conservative first-release decoration: bounded writes, with no neighboring-chunk mutation. */
public final class SpiralDecoration {
    private SpiralDecoration() {}

    public static void decorate(ChunkAccess chunk, TerrainProfile terrain) {
        int startX = chunk.getPos().getMinBlockX(), startZ = chunk.getPos().getMinBlockZ();
        for (int lx = 3; lx <= 12; lx += 3) {
            for (int lz = 3; lz <= 12; lz += 3) {
                int x = startX + lx, z = startZ + lz;
                long h = SeededNoise.hash(terrain.seed() ^ 3913, x, z);
                TerrainProfile.Column c = terrain.sample(x, z);
                if (!c.land() || c.wet() || c.edgeDistance() < 7) continue;
                boolean forest = c.theme() == TerrainProfile.Theme.FOREST || c.theme() == TerrainProfile.Theme.JUNGLE;
                boolean island = c.theme() == TerrainProfile.Theme.ISLAND;
                if ((forest && Math.floorMod(h, 4) == 0) || (island && Math.floorMod(h, 14) == 0)) {
                    tree(chunk, terrain, x, c.top() + 1, z, c.theme() == TerrainProfile.Theme.JUNGLE);
                } else if (Math.floorMod(h, 5) == 0 && (forest || c.theme() == TerrainProfile.Theme.PLAINS)) {
                    placeIfAir(chunk, new BlockPos(x, c.top() + 1, z),
                            (Math.floorMod(h, 3) == 0 ? Blocks.POPPY : Blocks.DANDELION).defaultBlockState());
                } else if (c.theme() == TerrainProfile.Theme.DESERT && Math.floorMod(h, 10) == 0) {
                    placeIfAir(chunk, new BlockPos(x, c.top() + 1, z), Blocks.DEAD_BUSH.defaultBlockState());
                }
            }
        }
    }

    private static void tree(ChunkAccess chunk, TerrainProfile terrain, int x, int y, int z, boolean jungle) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                TerrainProfile.Column ground = terrain.sample(x + dx, z + dz);
                if (!ground.land() || ground.wet() || Math.abs(ground.top() + 1 - y) > 2) return;
                for (int dy = 0; dy <= 7; dy++) {
                    if (dy > 2 && !chunk.getBlockState(new BlockPos(x + dx, y + dy, z + dz)).isAir()) return;
                }
            }
        }
        if (!chunk.getBlockState(new BlockPos(x, y - 1, z)).is(Blocks.GRASS_BLOCK)) return;
        Block wood = jungle ? Blocks.JUNGLE_LOG : Blocks.OAK_LOG;
        Block leaves = jungle ? Blocks.JUNGLE_LEAVES : Blocks.OAK_LEAVES;
        int trunk = jungle ? 6 : 4;
        for (int dy = 0; dy < trunk; dy++) placeIfAir(chunk, new BlockPos(x, y + dy, z), wood.defaultBlockState());
        for (int dy = trunk - 2; dy <= trunk + 1; dy++) {
            int radius = dy == trunk + 1 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 1) continue;
                    int distance = Math.max(1, Math.min(6, Math.abs(dx) + Math.abs(dz) + Math.max(0, dy - trunk + 1)));
                    placeIfAir(chunk, new BlockPos(x + dx, y + dy, z + dz),
                            leaves.defaultBlockState().setValue(LeavesBlock.DISTANCE, distance));
                }
            }
        }
    }

    private static void placeIfAir(ChunkAccess chunk, BlockPos pos, BlockState state) {
        if ((pos.getX() >> 4) != chunk.getPos().x || (pos.getZ() >> 4) != chunk.getPos().z
                || !chunk.getBlockState(pos).isAir()) return;
        chunk.setBlockState(pos, state, false);
        for (Heightmap.Types type : Heightmap.Types.values()) {
            if (chunk.hasPrimedHeightmap(type)) chunk.getOrCreateHeightmapUnprimed(type)
                    .update(pos.getX() & 15, pos.getY(), pos.getZ() & 15, state);
        }
    }
}
