package com.tony.deeprealmtheforth.worldgen.terrain;

import com.tony.deeprealmtheforth.worldgen.layout.SpiralLayout;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.biome.BiomeClimate;
import com.tony.deeprealmtheforth.worldgen.hydrology.ClimateSnapshot;
import com.tony.deeprealmtheforth.worldgen.hydrology.WatershedRivers;

/** Single, pure column model used by generation, ecology, queries, protection and previews. */
public final class TerrainProfile {
    public static final int SEA_LEVEL = 64;
    public static final int SHORE_WIDTH = 3;
    public enum Theme {
        VOID("void", "void"), DESERT("arid/desert", "arid"), BADLANDS("arid/badlands", "arid"),
        RIVER("arid/river", "arid"), NETHER("infernal/nether", "infernal"),
        BASALT("infernal/basalt", "infernal"), VOLCANO("infernal/basalt", "infernal"),
        PLAINS("temperate_tropical/plains", "temperate_tropical"),
        FOREST("temperate_tropical/forest", "temperate_tropical"),
        JUNGLE("temperate_tropical/jungle", "temperate_tropical"),
        MOUNTAIN("temperate_tropical/mountain", "temperate_tropical"),
        COAST("oceanic/coast", "oceanic"), OCEAN("oceanic/ocean", "oceanic"),
        DEEP_OCEAN("oceanic/deep", "oceanic"), ISLAND("oceanic/island", "oceanic"),
        PLATEAU("arid/plateau", "arid"), ARID_MOUNTAIN("arid/mountain", "arid");
        private final String pool, region;
        Theme(String pool, String region) { this.pool = pool; this.region = region; }
        public String pool() { return pool; }
        public String region() { return region; }
    }
    public enum Fluid { NONE, WATER, LAVA }
    public record Column(int arm, int top, int bottom, int fluidLevel, Fluid fluid,
                         Theme theme, long cell, double edgeDistance, boolean shore,
                         double riverDistance, double beach) {
        public boolean land() { return theme != Theme.VOID; }
        public boolean wet() { return fluid != Fluid.NONE && fluidLevel > top; }
        public int surface() { return wet() ? fluidLevel : top; }
    }

    private final BaseTerrain base;
    private final long seed;
    private final SpiralLayout layout;
    private final VortexField field;
    private final BiomeClimate biomes;
    private final WatershedRivers rivers;
    private final int generationVersion;
    // Bounded per-thread memoization, never part of terrain semantics or persisted state.
    private final ThreadLocal<Cache> cache = ThreadLocal.withInitial(Cache::new);
    private static final class Cache {
        final long[] keys = new long[16384];
        final Column[] raw = new Column[16384], result = new Column[16384];
    }

    public TerrainProfile(long seed, SpiralParameters parameters) {
        this(seed, parameters, null);
    }
    /** Immutable registry input is captured after loader biome modifiers, before generation. */
    public TerrainProfile(long seed, SpiralParameters parameters, ClimateSnapshot climate) {
        this(seed,parameters,climate,climate==null?1:3);
    }
    public TerrainProfile(long seed, SpiralParameters parameters, ClimateSnapshot climate,int version) {
        if(version!=1&&version!=3&&version!=4&&version!=5||(version==1)!=(climate==null))throw new IllegalArgumentException("Invalid terrain version/climate");
        generationVersion=version;
        this.seed = seed; this.base = climate == null ? new BaseTerrain(seed, parameters)
                : BaseTerrain.naturalWorld(seed, parameters,version);
        this.layout = base.layout(); this.field = base.field(); this.biomes = base.biomes();
        this.rivers = climate == null ? null : new WatershedRivers(seed, base, climate);
    }
    public int generationVersion() { return generationVersion; }
    public WatershedRivers hydrology() { return rivers; }
    public BaseTerrain baseTerrain() { return base; }
    public long seed() { return seed; }
    public SpiralLayout layout() { return layout; }
    public VortexField field() { return field; }
    public BiomeClimate biomes() { return biomes; }

    public Column sample(int x, int z) {
        Cache memo = cache.get(); long key = key(x, z); int index = index(key);
        if (memo.raw[index] != null && memo.keys[index] == key && memo.result[index] != null) return memo.result[index];
        Column c = raw(x, z), result = c;
        // Connected river grades may have different water heights. Do not replace
        // their shared cross-section with the legacy same-level basin wall.
        boolean connectedRiver = rivers != null && c.fluid() == Fluid.WATER && c.arm() != 7;
        if (!connectedRiver && (c.wet() || c.land() && c.arm() == 7)) {
            boolean ocean = c.arm() == 7;
            int radius = ocean ? SHORE_WIDTH : 1;
            boolean bank = false, naturalShore = false; int bottom = c.bottom();
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                Column n = raw(x + dx, z + dz);
                if (n.land()) bottom = Math.min(bottom, n.bottom());
                boolean sameBasin = n.wet() && n.fluid() == c.fluid() && n.fluidLevel() == c.fluidLevel();
                if (c.wet() && (!n.land() || n.top() < c.fluidLevel() && !sameBasin)) bank = true;
                if (ocean && !c.wet() && n.wet() && n.fluid() == Fluid.WATER) naturalShore = true;
            }
            // Natural island banks already contain the sea: put the three-block
            // sandstone core inside them instead of replacing shallow water by a flat ring.
            if (naturalShore) result = new Column(c.arm(), Math.max(SEA_LEVEL, c.top()), bottom, -64, Fluid.NONE,
                    c.theme(), c.cell(), c.edgeDistance(), true, c.riverDistance(), 1);
            if (bank) {
                int bankTop = c.fluidLevel() + (ocean ? 1 + (int) (2 + 2 *
                        SeededNoise.fractal(seed ^ 92871, x, z, 53, 2)) : 0);
                result = new Column(c.arm(), bankTop, bottom, -64, Fluid.NONE,
                        ocean ? Theme.COAST : c.theme(), c.cell(), c.edgeDistance(), ocean, c.riverDistance(), ocean ? 1 : 0);
            }
        }
        // Neighborhood lookups may have collided with this slot.
        memo.keys[index] = key; memo.raw[index] = c; memo.result[index] = result;
        return result;
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
    private static int index(long k) { return (int) SeededNoise.hash(531, k >> 32, (int) k) & 16383; }
    private Column raw(int x, int z) {
        Cache memo = cache.get(); long key = key(x, z); int index = index(key);
        if (memo.raw[index] != null && memo.keys[index] == key) return memo.raw[index];
        Column c = calculate(x, z);
        memo.keys[index] = key; memo.raw[index] = c; memo.result[index] = null;
        return c;
    }

    private Column calculate(int x, int z) {
        BaseTerrain.Shape s = base.shape(x, z);
        VortexField.Point p = s.point();
        if (!p.land()) return base.sample(x, z);
        double u = s.u(), v = s.v(), top = s.top();
        double rim = layout.parameters().plungeRadius();
        int water = s.water(); Fluid fluid = s.fluid(); Theme theme = s.theme();
        long variant = s.variant();
        double riverDistance = Double.POSITIVE_INFINITY;
        if (rivers != null) {
            if ((p.arm() == 1 || p.arm() == 5) && p.radius() > rim + 144 && p.edgeDistance() > 12) {
                WatershedRivers.Sample river = rivers.sample(x, z, top);
                riverDistance = river.distance();
                if (riverDistance < 0) {
                    theme = Theme.RIVER; water = river.water(); fluid = Fluid.WATER; variant = river.basin();
                    top = water - Math.max(1, river.depth() * (1 - Math.exp(riverDistance / 3.2)));
                } else if (Double.isFinite(riverDistance)) {
                    double bank = river.water() + .38 * riverDistance + .018 * riverDistance * riverDistance;
                    top = Math.min(top, bank);
                    // The short upstream flow envelope contains vanilla falling/spreading
                    // water at one-block grades without blocking the river cross-section.
                    top = Math.max(top, river.containment());
                }
            }
            Column c = base.finish(x, z, s, top, water, fluid, theme, variant, riverDistance);
            if (Double.isFinite(riverDistance)) {
                Column original = base.finish(x, z, s, s.top(), s.water(), s.fluid(), s.theme(), s.variant(), Double.POSITIVE_INFINITY);
                return new Column(c.arm(), Math.max(original.bottom() + 3, c.top()), original.bottom(), c.fluidLevel(),
                        c.fluid(), c.theme(), c.cell(), c.edgeDistance(), c.shore(), c.riverDistance(), c.beach());
            }
            return c;
        }
        if ((p.arm() == 1 || p.arm() == 5) && p.radius() > rim + 60) {
            RiverNetwork.Sample river = RiverNetwork.sample(seed, u, v, layout);
            double clearance = SpiralLayout.smooth(8, 26, p.edgeDistance())
                    * SpiralLayout.smooth(rim + 60, rim + 140, p.radius());
            riverDistance = river.distance();
            // Smooth minimum joins the valley to existing relief without a
            // fixed-width shoulder. Banks widen naturally where relief is higher.
            double h = Math.max(0, 4 - Math.abs(top - river.valleyFloor())) / 4;
            double valleyTop = Math.min(top, river.valleyFloor()) - h * h;
            top += (valleyTop - top) * clearance;
            if (river.distance() >= 0 && clearance > .999) {
                double bank = river.level() + .16 * river.distance();
                top += Math.max(0, bank - top) * (1 - SpiralLayout.smooth(0, 24, river.distance()));
                if (river.distance() < 4) top = Math.max(river.level(), top);
            }
            if (river.distance() < 0 && clearance > .999) {
                theme = Theme.RIVER; water = river.level(); fluid = Fluid.WATER;
                top = water - 5.5 * (1 - Math.exp(river.distance() / 3.5)); variant = river.catchment();
            }
        }
        return base.finish(x, z, s, top, water, fluid, theme, variant, riverDistance);
    }
}
