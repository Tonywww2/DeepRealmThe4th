package com.tonywww.deeprealm4th.platform.worldgen;

import com.tonywww.deeprealm4th.worldgen.biome.SpiralBiomeSource;
import com.tonywww.deeprealm4th.worldgen.hydrology.ClimateSnapshot;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/** Loader-patched API shared by Forge 47.4.0 and NeoForge 21.1.233. Call after server init. */
public final class BiomeClimateAdapter {
    private BiomeClimateAdapter() {}
    public static ClimateSnapshot capture(SpiralBiomeSource source) {
        Map<String, ClimateSnapshot.Climate> climates = new TreeMap<>();
        source.possibleBiomes().forEach(holder -> climates.put(id(holder), climate(holder.value())));
        return new ClimateSnapshot(climates, ids(source.resolvedPools()), ids(source.resolvedExtras()));
    }
    public static ClimateSnapshot.Climate climate(Biome biome) {
        var c = biome.getModifiedClimateSettings();
        return new ClimateSnapshot.Climate(c.temperature(), c.downfall(), c.hasPrecipitation(),
                c.temperatureModifier().getSerializedName());
    }
    public static boolean snowClimate(Biome biome, BlockPos pos) {
        return biome.hasPrecipitation() && biome.coldEnoughToSnow(pos);
    }
    private static String id(Holder<Biome> holder) { return holder.unwrapKey().orElseThrow().location().toString(); }
    private static Map<String, List<String>> ids(Map<String, List<Holder<Biome>>> pools) {
        Map<String, List<String>> result = new TreeMap<>();
        pools.forEach((key, values) -> result.put(key, values.stream().map(BiomeClimateAdapter::id).toList()));
        return result;
    }
}
