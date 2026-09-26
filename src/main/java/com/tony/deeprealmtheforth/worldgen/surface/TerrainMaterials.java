package com.tony.deeprealmtheforth.worldgen.surface;

import com.tony.deeprealmtheforth.worldgen.terrain.SeededNoise;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import com.tony.deeprealmtheforth.worldgen.terrain.SurfaceTransitions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class TerrainMaterials {
    private TerrainMaterials() {}

    public static BlockState at(TerrainProfile.Column c,int y,int x,int z,long seed,int version) {
        if(version>=5&&c.land()&&!c.shore()&&y>=c.bottom()&&y<=c.top()) {
            int depth=c.top()-y;
            if(c.arm()==7&&depth<4) {
                double patch=SeededNoise.fractal(seed^58139,x,z,61,3)+.24*SeededNoise.fractal(seed^87931,x,z,19,2);
                if(c.wet()) {
                    double depthBias=Math.min(1,(TerrainProfile.SEA_LEVEL-c.top())/45.0);
                    return (patch+depthBias*.43>.17?depth<2?Blocks.GRAVEL:Blocks.STONE
                            :patch<-.28?Blocks.STONE:depth==0?Blocks.SAND:Blocks.SANDSTONE).defaultBlockState();
                }
                if(c.beach()<.20&&patch>.02)return Blocks.STONE.defaultBlockState();
            }
        }
        return at(c,y,x,z,seed);
    }

    public static BlockState at(TerrainProfile.Column c, int y, int x, int z, long seed) {
        if (!c.land() || y < c.bottom() || y > c.surface()) return Blocks.AIR.defaultBlockState();
        if (y > c.top()) return (c.fluid() == TerrainProfile.Fluid.LAVA ? Blocks.LAVA : Blocks.WATER).defaultBlockState();
        int depth = c.top() - y;
        // Continuous supported sandstone core down to the seabed, not a cosmetic sand stripe.
        if (c.shore()) return (depth == 0 ? Blocks.SAND : Blocks.SANDSTONE).defaultBlockState();
        if (!c.wet() && c.arm() == 7 && c.top() >= TerrainProfile.SEA_LEVEL) {
            boolean sand = SurfaceTransitions.sand(seed, x, z, c.beach());
            return (sand ? depth == 0 ? Blocks.SAND : depth < 6 ? Blocks.SANDSTONE : Blocks.STONE
                    : depth == 0 ? Blocks.GRASS_BLOCK : depth < 4 ? Blocks.DIRT : ore(seed, x, y, z)).defaultBlockState();
        }
        if (depth < 3 && (c.theme() == TerrainProfile.Theme.RIVER
                || !c.wet() && c.riverDistance() < 3 + 3 * SeededNoise.fractal(seed ^ 95731, x, z, 23, 2))) {
            int sediment = SurfaceTransitions.sediment(seed, x, z, c.arm() == 1);
            return (sediment == 2 ? Blocks.CLAY : sediment == 1 ? depth == 0 ? Blocks.SAND : Blocks.SANDSTONE
                    : Blocks.GRAVEL).defaultBlockState();
        }
        return switch (c.theme()) {
            case DESERT -> (depth == 0 ? Blocks.SAND : depth < 6 ? Blocks.SANDSTONE : Blocks.STONE).defaultBlockState();
            case BADLANDS, PLATEAU -> (depth == 0 ? Blocks.RED_SAND : depth < 4 ? Blocks.RED_SANDSTONE
                    : Math.floorMod(y, 12) < 3 ? Blocks.ORANGE_TERRACOTTA
                    : Math.floorMod(y, 12) < 5 ? Blocks.YELLOW_TERRACOTTA : Blocks.TERRACOTTA).defaultBlockState();
            case RIVER -> (depth < 3 ? Blocks.GRAVEL : Blocks.STONE).defaultBlockState();
            case NETHER -> Blocks.NETHERRACK.defaultBlockState();
            case BASALT, VOLCANO -> (depth < 4 ? Blocks.BASALT : Blocks.BLACKSTONE).defaultBlockState();
            case COAST -> (depth == 0 ? Blocks.SAND : depth < 6 ? Blocks.SANDSTONE : Blocks.STONE).defaultBlockState();
            case OCEAN, DEEP_OCEAN -> (depth == 0 ? Blocks.SAND : depth < 3 ? Blocks.SANDSTONE : Blocks.STONE).defaultBlockState();
            case MOUNTAIN -> (depth == 0 && SurfaceTransitions.snow(seed, x, z, c.top()) ? Blocks.SNOW_BLOCK : Blocks.STONE).defaultBlockState();
            case ARID_MOUNTAIN -> (depth==0?Blocks.COARSE_DIRT:depth<3?Blocks.DIRT:Blocks.STONE).defaultBlockState();
            case PLAINS, FOREST, JUNGLE, ISLAND -> (depth == 0 ? Blocks.GRASS_BLOCK : depth < 4 ? Blocks.DIRT
                    : ore(seed, x, y, z)).defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }

    private static net.minecraft.world.level.block.Block ore(long seed, int x, int y, int z) {
        // Small deterministic deposits, confined to the solid interior of the land ribbon.
        long h = SeededNoise.hash(seed ^ y / 3, Math.floorDiv(x, 3), Math.floorDiv(z, 3));
        int roll = (int) Math.floorMod(h, 180);
        return roll == 0 ? Blocks.IRON_ORE : roll < 4 ? Blocks.COAL_ORE : Blocks.STONE;
    }
}
