package com.tony.deeprealmtheforth.worldgen.hydrology;

import com.tony.deeprealmtheforth.worldgen.biome.BaseBiomeResolver;
import com.tony.deeprealmtheforth.worldgen.terrain.BaseTerrain;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

/** Immutable, registry-derived inputs. No biome-name-to-temperature assumptions. */
public final class ClimateSnapshot {
    public static final String FORMAT = "HYDROLOGY_CLIMATE_V1";
    public record Climate(double temperature, double downfall, boolean precipitation, String modifier) {
        public Climate {
            if (!Double.isFinite(temperature) || !Double.isFinite(downfall) || downfall < 0)
                throw new IllegalArgumentException("Invalid registry climate");
            Objects.requireNonNull(modifier);
        }
        public double supply() { return precipitation ? Math.min(1, downfall) : 0; }
        public double evaporation() { return Math.max(0, Math.min(.65, .10 + .16 * temperature)); }
        // Long-term normalized runoff, not millimetres, Celsius, or real-time snow melt.
        public double runoff() { return Math.max(0, supply() * .88 - evaporation()); }
    }
    public record Sample(String biome, double temperature, double supply, double runoff, double evaporation) {}
    private final Map<String, Climate> climates;
    private final Map<String, List<String>> pools, extras;
    private final String canonical;

    public ClimateSnapshot(Map<String, Climate> climates, Map<String, List<String>> pools,
                           Map<String, List<String>> extras) {
        this.climates = Map.copyOf(climates); this.pools = freeze(pools); this.extras = freeze(extras);
        if (!pools.containsKey("void")) throw new IllegalArgumentException("Missing void pool");
        for (var map : List.of(this.pools, this.extras)) for (var list : map.values())
            for (String id : list) if (!climates.containsKey(id)) throw new IllegalArgumentException("Missing climate: " + id);
        StringBuilder b = new StringBuilder(FORMAT + "\n");
        new TreeMap<>(this.climates).forEach((id, c) -> b.append("biome\t").append(id).append('\t')
                .append(c.temperature()).append('\t').append(c.downfall()).append('\t')
                .append(c.precipitation()).append('\t').append(c.modifier()).append('\n'));
        appendPools(b, "pool", this.pools); appendPools(b, "extra", this.extras);
        canonical = b.toString();
    }
    private static Map<String, List<String>> freeze(Map<String, List<String>> source) {
        Map<String, List<String>> result = new TreeMap<>();
        source.forEach((key, value) -> result.put(key, value.stream().distinct().sorted().toList()));
        return Map.copyOf(result);
    }
    private static void appendPools(StringBuilder b, String kind, Map<String, List<String>> map) {
        new TreeMap<>(map).forEach((key, value) -> b.append(kind).append('\t').append(key).append('\t')
                .append(String.join(",", value)).append('\n'));
    }
    public String serialize() { return canonical; }
    public Map<String, Climate> climates() { return climates; }
    public String biomeAt(BaseTerrain base, int x, int z) {
        return BaseBiomeResolver.select(base.biomes(), base.sample(x, z), x, z, pools, extras, Function.identity());
    }
    public Sample sample(BaseTerrain base, int x, int z) {
        String id = biomeAt(base, x, z);
        double temperature = 0, supply = 0, runoff = 0, evaporation = 0;
        // A finite 96-block climate footprint; does not mutate Minecraft's actual temperature.
        for (int[] p : new int[][]{{0,0,4}, {-48,0,1}, {48,0,1}, {0,-48,1}, {0,48,1}}) {
            Climate c = climates.get(biomeAt(base, x + p[0], z + p[1]));
            double weight = p[2] / 8.0;
            temperature += c.temperature() * weight; supply += c.supply() * weight;
            runoff += c.runoff() * weight; evaporation += c.evaporation() * weight;
        }
        return new Sample(id, temperature, supply, runoff, evaporation);
    }
    public static ClimateSnapshot read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty() || !lines.get(0).equals(FORMAT)) throw new IOException("Not a registry climate snapshot");
        Map<String, Climate> climates = new HashMap<>();
        Map<String, List<String>> pools = new HashMap<>(), extras = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) continue;
            String[] p = line.split("\t", -1);
            switch (p[0]) {
                case "biome" -> climates.put(p[1], new Climate(Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                        Boolean.parseBoolean(p[4]), p[5]));
                case "pool", "extra" -> (p[0].equals("pool") ? pools : extras).put(p[1],
                        p[2].isEmpty() ? List.of() : List.of(p[2].split(",")));
                default -> throw new IOException("Unknown climate record " + p[0]);
            }
        }
        return new ClimateSnapshot(climates, pools, extras);
    }
}
