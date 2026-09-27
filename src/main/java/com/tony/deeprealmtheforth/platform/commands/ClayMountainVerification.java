package com.tony.deeprealmtheforth.platform.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tony.deeprealmtheforth.worldgen.SpiralChunkGenerator;
import com.tony.deeprealmtheforth.worldgen.surface.TerrainMaterials;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Read/probe actual generated columns, without placing test blocks or taking forced-chunk ownership. */
public final class ClayMountainVerification {
    private ClayMountainVerification() {}

    public static int run(CommandSourceStack source,ServerLevel level,SpiralChunkGenerator generator)
            throws CommandSyntaxException {
        // --world overrides the save directory, not necessarily LevelData's display name.
        var save=source.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize();
        if (!save.getFileName().toString().equals("verification-world-vortex-v12-clay")
                ||level.getSeed()!=42||!source.getServer().getPlayerList().getPlayers().isEmpty()
                ||generator.terrain().generationVersion()!=5)
            throw error("Only the empty seed-42 V12 clay verification save with generation 5 is permitted.");
        try {
            int checks=verifyMaterialGuards();
            var sites=new TreeMap<String,BlockPos>();
            for(int x=-6142;x<6144&&sites.size()<5;x+=32)for(int z=-6142;z<6144;z+=32) {
                var base=generator.terrain().baseTerrain().sample(x,z);
                if(!base.land()||base.arm()!=1||base.edgeDistance()<90)continue;
                String biome=generator.spiralBiomes().baseBiomeAt(x,z).unwrapKey().orElseThrow().location().toString();
                String kind=switch(base.theme()) {
                    case ARID_MOUNTAIN -> biome.equals("minecraft:eroded_badlands")?"clay_ridge"
                            :biome.equals("minecraft:windswept_savanna")?"dry_ridge":null;
                    case PLATEAU -> biome.equals("minecraft:badlands")?"clay_mesa":null;
                    case BADLANDS -> biome.equals("minecraft:badlands")?"clay_hills":null;
                    case DESERT -> biome.equals("minecraft:desert")?"desert":null;
                    default -> null;
                };
                if(kind==null||sites.containsKey(kind))continue;
                var c=generator.terrain().sample(x,z);
                if(c.wet()||c.shore()||c.theme()!=base.theme()||c.riverDistance()<16||c.top()-c.bottom()<32)continue;
                if(!generator.spiralBiomes().biomeAt(x,z).unwrapKey().orElseThrow().location().toString().equals(biome))continue;
                sites.put(kind,new BlockPos(x,c.top(),z));
            }
            require(sites.size()==5,"Missing clay/desert control sites: "+sites.keySet());
            var report=new StringBuilder("CLAY_MOUNTAIN_V5 seed=42\n");
            for(var entry:sites.entrySet()) {
                var p=entry.getValue();int x=p.getX(),z=p.getZ();
                var c=generator.terrain().sample(x,z);
                var chunk=level.getChunk(x>>4,z>>4);
                String expectedBiome=generator.spiralBiomes().biomeAt(x,z).unwrapKey().orElseThrow().location().toString();
                require(chunk.getNoiseBiome(x>>2,c.top()>>2,z>>2).unwrapKey().orElseThrow().location().toString().equals(expectedBiome),
                        "Saved biome mismatch at "+p);checks++;
                var query=generator.getBaseColumn(x,z,level,level.getChunkSource().randomState());
                int clay=0,matches=0;var actualBlocks=new TreeSet<String>();
                for(int depth=4;depth<=28;depth++) {
                    int y=c.top()-depth;var expected=generator.terrainBlock(c,y,x,z);
                    require(query.getBlock(y).equals(expected),"Base column mismatch at "+p);checks++;
                    var actual=chunk.getBlockState(new BlockPos(x,y,z));
                    if(actual.equals(expected))matches++;
                    if(actual.is(Blocks.TERRACOTTA)||actual.is(Blocks.ORANGE_TERRACOTTA)||actual.is(Blocks.YELLOW_TERRACOTTA))clay++;
                    actualBlocks.add(BuiltInRegistries.BLOCK.getKey(actual.getBlock()).toString());
                }
                boolean claySite=entry.getKey().startsWith("clay_");
                require(matches>=23,"Generated material/query mismatch at "+p+": "+matches);checks++;
                require(claySite?clay>=23&&actualBlocks.size()>=3:clay==0,"Clay material/control mismatch at "+p);checks++;
                var top=generator.terrainBlock(c,c.top(),x,z);
                require(top.is(claySite?Blocks.RED_SAND:entry.getKey().equals("desert")?Blocks.SAND:Blocks.COARSE_DIRT),
                        "Wrong base surface at "+p);checks++;
                require(generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,level,level.getChunkSource().randomState())==c.top()+1,
                        "Changed base height at "+p);checks++;
                report.append(entry.getKey()).append(" biome=").append(expectedBiome).append(" at=").append(p.toShortString())
                        .append(" clayBlocks=").append(clay).append(" matchedBaseBlocks=").append(matches)
                        .append(" actualMaterials=").append(actualBlocks).append('\n');
            }
            report.append("checks=").append(checks).append(" forcedChunksAdded=0 placedTestBlocks=0\n");
            Path folder=Path.of("clay-mountains-v5");Files.createDirectories(folder);
            Files.writeString(folder.resolve("verification.txt"),report);
            final int total=checks;
            source.sendSuccess(()->Component.literal("CLAY_MOUNTAIN_OK sites="+sites.size()+" checks="+total
                    +" report="+folder.resolve("verification.txt")),false);
            return checks;
        }catch(Exception e){throw error("CLAY_MOUNTAIN_FAILED "+e);}
    }

    private static int verifyMaterialGuards() {
        int checks=0;
        for(var theme:TerrainProfile.Theme.values())
            for(int variant=0;variant<4;variant++) {
                var c=new TerrainProfile.Column(theme==TerrainProfile.Theme.COAST?7:1,100,40,variant==1?104:-64,
                        variant==1?TerrainProfile.Fluid.WATER:TerrainProfile.Fluid.NONE,theme,0,100,
                        variant==2,variant==3?0:Double.POSITIVE_INFINITY,0);
                for(int y=39;y<=105;y++) {
                    var old=TerrainMaterials.at(c,y,-18,34,42);
                    var now=TerrainMaterials.at(c,y,-18,34,42,true);
                    boolean allowed=TerrainMaterials.supportsClayMountain(c)&&y>=c.bottom()&&y<=c.top();
                    if(!allowed||variant==3&&y>=c.top()-2)require(now.equals(old),"Material guard changed "+theme+" / "+variant);
                    checks++;
                }
            }
        return checks;
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static CommandSyntaxException error(String message){return new SimpleCommandExceptionType(Component.literal(message)).create();}
}
