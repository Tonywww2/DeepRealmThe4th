package com.tony.deeprealmtheforth.worldgen.surface;

import com.mojang.logging.LogUtils;
import com.tony.deeprealmtheforth.worldgen.SpiralChunkGenerator;
import com.tony.deeprealmtheforth.worldgen.terrain.SeededNoise;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Native biome features, ordered by stage and stable registry key, with biome filters intact.
 * Explicit ordering avoids vanilla's cross-Nether/Overworld FeatureSorter cycle constraint. */
public final class BiomeDecoration {
    private final LongAdder chunks = new LongAdder(), attempts = new LongAdder(), successes = new LongAdder();
    private final LongAdder rejected = new LongAdder(), writes = new LongAdder();
    private final Map<String, LongAdder> placed = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> attempted = new ConcurrentHashMap<>();
    private final Set<String> failures = ConcurrentHashMap.newKeySet();
    private final StructureDecoration structures = new StructureDecoration();

    public Map<String,Long> attemptedFeatures(){return snapshot(attempted);}
    public Map<String,Long> placedFeatures(){return snapshot(placed);}
    public Set<String> failedFeatures(){return Set.copyOf(failures);}
    private static Map<String,Long> snapshot(Map<String,LongAdder> values) {
        Map<String,Long> result=new TreeMap<>();values.forEach((key,value)->result.put(key,value.sum()));return result;
    }

    public String report() {
        return "chunks=" + chunks.sum() + " attempts=" + attempts.sum() + " nativeReportedSuccesses=" + successes.sum()
                + " allowedWriteChecks=" + writes.sum() + " blockedWriteChecks=" + rejected.sum()
                + " failedFeatureIds=" + new TreeSet<>(failures) + " reportedFeatureIds=" + new TreeSet<>(placed.keySet())
                + " " + structures.report();
    }

    public void decorate(WorldGenLevel level, ChunkAccess chunk, SpiralChunkGenerator generator, StructureManager manager) {
        int x = chunk.getPos().getMinBlockX(), z = chunk.getPos().getMinBlockZ();
        Set<Holder<Biome>> biomes = new HashSet<>();
        boolean land = false;
        for (int dx = -16; dx < 32; dx += 4) for (int dz = -16; dz < 32; dz += 4) {
            if (generator.terrain().sample(x + dx + 2, z + dz + 2).land()) land = true;
            // Quart-grid lookup avoids BiomeManager's extra fuzzy halo outside the 3x3
            // BIOMES dependency guaranteed during 1.21's FEATURES chunk step.
            biomes.add(level.getNoiseBiome((x + dx) >> 2, 16, (z + dz) >> 2));
        }
        var structureStages = StructureDecoration.stages(level);
        var registry = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        chunks.increment();
        for (int step = 0; step < GenerationStep.Decoration.values().length; step++) {
            structures.decorateStage(level, chunk, generator, manager, step, structureStages.getOrDefault(step, List.of()));
            if (!land) continue;
            try (DecorationGuard ignored = new DecorationGuard(generator.terrain(), rejected, writes, level, chunk.getPos())) {
                Map<String, PlacedFeature> features = new TreeMap<>();
                for (Holder<Biome> biome : biomes) {
                    var stages = generator.getBiomeGenerationSettings(biome).features();
                    if (step >= stages.size()) continue;
                    int inline = 0;
                    for (Holder<PlacedFeature> holder : stages.get(step)) {
                        var key = registry.getKey(holder.value());
                        String id = key != null ? key.toString() : biome.unwrapKey().orElseThrow().location() + "/inline/" + step + "/" + inline;
                        features.put(id, holder.value()); inline++;
                    }
                }
                for (var entry : features.entrySet()) {
                    attempts.increment();
                    attempted.computeIfAbsent(entry.getKey(),key->new LongAdder()).increment();
                    long seed = SeededNoise.hash(generator.terrain().seed() ^ entry.getKey().hashCode() ^ ((long) step << 32), x, z);
                    try {
                        if (entry.getValue().placeWithBiomeCheck(level, generator, RandomSource.create(seed),
                                new BlockPos(x, level.getMinBuildHeight(), z))) {
                            successes.increment(); placed.computeIfAbsent(entry.getKey(), key -> new LongAdder()).increment();
                        }
                    } catch (RuntimeException exception) {
                        // Third-party features may require their own generator. Report once, don't silently promise support.
                        if (failures.add(entry.getKey())) LogUtils.getLogger().warn(
                                "Fourth-layer feature {} needs compatibility adaptation; chunk {}", entry.getKey(), chunk.getPos(), exception);
                    }
                }
            }
        }
    }
}
