package com.tony.deeprealmtheforth.platform.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tony.deeprealmtheforth.worldgen.SpiralChunkGenerator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Checks the real biome registry, query paths and stored quart cells on a disposable save. */
public final class BiomeVerification {
    private BiomeVerification() {}

    public static int run(CommandSourceStack source, ServerLevel level, SpiralChunkGenerator generator) throws CommandSyntaxException {
        if (!VerificationWorlds.allowed(source, level))
            throw error("Only the empty seed-42 project verification saves are permitted.");
        var biomes = generator.spiralBiomes();
        Map<String, BlockPos> found = new TreeMap<>();
        Map<String, Integer> counts = new TreeMap<>();
        for (int x = -2046; x <= 2046; x += 28) for (int z = -2046; z <= 2046; z += 28) {
            var expected = biomes.biomeAt(x, z);
            String id = expected.unwrapKey().orElseThrow().location().toString();
            int y = generator.terrain().sample(x, z).surface();
            found.putIfAbsent(id, new BlockPos(x, y, z));
            counts.merge(id, 1, Integer::sum);
            for (int qy : new int[]{-16, 16, 96}) {
                if (!expected.equals(biomes.getNoiseBiome(QuartPos.fromBlock(x), qy, QuartPos.fromBlock(z),
                        level.getChunkSource().randomState().sampler()))) throw error("BIOME_VERIFY FAILED: noise biome sampling differs");
            }
        }
        var missing = new TreeSet<String>();
        var conditionalNotObserved = new TreeSet<String>();
        int coldCandidates=0, coldWetCandidates=0;
        biomes.possibleBiomes().forEach(b -> missing.add(b.unwrapKey().orElseThrow().location().toString()));
        missing.removeAll(found.keySet());
        // A narrow cold river cannot be proved unreachable by a 28-block local grid.
        // Search native-cold H0 sites first, then query the actual river overlay.
        if (missing.contains("minecraft:frozen_river")) {
            searchCold:
            for (int radius : new int[]{4096, 8192, 12288, 16384, 24576}) {
                double[] center = generator.terrain().layout().armCenter(5, radius);
                int cx = Math.floorDiv((int)center[0], 4) * 4 + 2, cz = Math.floorDiv((int)center[1], 4) * 4 + 2;
                for (int dx = -1536; dx <= 1536; dx += 12) for (int dz = -1536; dz <= 1536; dz += 12) {
                    int x=cx+dx, z=cz+dz;var h=generator.terrain().baseTerrain().sample(x,z);
                    if (!h.land() || h.arm()!=5) continue;
                    if (!com.tony.deeprealmtheforth.platform.worldgen.BiomeClimateAdapter.snowClimate(
                            biomes.baseBiomeAt(x,z).value(),new BlockPos(x,h.top(),z))) continue;
                    coldCandidates++;
                    var c=generator.terrain().sample(x,z);
                    if (!c.wet()) continue;
                    if (!com.tony.deeprealmtheforth.platform.worldgen.BiomeClimateAdapter.snowClimate(
                            biomes.baseBiomeAt(x,z).value(),new BlockPos(x,c.fluidLevel(),z))) continue;
                    coldWetCandidates++;
                    String id=biomes.biomeAt(x,z).unwrapKey().orElseThrow().location().toString();
                    if (id.equals("minecraft:frozen_river")) {
                        found.put(id,new BlockPos(x,c.surface(),z));counts.merge(id,1,Integer::sum);missing.remove(id);
                        break searchCold;
                    }
                    throw error("BIOME_VERIFY FAILED: a native-cold wet river was not converted to frozen_river");
                }
            }
            if (missing.contains("minecraft:frozen_river") && coldWetCandidates==0) {
                // Conditional overlay, not a required base-biome pool. Report the
                // coverage gap explicitly; ClimateVerification tests its cold input branch.
                boolean inBasePool=biomes.resolvedPools().entrySet().stream().filter(e->!e.getKey().startsWith("hydrology/"))
                        .anyMatch(e->e.getValue().stream().anyMatch(b->b.unwrapKey().orElseThrow().location().toString().equals("minecraft:frozen_river")));
                if(!inBasePool){missing.remove("minecraft:frozen_river");conditionalNotObserved.add("minecraft:frozen_river");}
            }
        }
        if (!missing.isEmpty()) throw error("BIOME_VERIFY FAILED: unreachable configured biomes " + missing);
        int stored = 0;
        for (var site : found.entrySet()) {
            BlockPos pos = site.getValue();
            var chunk = level.getChunk(pos);
            for (int qy : new int[]{-16, 16, 96}) {
                String actual = chunk.getNoiseBiome(QuartPos.fromBlock(pos.getX()), qy, QuartPos.fromBlock(pos.getZ()))
                        .unwrapKey().orElseThrow().location().toString();
                if (!actual.equals(site.getKey())) throw error("BIOME_VERIFY FAILED: stored quart biome at " + pos + " actual=" + actual);
                stored++;
            }
        }
        int checked = stored;
        String coverage=" conditionalNotObserved="+conditionalNotObserved+" nativeColdCandidates="+coldCandidates+" coldWetCandidates="+coldWetCandidates;
        source.sendSuccess(() -> Component.literal("BIOME_VERIFY_OK reachable=" + found.size() + " storedQuartChecks=" + checked
                + coverage + " counts=" + counts + " sites=" + found), false);
        return stored;
    }

    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }
}
