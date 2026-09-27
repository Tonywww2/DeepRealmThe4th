package com.tony.deeprealmtheforth.platform.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tony.deeprealmtheforth.platform.worldgen.BiomeClimateAdapter;
import com.tony.deeprealmtheforth.worldgen.SpiralChunkGenerator;
import com.tony.deeprealmtheforth.worldgen.hydrology.ClimateSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Non-mutating world checks; exports only immutable registry inputs, never blocks or climate settings. */
public final class ClimateVerification {
    private ClimateVerification() {}
    public static int run(CommandSourceStack source, ServerLevel level, SpiralChunkGenerator generator)
            throws CommandSyntaxException {
        if (!VerificationWorlds.allowed(source, level))
            throw error("Only the empty seed-42 project verification saves are permitted.");
        try {
            var biomes = generator.spiralBiomes();
            var snapshot = BiomeClimateAdapter.capture(biomes);
            var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, level.registryAccess());
            var codec = com.tony.deeprealmtheforth.worldgen.biome.SpiralBiomeSource.CODEC;
            var encoded = codec.encodeStart(ops, biomes).result().orElseThrow().getAsJsonObject();
            if (codec.parse(ops, encoded).result().orElseThrow().generationVersion() != biomes.generationVersion())
                throw new IllegalStateException("Worldgen version codec roundtrip");
            int checked = 0, snow = 0, warmPeaks = 0;
            for (int x = -2046; x <= 2046; x += 28) for (int z = -2046; z <= 2046; z += 28) {
                var holder = biomes.baseBiomeAt(x, z);
                String id = holder.unwrapKey().orElseThrow().location().toString();
                if (!snapshot.biomeAt(generator.terrain().baseTerrain(), x, z).equals(id))
                    throw new IllegalStateException("Offline and registry base-biome selectors disagree");
                if (!snapshot.climates().get(id).equals(BiomeClimateAdapter.climate(holder.value())))
                    throw new IllegalStateException("Climate snapshot differs from modified registry");
                int y = generator.terrain().baseTerrain().sample(x, z).top();
                boolean cold = BiomeClimateAdapter.snowClimate(holder.value(), new BlockPos(x, y, z));
                if (cold) snow++;
                if (id.equals("minecraft:stony_peaks") && y >= 120 && !cold) warmPeaks++;
                checked++;
            }
            Path folder = Path.of("biome-compat-v5");
            Files.createDirectories(folder);
            Path path = folder.resolve("climate.tsv");
            Files.writeString(path, snapshot.serialize());
            if (!ClimateSnapshot.read(path).serialize().equals(snapshot.serialize()))
                throw new IllegalStateException("Snapshot roundtrip");
            int coldFixture=coldFixture(level,biomes);
            String report = "CLIMATE_VERIFY_OK biomes=" + snapshot.climates().size() + " baseColumns=" + checked
                    + " nativeSnowEligible=" + snow + " nativeWarmStonyPeaksAbove120=" + warmPeaks
                    + " versionCodec=OK isolatedColdFixtureColumns="+coldFixture
                    + " snapshot="+path;
            Files.writeString(folder.resolve("runtime-verification.txt"), report + "\n");
            source.sendSuccess(() -> Component.literal(report), false);
            return checked;
        } catch (Exception e) { throw error("CLIMATE_VERIFY_FAILED " + e); }
    }
    /** Separate in-memory input fixture; does NOT replace live pools, registry climate or blocks. */
    private static int coldFixture(ServerLevel level,com.tony.deeprealmtheforth.worldgen.biome.SpiralBiomeSource live) {
        var cold=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getHolderOrThrow(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS);
        var pools=new java.util.HashMap<String,com.tony.deeprealmtheforth.worldgen.biome.BiomePool>();
        live.resolvedPools().forEach((name,entries)-> {
            var values=name.startsWith("temperate_tropical")?java.util.List.of(cold):entries;
            pools.put(name,new com.tony.deeprealmtheforth.worldgen.biome.BiomePool(net.minecraft.core.HolderSet.direct(values),values.get(0)));
        });
        var fixture=new com.tony.deeprealmtheforth.worldgen.biome.SpiralBiomeSource(live.parameters(),pools,live.generationVersion());
        fixture.bindSeed(level.getSeed());var generator=new SpiralChunkGenerator(fixture);int checked=0;
        for(int x=3234;x<5280;x+=16)for(int z=-3646;z<-1600;z+=16) {
            var c=fixture.terrain().sample(x,z);if(!c.wet()||c.arm()!=5)continue;
            if(!fixture.biomeAt(x,z).is(net.minecraft.world.level.biome.Biomes.FROZEN_RIVER))
                throw new IllegalStateException("Native-cold input did not select frozen river");
            if(!generator.terrainBlock(c,c.fluidLevel(),x,z).is(net.minecraft.world.level.block.Blocks.ICE))
                throw new IllegalStateException("Native-cold river material is not ice");
            if(++checked>=64)return checked;
        }
        throw new IllegalStateException("Insufficient cold fixture water columns: "+checked);
    }
    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }
}
