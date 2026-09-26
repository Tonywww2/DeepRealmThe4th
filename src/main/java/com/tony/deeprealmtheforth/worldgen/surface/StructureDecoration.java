package com.tony.deeprealmtheforth.worldgen.surface;

import com.tony.deeprealmtheforth.worldgen.SpiralChunkGenerator;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Native structure stages, without vanilla's mixed-biome FeatureSorter. No ID whitelist. */
public final class StructureDecoration {
    private final LongAdder calls = new LongAdder(), rejected = new LongAdder(), writes = new LongAdder();
    private final Map<String, LongAdder> placed = new ConcurrentHashMap<>();

    public String report() {
        return "structureChunkPlacements=" + calls.sum() + " structureIds=" + new java.util.TreeSet<>(placed.keySet())
                + " structureAllowedWriteChecks=" + writes.sum() + " structureBlockedWriteChecks=" + rejected.sum();
    }

    public static boolean supported(StructureStart start, TerrainProfile terrain) {
        for (var piece : start.getPieces()) {
            BoundingBox box = piece.getBoundingBox();
            for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                var column = terrain.sample(x, z);
                if (!column.land()) return false;
            }
        }
        return true;
    }

    public static Map<Integer, List<Structure>> stages(WorldGenLevel level) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).stream()
                .collect(Collectors.groupingBy(structure -> structure.step().ordinal()));
    }

    public void decorateStage(WorldGenLevel level, ChunkAccess chunk, SpiralChunkGenerator generator,
            StructureManager manager, int step, List<Structure> structures) {
        if (!manager.shouldGenerateStructures() || structures.isEmpty()) return;
        var pos = chunk.getPos();
        var height = chunk.getHeightAccessorForGeneration();
        BoundingBox writable = new BoundingBox(pos.getMinBlockX(), height.getMinBuildHeight() + 1,
                pos.getMinBlockZ(), pos.getMaxBlockX(), height.getMaxBuildHeight() - 1, pos.getMaxBlockZ());
        var random = new WorldgenRandom(new XoroshiroRandomSource(0));
        long seed = random.setDecorationSeed(level.getSeed(), pos.getMinBlockX(), pos.getMinBlockZ());
        int index = 0;
        for (Structure structure : structures) {
            random.setFeatureSeed(seed, index++, step);
            var starts = manager.startsForStructure(SectionPos.of(pos, level.getMinSection()), structure);
            if (starts.isEmpty()) continue;
            String id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure).toString();
            try (var ignored = new DecorationGuard(generator.terrain(), rejected, writes, level, pos, true)) {
                level.setCurrentlyGenerating(() -> id);
                for (var start : starts) {
                    start.placeInChunk(level, manager, generator, random, writable, pos);
                    calls.increment(); placed.computeIfAbsent(id, key -> new LongAdder()).increment();
                }
            } catch (Exception exception) {
                CrashReport report = CrashReport.forThrowable(exception, "Fourth-layer structure placement");
                report.addCategory("Structure").setDetail("ID", id).setDetail("Chunk", pos.toString());
                throw new ReportedException(report);
            } finally {
                level.setCurrentlyGenerating(null);
            }
        }
    }
}
