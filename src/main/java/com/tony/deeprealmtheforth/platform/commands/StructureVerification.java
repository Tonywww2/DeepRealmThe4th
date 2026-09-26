package com.tony.deeprealmtheforth.platform.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tony.deeprealmtheforth.platform.PlatformIds;
import com.tony.deeprealmtheforth.worldgen.SpiralChunkGenerator;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Loads naturally selected starts; never uses /place, edits terrain, or opens loot. */
public final class StructureVerification {
    private StructureVerification() {}

    public static int run(CommandSourceStack source, ServerLevel level, SpiralChunkGenerator generator, String name)
            throws CommandSyntaxException {
        if (!VerificationWorlds.allowed(source, level))
            throw error("Only the empty seed-42 project verification saves are permitted.");
        if (!Set.of("desert_pyramid", "jungle_pyramid", "shipwreck", "fortress", "village_plains").contains(name))
            throw error("Choose desert_pyramid, jungle_pyramid, shipwreck, fortress, or village_plains.");
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var holder = registry.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, PlatformIds.parse("minecraft:" + name)));
        // Native random-spread radius counts placement regions, NOT individual chunks.
        var found = generator.findNearestMapStructure(level, HolderSet.direct(holder), BlockPos.ZERO, 8, false);
        if (found == null) throw error("STRUCTURE_VERIFY FAILED: no eligible " + name + " within eight placement regions.");
        var startChunk = level.getChunk(found.getFirst());
        var start = startChunk.getAllStarts().get(holder.value());
        if (start == null || !start.isValid()) throw error("STRUCTURE_VERIFY FAILED: missing natural start.");
        BoundingBox bounds = start.getBoundingBox();
        int chunkCount = ((bounds.maxX() >> 4) - (bounds.minX() >> 4) + 1)
                * ((bounds.maxZ() >> 4) - (bounds.minZ() >> 4) + 1);
        if (chunkCount > 1024) throw error("Structure probe exceeds its 1024-chunk budget.");
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++)
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        // Some native pieces align their Y to the actual heightmap during postProcess.
        int landmarks = 0, containers = 0, beds = 0;
        Set<String> lootTables = new TreeSet<>();
        Set<Long> visited = new java.util.HashSet<>();
        for (var piece : start.getPieces()) {
            var box = piece.getBoundingBox();
            // ScatteredFeaturePiece bounds omit the basement: temples write down to local Y=-14.
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), Math.max(level.getMinBuildHeight(), box.minY() - 16), box.minZ(),
                    box.maxX(), Math.min(level.getMaxBuildHeight() - 1, box.maxY()), box.maxZ())) {
                if (!visited.add(pos.asLong())) continue;
                String block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
                if (block.contains("planks") || block.contains("bricks") || block.contains("cut_sandstone")
                        || block.equals("tnt") || block.equals("tripwire_hook") || block.equals("bell")
                        || block.equals("mossy_cobblestone")) landmarks++;
                if (block.endsWith("_bed")) beds++;
                var entity = level.getBlockEntity(pos);
                if (entity == null) continue;
                //? if <1.21 {
                CompoundTag tag = entity.saveWithFullMetadata();
                //?} else {
                /*CompoundTag tag = entity.saveWithFullMetadata(level.registryAccess());
                *///?}
                if (tag.contains("LootTable")) { containers++; lootTables.add(tag.getString("LootTable")); }
            }
        }
        String expectedLoot = switch (name) {
            case "desert_pyramid" -> "chests/desert_pyramid";
            case "jungle_pyramid" -> "chests/jungle_temple";
            case "shipwreck" -> "chests/shipwreck";
            case "fortress" -> "chests/nether_bridge";
            default -> "chests/village/";
        };
        // A valid village may have no chest-bearing house in its random template selection.
        boolean furnished = name.equals("village_plains") ? beds > 0
                : lootTables.stream().anyMatch(id -> id.contains(expectedLoot));
        if (landmarks < 8 || !furnished) throw error("STRUCTURE_VERIFY FAILED: " + name + " start=" + start.getChunkPos()
                + " landmarks=" + landmarks + " lootContainers=" + containers + " beds=" + beds + " lootTables=" + lootTables);
        String report = "STRUCTURE_VERIFY_OK id=minecraft:" + name + " start=" + start.getChunkPos()
                + " pieces=" + start.getPieces().size() + " chunks=" + chunkCount + " landmarks=" + landmarks
                + " lootContainers=" + containers + " beds=" + beds + " lootTables=" + lootTables;
        source.sendSuccess(() -> Component.literal(report), true);
        return 1;
    }

    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }
}
