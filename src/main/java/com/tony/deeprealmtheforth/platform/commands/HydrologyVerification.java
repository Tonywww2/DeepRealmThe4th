package com.tony.deeprealmtheforth.platform.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tony.deeprealmtheforth.worldgen.SpiralChunkGenerator;
import com.tony.deeprealmtheforth.worldgen.hydrology.GlobalDrainage;
import com.tony.deeprealmtheforth.worldgen.hydrology.WatershedRivers;
import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Natural generation only: no test-scheduled fluid ticks, neighbour updates or placed water. */
public final class HydrologyVerification {
    private record Probe(String name,int x,int z) {}
    private HydrologyVerification() {}
    public static int run(CommandSourceStack source,ServerLevel level,SpiralChunkGenerator generator,String phase)
            throws CommandSyntaxException {
        int version=generator.terrain().generationVersion();
        String world=source.getServer().getWorldData().getLevelName();
        boolean match=version==3&&world.equals("verification-world-vortex-v9-hydrology")
                ||version==4&&world.equals("verification-world-vortex-v10-edges")
                ||version==5&&(world.equals("verification-world-vortex-v11-marine")||world.equals("verification-world-vortex-v11-compat"));
        if(!VerificationWorlds.allowed(source,level)||!match)
            throw error("Hydrology tests require an empty seed-42 disposable save matching its generation version.");
        Path folder=Path.of("hydrology-v"+version),manifestPath=folder.resolve("active-probes.tsv");
        try {
            Files.createDirectories(folder);
            if(phase.equals("prepare")) {
                if(Files.exists(manifestPath))throw new IllegalStateException("An existing probe session must be checked/cleaned first");
                List<Probe> probes=choose(generator);Set<Long> owned=new LinkedHashSet<>();
                for(var p:probes)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
                    long key=ChunkPos.asLong((p.x>>4)+dx,(p.z>>4)+dz);
                    if(!level.getForcedChunks().contains(key))owned.add(key);
                }
                List<String> manifest=new ArrayList<>();manifest.add("T\t"+level.getGameTime());
                probes.forEach(p->manifest.add("P\t"+p.name+"\t"+p.x+"\t"+p.z));
                owned.forEach(k->{var c=new ChunkPos(k);manifest.add("C\t"+c.x+"\t"+c.z);});
                Files.write(manifestPath,manifest); // Recovery information before changing forced chunks.
                for(long k:owned){var c=new ChunkPos(k);level.setChunkForced(c.x,c.z,true);}
                for(var p:probes)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                    level.getChunk((p.x>>4)+dx,(p.z>>4)+dz);
                String result=inspect(level,generator,probes,true);
                return report(source,folder,"prepare", "HYDROLOGY_PREPARED "+result+" probes="+probes);
            }
            if(!Files.exists(manifestPath))throw new IllegalStateException("No active probe manifest");
            List<String> lines=Files.readAllLines(manifestPath);List<Probe> probes=new ArrayList<>();long start=-1;
            for(String line:lines){String[] f=line.split("\t");
                if(f[0].equals("T"))start=Long.parseLong(f[1]);
                if(f[0].equals("P"))probes.add(new Probe(f[1],Integer.parseInt(f[2]),Integer.parseInt(f[3])));
            }
            if(phase.equals("check")) {
                long elapsed=level.getGameTime()-start;
                if(elapsed<200)throw new IllegalStateException("Wait for 200 world ticks; elapsed="+elapsed);
                String result=inspect(level,generator,probes,false);
                return report(source,folder,"check-"+level.getGameTime(),"HYDROLOGY_RUNTIME_OK ticks="+elapsed+" "+result);
            }
            if(phase.equals("cleanup")) {
                int removed=0;
                for(String line:lines){String[] f=line.split("\t");if(f[0].equals("C")) {
                    level.setChunkForced(Integer.parseInt(f[1]),Integer.parseInt(f[2]),false);removed++;
                }}
                Files.move(manifestPath,folder.resolve("completed-probes-"+level.getGameTime()+".tsv"));
                return report(source,folder,"cleanup","HYDROLOGY_CLEANUP_OK ownedForcedChunksReleased="+removed);
            }
            throw new IllegalArgumentException("Expected prepare, check or cleanup");
        } catch(Exception e){throw error("HYDROLOGY_FAILED "+e);}
    }
    private static List<Probe> choose(SpiralChunkGenerator generator) {
        var rivers=generator.terrain().hydrology();WatershedRivers.Model best=null;Set<Long> seen=new HashSet<>();
        for(int x=3360;x<5280;x+=384)for(int z=-3520;z<-1600;z+=384) {
            var m=rivers.model(GlobalDrainage.key(Math.floorDiv(x,64),Math.floorDiv(z,64)));
            if(!seen.add(m.id())||!m.accepted())continue;
            if(best==null||m.reaches().size()>best.reaches().size())best=m;
        }
        if(best==null)throw new IllegalStateException("No accepted verification catchment");
        var model=best;
        var junction=rivers.drainage().basin(model.id()).nodes().stream().filter(n->n.incoming()>1 && n.downstream()!=GlobalDrainage.NONE)
                .max(Comparator.comparingDouble(GlobalDrainage.Flow::discharge)).orElseThrow();
        var graded=model.reaches().stream().max(Comparator.comparingDouble(r->
                r.knots().get(0).water()-r.knots().get(r.knots().size()-1).water())).orElseThrow();
        var middle=graded.knots().get(graded.knots().size()/2);
        return List.of(new Probe("terminal_lake",(int)Math.floor(model.lake().x()),(int)Math.floor(model.lake().z())),
                new Probe("confluence",(int)Math.floor(junction.site().x()),(int)Math.floor(junction.site().z())),
                new Probe("graded_reach",(int)Math.floor(middle.x()),(int)Math.floor(middle.z())));
    }
    private static String inspect(ServerLevel level,SpiralChunkGenerator generator,List<Probe> probes,boolean initial) {
        int columns=0,water=0,ice=0,flowing=0,bed=0;Set<Long> seen=new HashSet<>();
        for(var p:probes) {
            int wetInProbe=0;
            for(int x=p.x-12;x<=p.x+12;x++)for(int z=p.z-12;z<=p.z+12;z++) {
                var c=generator.terrain().sample(x,z);if(c.wet())wetInProbe++;
                if(!seen.add(ChunkPos.asLong(x,z)))continue;
                level.getChunk(x>>4,z>>4);
                var predicted=generator.getBaseColumn(x,z,level,level.getChunkSource().randomState());
                for(int y=c.bottom();y<=c.surface()+1;y++) {
                    var expected=generator.terrainBlock(c,y,x,z);
                    if(!expected.equals(predicted.getBlock(y)))throw new IllegalStateException("Column query mismatch");
                    if(y<c.bottom()+3 && !level.getBlockState(new BlockPos(x,y,z)).equals(expected))
                        throw new IllegalStateException("Protected bottom mismatch at "+x+","+y+","+z);
                }
                int expectedHeight=c.surface()+1;
                int height=generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,level,level.getChunkSource().randomState());
                if(height!=expectedHeight)throw new IllegalStateException("Height query mismatch at "+x+","+z);
                if(c.wet()) {
                    if(!level.getBlockState(new BlockPos(x,c.top(),z)).getFluidState().isEmpty()
                            || level.getBlockState(new BlockPos(x,c.top(),z)).isAir())throw new IllegalStateException("Missing river bed");
                    bed++;
                }
                int actualWater=0;
                for(int y=c.top()+1;y<=c.surface()+9;y++) {
                    BlockPos pos=new BlockPos(x,y,z);var state=level.getBlockState(pos);var fluid=state.getFluidState();
                    if(state.is(Blocks.ICE)){ice++;actualWater++;}
                    if(!fluid.is(FluidTags.WATER))continue;
                    water++;actualWater++;if(!fluid.isSource())flowing++;
                    if(!c.wet())throw new IllegalStateException("Water escaped into a dry column at "+pos);
                    double ceiling=generator.terrain().hydrology().sample(x,z,c.top()).containment();
                    if(y>Math.max(c.fluidLevel(),ceiling)+1)throw new IllegalStateException("Water above bounded channel envelope at "+pos);
                }
                if(c.wet()&&actualWater==0)throw new IllegalStateException("Generated channel dried at "+x+","+z);
                columns++;
            }
            if(wetInProbe==0)throw new IllegalStateException("Probe misses actual water: "+p.name);
        }
        if(!initial&&flowing==0)throw new IllegalStateException("No naturally flowing water after generation");
        return "version="+generator.terrain().generationVersion()+" testScheduledTicks=0 columns="+columns+" bedColumns="+bed+" waterBlocks="+water+" iceBlocks="+ice+" flowingBlocks="+flowing+" leaks=0";
    }
    private static int report(CommandSourceStack source,Path folder,String phase,String text) throws java.io.IOException {
        Files.writeString(folder.resolve(phase+".txt"),text+"\n");
        source.sendSuccess(()->Component.literal(text),false);return 1;
    }
    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }
}
