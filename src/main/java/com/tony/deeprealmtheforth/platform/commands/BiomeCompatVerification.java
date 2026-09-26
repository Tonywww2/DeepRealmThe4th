package com.tony.deeprealmtheforth.platform.commands;

import com.mojang.brigadier.exceptions.*;
import com.tony.deeprealmtheforth.worldgen.SpiralChunkGenerator;
import com.tony.deeprealmtheforth.platform.worldgen.BiomeClimateAdapter;
import java.util.*;
import java.nio.file.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;

/** Real loaded biome holders, saved quart cells and native feature writes in disposable worlds only. */
public final class BiomeCompatVerification {
    private static final Set<String> TARGETS=Set.of("biomesoplenty:lush_desert","biomesoplenty:redwood_forest",
            "biomesoplenty:tropics","biomesoplenty:dune_beach","terralith:white_mesa",
            "terralith:arid_highlands","terralith:lavender_forest","terralith:gravel_beach");
    private BiomeCompatVerification() {}
    public static int run(CommandSourceStack source,ServerLevel level,SpiralChunkGenerator generator)throws CommandSyntaxException {
        if(!VerificationWorlds.allowed(source,level)||generator.terrain().generationVersion()!=5)
            throw error("Only empty seed-42 V5 verification saves are permitted.");
        try {
            var biomes=generator.spiralBiomes();var possible=new TreeSet<String>();
            biomes.possibleBiomes().forEach(h->possible.add(h.unwrapKey().orElseThrow().location().toString()));
            boolean expectMods=source.getServer().getWorldData().getLevelName().equals("verification-world-vortex-v11-compat");
            var required=new TreeSet<String>();if(expectMods)required.addAll(TARGETS);
            if(!possible.containsAll(required))throw new IllegalStateException("Missing mapped mod biomes: "+difference(required,possible));
            if(!expectMods&&possible.stream().anyMatch(id->id.startsWith("biomesoplenty:")||id.startsWith("terralith:")))
                throw new IllegalStateException("Control world unexpectedly has test biomes");
            Map<String,BlockPos> sites=new TreeMap<>();
            // The two dry upland defaults exercise the new categories even without external mods.
            if(!expectMods){required.add("minecraft:wooded_badlands");required.add("minecraft:windswept_savanna");}
            for(int x=-6142;x<6144&&sites.size()<required.size();x+=48)for(int z=-6142;z<6144;z+=48) {
                var c=generator.terrain().baseTerrain().sample(x,z);
                if(!c.land()||c.edgeDistance()<90||c.wet()||c.shore())continue;
                String id=biomes.baseBiomeAt(x,z).unwrapKey().orElseThrow().location().toString();
                if(!required.contains(id)||sites.containsKey(id))continue;
                var actual=biomes.biomeAt(x,z);
                if(!actual.unwrapKey().orElseThrow().location().toString().equals(id))continue;
                sites.put(id,new BlockPos(x,c.top(),z));
            }
            if(sites.size()!=required.size())throw new IllegalStateException("Unreachable mapped biomes: "+difference(required,sites.keySet()));
            var report=new StringBuilder("BIOME_COMPAT_V5 profile=").append(expectMods?"BOP+Terralith":"vanilla-control")
                    .append(" possibleBiomes=").append(possible.size()).append('\n');
            Map<String,Long> blocks=new TreeMap<>();int quartChecks=0;
            for(var entry:sites.entrySet()) {
                var p=entry.getValue();int cx=p.getX()>>4,cz=p.getZ()>>4;
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)level.getChunk(cx+dx,cz+dz);
                var chunk=level.getChunk(cx,cz);
                var stored=chunk.getNoiseBiome(p.getX()>>2,p.getY()>>2,p.getZ()>>2);
                if(!stored.unwrapKey().orElseThrow().location().toString().equals(entry.getKey()))
                    throw new IllegalStateException("Saved biome differs at "+p);quartChecks++;
                // Climate adapter is checked separately by verifyclimate; record native values here.
                var nativeClimate=BiomeClimateAdapter.climate(stored.value());
                int spawnEntries=0;for(var category:MobCategory.values())spawnEntries+=stored.value().getMobSettings().getMobs(category).unwrap().size();
                report.append(entry.getKey()).append(" at=").append(p.toShortString()).append(" climate=").append(nativeClimate)
                        .append(" nativeSpawnEntries=").append(spawnEntries).append('\n');
                var cursor=new BlockPos.MutableBlockPos();
                for(int x=(cx-1)*16;x<(cx+2)*16;x++)for(int z=(cz-1)*16;z<(cz+2)*16;z++) {
                    var c=generator.terrain().sample(x,z);if(!c.land())continue;
                    for(int y=Math.max(c.bottom(),c.top()-3);y<=Math.min(level.getMaxBuildHeight()-1,c.surface()+45);y++) {
                        var state=level.getBlockState(cursor.set(x,y,z));
                        var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
                        if(!id.getNamespace().equals("minecraft"))blocks.merge(id.toString(),1L,Long::sum);
                    }
                }
            }
            var decoration=generator.decoration();
            var attempts=namespaces(decoration.attemptedFeatures());var successes=namespaces(decoration.placedFeatures());
            report.append("quartChecks=").append(quartChecks).append(" nativeFeatureAttempts=").append(attempts)
                    .append(" nativeReportedSuccesses=").append(successes).append("\nactualNonVanillaBlocks=").append(blocks)
                    .append("\nfailedFeatureIds=").append(new TreeSet<>(decoration.failedFeatures())).append('\n');
            if(expectMods)for(String ns:List.of("biomesoplenty","terralith")) {
                if(attempts.getOrDefault(ns,0L)==0||successes.getOrDefault(ns,0L)==0)
                    throw new IllegalStateException("No native feature execution for "+ns+" "+report);
            }
            if(expectMods&&blocks.isEmpty())throw new IllegalStateException("No actual custom mod blocks found");
            var folder=Path.of("biome-compat-v5");Files.createDirectories(folder);
            Files.writeString(folder.resolve("verification.txt"),report);
            String result="BIOME_COMPAT_OK quartChecks="+quartChecks+" featureNamespaces="+successes.keySet()
                    +" customBlockKinds="+blocks.size()+" failedFeatureIds="+decoration.failedFeatures()+" report="+folder.resolve("verification.txt");
            source.sendSuccess(()->Component.literal(result),false);return quartChecks;
        }catch(Exception e){throw error("BIOME_COMPAT_FAILED "+e);}
    }
    private static Set<String> difference(Set<String> a,Set<String> b){var copy=new TreeSet<>(a);copy.removeAll(b);return copy;}
    private static Map<String,Long> namespaces(Map<String,Long> values) {
        Map<String,Long> result=new TreeMap<>();values.forEach((id,count)->result.merge(id.split(":")[0],count,Long::sum));return result;
    }
    private static CommandSyntaxException error(String text){return new SimpleCommandExceptionType(Component.literal(text)).create();}
}
