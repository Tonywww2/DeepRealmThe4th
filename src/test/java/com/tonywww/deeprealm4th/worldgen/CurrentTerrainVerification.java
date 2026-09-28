package com.tonywww.deeprealm4th.worldgen;

import com.tonywww.deeprealm4th.worldgen.layout.SpiralParameters;
import com.tonywww.deeprealm4th.worldgen.terrain.BaseTerrain;
import com.tonywww.deeprealm4th.worldgen.terrain.TerrainProfile;
import java.util.EnumSet;

/** Checks the shipped terrain model without depending on previous generator versions. */
public final class CurrentTerrainVerification {
    private static long checks;

    private static void require(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        require(SpiralParameters.DEFAULT.landFraction() == .75, "Current land fraction");
        for (long seed : new long[]{0, 42, -739221}) {
            BaseTerrain terrain = new BaseTerrain(seed, SpiralParameters.DEFAULT);
            BaseTerrain independent = new BaseTerrain(seed, SpiralParameters.DEFAULT);
            require(terrain.layout().baseHeight(SpiralParameters.DEFAULT.plungeRadius()) == 60,
                    "Central cliff must meet Y=60");
            int land = 0, sampled = 0, ocean = 0, wetOcean = 0, deepOcean = 0;
            int plateau = 0, aridMountain = 0, wideBeach = 0;
            EnumSet<TerrainProfile.Theme> themes = EnumSet.noneOf(TerrainProfile.Theme.class);
            for (int x = -3072; x <= 3072; x += 24) for (int z = -3072; z <= 3072; z += 24) {
                TerrainProfile.Column c = terrain.sample(x, z);
                sampled++;
                if (!c.land()) continue;
                land++;
                themes.add(c.theme());
                require(c.bottom() <= c.top() && c.surface() >= c.top(), "Invalid column height");
                if (c.arm() == 7 && c.edgeDistance() > 80) {
                    ocean++;
                    if (c.wet()) {
                        wetOcean++;
                        if (c.top() < 40) deepOcean++;
                    } else if (c.beach() > .6) wideBeach++;
                }
                if (c.theme() == TerrainProfile.Theme.PLATEAU) plateau++;
                if (c.theme() == TerrainProfile.Theme.ARID_MOUNTAIN) aridMountain++;
                if ((c.arm() == 1 || c.arm() == 7) && c.edgeDistance() > 80
                        && Math.hypot(x - 8, z - 8) > 240) {
                    int slope = Math.max(Math.abs(c.top() - terrain.sample(x + 1, z).top()),
                            Math.abs(c.top() - terrain.sample(x, z + 1).top()));
                    require(slope <= 8, "One-block arid/ocean spike at " + x + "," + z);
                }
                if ((x + z) % 96 == 0)
                    require(c.equals(independent.sample(x, z)), "Nondeterministic column at " + x + "," + z);
            }
            double fraction = land / (double) sampled;
            require(fraction > .64 && fraction < .78, "Unexpected land coverage: " + fraction);
            require(ocean > 500 && wetOcean > ocean * .3 && wetOcean < ocean * .9,
                    "Ocean must contain both land and water");
            require(deepOcean > 100 && wideBeach > 20, "Missing deep water or broad beaches");
            require(plateau > 20 && aridMountain > 20, "Missing arid plateau or mountain");
            require(themes.containsAll(EnumSet.of(TerrainProfile.Theme.DESERT,
                    TerrainProfile.Theme.NETHER, TerrainProfile.Theme.FOREST,
                    TerrainProfile.Theme.OCEAN)), "Missing a major ecology");
            System.out.println("seed=" + seed + " land=" + fraction + " ocean=" + ocean
                    + " wetOcean=" + wetOcean + " deepOcean=" + deepOcean
                    + " plateau=" + plateau + " aridMountain=" + aridMountain);
        }
        System.out.println("CURRENT_TERRAIN_OK checks=" + checks);
    }
}
