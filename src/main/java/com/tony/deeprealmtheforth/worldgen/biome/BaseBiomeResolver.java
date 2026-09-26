package com.tony.deeprealmtheforth.worldgen.biome;

import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Shared tag selection; callers explicitly supply a base or a final column. */
public final class BaseBiomeResolver {
    private BaseBiomeResolver() {}
    public static <T> T select(BiomeClimate climate, TerrainProfile.Column column, int x, int z,
                               Map<String, List<T>> pools, Map<String, List<T>> extra,
                               Function<T, String> id) {
        var theme = column.theme();
        List<T> entries = pools.getOrDefault(theme.pool(), pools.get(theme.region()));
        List<T> additions = extra.getOrDefault(theme.region(), List.of());
        var point = climate.point(x + .5, z + .5);
        if (!additions.isEmpty() && climate.useExtras(point, theme.region()) && !column.wet() && !column.shore())
            entries = additions;
        return climate.choose(point, entries, id, 190);
    }
}
