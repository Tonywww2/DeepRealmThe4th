package com.tony.deeprealmtheforth.worldgen.terrain;

import com.tony.deeprealmtheforth.worldgen.layout.SpiralLayout;

/** Pure surface fields shared by generation, height queries and diagnostic previews. */
public final class SurfaceTransitions {
    private SurfaceTransitions() {}
    public static double snowLine(long seed, double x, double z) {
        return 123 + 23 * SeededNoise.fractal(seed ^ 81273, x, z, 210, 2)
                + 7 * SeededNoise.fractal(seed ^ 52719, x, z, 39, 2);
    }
    public static boolean snow(long seed, int x, int z, int y) { return y >= snowLine(seed, x, z); }
    public static double beachWidth(long seed, double x, double z) {
        return 25 + 14 * SeededNoise.fractal(seed ^ 23871, x, z, 210, 2);
    }
    public static double beach(long seed, double x, double z, double inland) {
        double patch = 5 * SeededNoise.fractal(seed ^ 81931, x, z, 34, 2);
        return 1 - SpiralLayout.smooth(beachWidth(seed, x, z) - 8, beachWidth(seed, x, z) + 10, inland + patch);
    }
    public static boolean sand(long seed, int x, int z, double beach) {
        return beach > .5 + .24 * SeededNoise.fractal(seed ^ 71357, x, z, 13, 2);
    }
    /** 0=gravel, 1=sand, 2=clay; spatial patches, not per-block speckle. */
    public static int sediment(long seed, int x, int z, boolean arid) {
        double n = SeededNoise.fractal(seed ^ 37911, x, z, 24, 2);
        return n > .30 ? 2 : n < (arid ? .20 : -.12) ? 1 : 0;
    }
}
