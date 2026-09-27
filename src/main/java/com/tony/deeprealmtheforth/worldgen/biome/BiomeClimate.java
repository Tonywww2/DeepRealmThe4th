package com.tony.deeprealmtheforth.worldgen.biome;

import com.tony.deeprealmtheforth.worldgen.layout.SpiralLayout;
import com.tony.deeprealmtheforth.worldgen.terrain.SeededNoise;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile.Theme;
import java.util.List;
import java.util.function.Function;

/** World-space ecological patches; deliberately independent of height and spiral shear. */
public final class BiomeClimate {
    private final long seed;
    private final ThreadLocal<Memo> memo=ThreadLocal.withInitial(Memo::new);
    private static final class Memo {
        final String[] keys=new String[128];final long[] salts=new long[128];
        long x,z;Point point;
    }

    public BiomeClimate(long seed) { this.seed = seed; }

    public Point point(double x, double z) {
        Memo m=memo.get();long bx=Double.doubleToLongBits(x),bz=Double.doubleToLongBits(z);
        if(m.point!=null&&m.x==bx&&m.z==bz)return m.point;
        // Rotation hides the noise lattice; bounded domain warp makes irregular
        // borders without increasingly stretching biomes farther from the center.
        double a = x * .8 - z * .6, b = x * .6 + z * .8;
        Point result=new Point(a + 54 * SeededNoise.fractal(seed ^ 17293, a, b, 320, 2)
                + 12 * SeededNoise.fractal(seed ^ 97631, a, b, 90, 2),
                b + 54 * SeededNoise.fractal(seed ^ 61871, a, b, 320, 2)
                + 12 * SeededNoise.fractal(seed ^ 31819, a, b, 90, 2));
        result=new Point(result.x()+18*SeededNoise.fractal(seed^791821,a,b,47,2)
                +5*SeededNoise.fractal(seed^717893,a,b,17,2),
                result.z()+18*SeededNoise.fractal(seed^271931,a,b,47,2)+5*SeededNoise.fractal(seed^319827,a,b,17,2));
        m.x=bx;m.z=bz;m.point=result;return result;
    }

    public double score(Point p, String key, double scale) {
        Memo m=memo.get();int i=key.hashCode()&127;
        if(!key.equals(m.keys[i])){m.keys[i]=key;m.salts[i]=keySeed(key);}
        long salt=m.salts[i];
        return .78 * SeededNoise.fractal(seed ^ salt, p.x(), p.z(), scale, 2)
                + .22 * SeededNoise.fractal(seed ^ salt ^ 57193, p.x(), p.z(), scale * .38, 2);
    }

    /** Independent fields compete, rather than slicing a single scalar into ordered stripes. */
    public <T> T choose(Point p, List<T> entries, Function<T, String> key, double scale) {
        if (entries.isEmpty()) throw new IllegalArgumentException("Empty biome candidates");
        if (entries.size() == 1) return entries.get(0);
        T chosen = null; String chosenKey = null; double best = Double.NEGATIVE_INFINITY;
        for (T candidate : entries) {
            String name = key.apply(candidate);
            double value = score(p, name, scale);
            if (value > best || value == best && (chosenKey == null || name.compareTo(chosenKey) < 0)) {
                chosen = candidate; chosenKey = name; best = value;
            }
        }
        return chosen;
    }

    /** Only ecological labels change. Height, floor, water and boundary calculations do not. */
    public Theme theme(int x, int z, Theme physical, int arm, double rise) {
        if (physical == Theme.VOID || physical == Theme.RIVER || physical == Theme.VOLCANO
                || physical==Theme.PLATEAU || physical==Theme.ARID_MOUNTAIN || arm == 7) return physical;
        Point p = point(x + .5, z + .5);
        if (arm == 1) {
            double upland = .10 * SpiralLayout.smooth(8, 32, rise);
            return score(p, "arid/badlands", 320) + upland > score(p, "arid/desert", 320) ? Theme.BADLANDS : Theme.DESERT;
        }
        if (arm == 3) return score(p, "infernal/basalt", 260) - .12 > score(p, "infernal/nether", 260)
                ? Theme.BASALT : Theme.NETHER;
        Theme result = Theme.FOREST;
        double best = score(p, result.pool(), 340) + .04;
        for (Theme candidate : new Theme[]{Theme.PLAINS, Theme.JUNGLE}) {
            double value = score(p, candidate.pool(), 340);
            // Meadow/forest can occupy high slopes, but plains should favor
            // low relief instead of coloring a 150-block summit as flatland.
            if (candidate == Theme.PLAINS) value -= .75 * SpiralLayout.smooth(6, 28, rise);
            if (value > best) { best = value; result = candidate; }
        }
        // Mountains remain tied to genuine uplands, but a ridge's contour is
        // no longer the sole biome boundary. Low valleys cannot become peaks.
        double highland = score(p, Theme.MOUNTAIN.pool(), 260) + .85 * SpiralLayout.smooth(16, 56, rise) - .35;
        return rise > 16 && highland > best ? Theme.MOUNTAIN : result;
    }

    public boolean useExtras(Point p, String region) { return score(p, "extra/" + region, 360) > .30; }

    private static long keySeed(String key) {
        // Stable resource-key identity, independent of tag ordering and Java hash collisions.
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < key.length(); i++) { h ^= key.charAt(i); h *= 0x100000001b3L; }
        return h;
    }

    public record Point(double x, double z) {}
}
