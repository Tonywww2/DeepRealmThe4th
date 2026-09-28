package com.tonywww.deeprealm4th.worldgen.biome;

import com.google.common.base.Suppliers;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tonywww.deeprealm4th.platform.worldgen.VersionedBiomeSource;
import com.tonywww.deeprealm4th.worldgen.codec.WorldgenCodecs;
import com.tonywww.deeprealm4th.platform.worldgen.BiomeClimateAdapter;
import com.tonywww.deeprealm4th.worldgen.layout.SpiralParameters;
import com.tonywww.deeprealm4th.worldgen.terrain.TerrainProfile;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

public final class SpiralBiomeSource extends VersionedBiomeSource {
    public static final MapCodec<SpiralBiomeSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            WorldgenCodecs.PARAMETERS.optionalFieldOf("parameters", SpiralParameters.DEFAULT).forGetter(s -> s.parameters),
            Codec.unboundedMap(Codec.STRING, BiomePool.CODEC).fieldOf("pools").forGetter(s -> s.pools),
            Codec.intRange(5, 5).fieldOf("generation_version").forGetter(s -> s.generationVersion)
    ).apply(i, SpiralBiomeSource::new));
    public static final Codec<SpiralBiomeSource> CODEC = MAP_CODEC.codec();

    private final SpiralParameters parameters;
    private final Map<String, BiomePool> pools;
    private final int generationVersion;
    private final Supplier<Map<String, List<Holder<Biome>>>> resolved;
    private final Supplier<Map<String, List<Holder<Biome>>>> extraBiomes;
    private volatile TerrainProfile terrain;
    // The biome is deliberately independent of Y. Vanilla asks again for every
    // quart-height layer; keep exact X/Z answers per source and worker, not per world globally.
    private final ThreadLocal<BiomeCache> biomeCache = ThreadLocal.withInitial(BiomeCache::new);
    private final Supplier<Map<Holder<Biome>, String>> biomeKeys;
    private static final class BiomeCache {
        final long[] keys = new long[4096];
        final Holder<?>[] values = new Holder<?>[4096];
    }

    public SpiralBiomeSource(SpiralParameters parameters, Map<String, BiomePool> pools, int generationVersion) {
        if (generationVersion != 5) throw new IllegalArgumentException("Only generation version 5 is supported");
        this.parameters = parameters;
        this.generationVersion = generationVersion;
        this.pools = Map.copyOf(pools);
        for (String name : List.of("void", "arid", "infernal", "temperate_tropical", "oceanic")) {
            if (!pools.containsKey(name)) throw new IllegalArgumentException("Missing biome pool: " + name);
        }
        // Tag-backed HolderSets are resolved only after the dynamic registries are bound.
        resolved = Suppliers.memoize(this::resolvePools);
        extraBiomes = Suppliers.memoize(this::resolveExtras);
        biomeKeys = Suppliers.memoize(() -> {
            Map<Holder<Biome>, String> keys = new HashMap<>();
            resolved.get().values().forEach(entries -> entries.forEach(h ->
                    keys.put(h, h.unwrapKey().orElseThrow().location().toString())));
            return Map.copyOf(keys);
        });
    }

    public synchronized void bindSeed(long seed) {
        if (terrain != null && terrain.seed() != seed) throw new IllegalStateException("Generator reused across world seeds");
        if (terrain == null) terrain = new TerrainProfile(seed, parameters, BiomeClimateAdapter.capture(this));
    }

    public TerrainProfile terrain() {
        TerrainProfile current = terrain;
        if (current == null) throw new IllegalStateException("Spiral world seed has not been initialized");
        return current;
    }

    public SpiralParameters parameters() { return parameters; }
    public int generationVersion() { return generationVersion; }

    private Map<String, List<Holder<Biome>>> resolvePools() {
        Map<String, List<Holder<Biome>>> result = new HashMap<>();
        pools.forEach((name, pool) -> {
            List<Holder<Biome>> entries = pool.biomes().stream().distinct()
                    .sorted(Comparator.comparing(h -> h.unwrapKey().orElseThrow().location().toString())).toList();
            if (entries.isEmpty()) {
                LogUtils.getLogger().warn("Empty fourth-layer biome pool {}; using {}", name, pool.fallback());
                entries = List.of(pool.fallback());
            }
            result.put(name, entries);
        });
        return Map.copyOf(result);
    }

    private Map<String, List<Holder<Biome>>> resolveExtras() {
        Map<String, List<Holder<Biome>>> result = new HashMap<>();
        for (String region : List.of("arid", "infernal", "temperate_tropical", "oceanic")) {
            Set<Holder<Biome>> categorized = new HashSet<>();
            resolved.get().forEach((key, entries) -> { if (key.startsWith(region + "/")) categorized.addAll(entries); });
            result.put(region, resolved.get().get(region).stream().filter(h -> !categorized.contains(h)).toList());
        }
        return Map.copyOf(result);
    }

    @Override
    protected MapCodec<? extends BiomeSource> mapCodec() { return MAP_CODEC; }

    @Override
    protected Codec<? extends BiomeSource> forgeCodec() { return CODEC; }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(resolved.get().values().stream().flatMap(Collection::stream),
                pools.values().stream().map(BiomePool::fallback)).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return biomeAt(QuartPos.toBlock(x) + 2, QuartPos.toBlock(z) + 2);
    }

    public Holder<Biome> biomeAt(int x, int z) {
        BiomeCache cache = biomeCache.get();
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        int slot = (int) com.tonywww.deeprealm4th.worldgen.terrain.SeededNoise.hash(0, x, z) & 4095;
        if (cache.values[slot] != null && cache.keys[slot] == key) {
            @SuppressWarnings("unchecked") Holder<Biome> hit = (Holder<Biome>) cache.values[slot];
            return hit;
        }
        Holder<Biome> result = select(terrain().sample(x, z), x, z);
        cache.keys[slot] = key;
        cache.values[slot] = result;
        return result;
    }

    /** H0 -> base biome, never querying the final river overlay. */
    public Holder<Biome> baseBiomeAt(int x, int z) {
        return select(terrain().baseTerrain().sample(x, z), x, z);
    }

    public Map<String, List<Holder<Biome>>> resolvedPools() { return resolved.get(); }
    public Map<String, List<Holder<Biome>>> resolvedExtras() { return extraBiomes.get(); }

    private Holder<Biome> select(TerrainProfile.Column column, int x, int z) {
        if (column.theme() == TerrainProfile.Theme.RIVER && column.wet()) {
            Holder<Biome> original = BaseBiomeResolver.select(terrain().biomes(), terrain().baseTerrain().sample(x, z),
                    x, z, resolved.get(), extraBiomes.get(), biomeKeys.get()::get);
            if (BiomeClimateAdapter.snowClimate(original.value(), new net.minecraft.core.BlockPos(x, column.fluidLevel(), z))) {
                List<Holder<Biome>> cold = resolved.get().get("hydrology/frozen_river");
                if (cold != null) return terrain().biomes().choose(terrain().biomes().point(x + .5, z + .5), cold,
                        biomeKeys.get()::get, 190);
            }
        }
        return BaseBiomeResolver.select(terrain().biomes(), column, x, z, resolved.get(), extraBiomes.get(),
                biomeKeys.get()::get);
    }
}
