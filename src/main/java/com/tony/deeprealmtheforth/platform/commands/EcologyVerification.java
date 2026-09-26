package com.tony.deeprealmtheforth.platform.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tony.deeprealmtheforth.worldgen.SpiralChunkGenerator;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.BlockTags;
//? if <1.21 {
import net.minecraftforge.common.util.FakePlayer;
//?} else {
/*import net.neoforged.neoforge.common.util.FakePlayer;
*///?}

/** Explicit, bounded integration test; refuses normal saves and never summons entities.
 * A scoped fake observer enables NaturalSpawner's distance checks, with real biome tables,
 * substrate/light/fluid predicates, collision, finalization, and actual entity insertion.
 * This tests placement, not the long-running mob-cap/tick scheduler. */
public final class EcologyVerification {
    private EcologyVerification() {}

    public static int run(CommandSourceStack source, ServerLevel level, SpiralChunkGenerator generator) throws CommandSyntaxException {
        if (!VerificationWorlds.allowed(source, level)) {
            throw error("仅允许无人在线的 seed=42 项目专用测试存档。");
        }
        Map<String, BlockPos> sites = new TreeMap<>();
        Set<String> wanted = Set.of("plains", "forest", "jungle", "desert", "nether_wastes", "crimson_forest", "warped_forest",
                "basalt_deltas", "soul_sand_valley", "ocean", "warm_ocean", "deep_ocean", "river");
        for (int x = -1536; x <= 1536; x += 24) for (int z = -1536; z <= 1536; z += 24) {
            TerrainProfile.Column c = generator.terrain().sample(x, z);
            if (!c.land() || c.edgeDistance() < 32 || c.shore()) continue;
            String biome = generator.spiralBiomes().biomeAt(x, z).unwrapKey().orElseThrow().location().getPath();
            if (wanted.contains(biome) && (!biome.contains("ocean") || c.wet() && c.top() < 52)) sites.putIfAbsent(biome, new BlockPos(x, c.surface(), z));
        }
        // Custom dimensions use DerivedLevelData: setDayTime there is a no-op.
        ServerLevel clockLevel = source.getServer().overworld();
        long previousTime = clockLevel.getDayTime();
        ServerPlayer observer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "[FourthLayerTest]"));
        Map<String, Integer> spawned = new TreeMap<>();
        try {
            level.players().add(observer);
            for (var site : sites.entrySet()) {
                BlockPos center = site.getValue();
                // Real generated/illuminated chunks, no artificial platforms or changed spawn predicates.
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                    level.getChunk((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz);
                observer.setPos(center.getX() + 30, center.getY() + 2, center.getZ() + 30);
                for (MobCategory category : new MobCategory[]{MobCategory.CREATURE, MobCategory.MONSTER,
                        MobCategory.WATER_CREATURE, MobCategory.WATER_AMBIENT}) {
                    clockLevel.setDayTime(category == MobCategory.MONSTER ? 18000 : 6000);
                    level.updateSkyBrightness();
                    int before = spawned.values().stream().mapToInt(Integer::intValue).sum();
                    for (int attempt = 0; attempt < 96; attempt++) {
                        int x = (center.getX() & ~15) + 3 + attempt % 10;
                        int z = (center.getZ() & ~15) + 3 + (attempt / 10) % 10;
                        TerrainProfile.Column c = generator.terrain().sample(x, z);
                        boolean aquatic = category == MobCategory.WATER_AMBIENT || category == MobCategory.WATER_CREATURE;
                        int y = aquatic && c.wet() ? Math.max(c.top() + 2, c.fluidLevel() - 3)
                                : attempt % 2 == 0 ? c.top() + 1 : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                        BlockPos pos = new BlockPos(x, y, z);
                        NaturalSpawner.spawnCategoryForPosition(category, level, level.getChunk(pos), pos,
                                (type, candidate, chunk) -> true, (mob, chunk) -> {
                                    if (!mob.isRemoved()) {
                                        String id = site.getKey() + "/" + BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
                                        spawned.merge(id, 1, Integer::sum);
                                    }
                                    // Remove only this probe's freshly generated entities, including passengers.
                                    mob.getSelfAndPassengers().toList().forEach(entity -> entity.discard());
                                });
                        if (spawned.values().stream().mapToInt(Integer::intValue).sum() - before >= 6) break;
                    }
                }
            }
        } finally {
            level.players().remove(observer);
            clockLevel.setDayTime(previousTime); level.updateSkyBrightness(); clockLevel.updateSkyBrightness();
        }
        boolean land = spawned.keySet().stream().anyMatch(s -> s.startsWith("plains/") || s.startsWith("forest/") || s.startsWith("jungle/"));
        boolean water = spawned.keySet().stream().anyMatch(s -> s.contains(":cod") || s.contains(":salmon") || s.contains(":squid") || s.contains(":tropical_fish"));
        boolean infernal = spawned.keySet().stream().anyMatch(s -> s.contains(":zombified_piglin") || s.contains(":magma_cube") || s.contains(":hoglin"));
        boolean hostile = spawned.keySet().stream().anyMatch(s -> s.contains(":zombie") || s.contains(":creeper") || s.contains(":skeleton") || s.contains(":spider"));
        Map<String, Integer> blocks = new TreeMap<>();
        for (BlockPos site : sites.values()) {
            int sx = site.getX() & ~15, sz = site.getZ() & ~15;
            BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
            for (int x = sx; x < sx + 16; x++) for (int z = sz; z < sz + 16; z++) {
                var c = generator.terrain().sample(x, z);
                for (int y = c.bottom(); y <= c.surface() + 40; y++) {
                    var state = level.getBlockState(probe.set(x, y, z));
                    String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                    String group = state.is(BlockTags.LOGS) ? "logs" : state.is(BlockTags.LEAVES) ? "leaves"
                            : state.is(BlockTags.FLOWERS) ? "flowers" : id.contains("coral") ? "coral"
                            : id.contains("kelp") || id.contains("seagrass") ? "aquaticPlants"
                            : id.contains("fungus") || id.contains("nylium") || id.contains("wart_block") ? "fungalEcology"
                            : id.contains("_ore") || id.equals("ancient_debris") ? "ores" : null;
                    if (group != null) blocks.merge(group, 1, Integer::sum);
                }
            }
        }
        boolean features = blocks.containsKey("logs") && blocks.containsKey("aquaticPlants") && blocks.containsKey("ores");
        String report = "NATURAL_SPAWN_PROBE " + (land && water && infernal && hostile && features ? "OK" : "FAILED")
                + " sites=" + sites + " spawned=" + spawned + " actualFeatureBlocks=" + blocks + "\n" + generator.decoration().report();
        source.sendSuccess(() -> Component.literal(report), false);
        if (!land || !water || !infernal || !hostile) throw error("Natural spawn probe missing category: land=" + land + " aquatic=" + water + " infernal=" + infernal + " hostile=" + hostile);
        if (!features) throw error("Missing actual generated feature blocks: " + blocks);
        return spawned.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }
}
