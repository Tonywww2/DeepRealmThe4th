package com.tonywww.deeprealm4th.worldgen;

import com.tonywww.deeprealm4th.worldgen.hydrology.ClimateSnapshot;
import com.tonywww.deeprealm4th.worldgen.layout.SpiralParameters;
import com.tonywww.deeprealm4th.worldgen.terrain.TerrainProfile;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Fixed work, including cold caches; not a client FPS or whole-chunk benchmark. */
public final class WorldgenPerformanceBenchmark {
    private static final int[][] ORIGINS={{-6208,4800},{-2560,-1792},{3968,-2944},{-1664,640}};
    private static volatile long sink;
    public static void main(String[] args)throws Exception {
        var climate=ClimateSnapshot.read(Path.of(args[0]));var out=Path.of(args[1]);Files.createDirectories(out);
        int repeats=args.length>2?Integer.parseInt(args[2]):3;
        // Warm the JIT without warming measured model instances.
        run(new TerrainProfile(42,SpiralParameters.DEFAULT,climate),ORIGINS[2],4);
        var report=new StringBuilder("WORLDGEN_FIXED_WORK seed=42 patch=8x8_chunks repetitions="+repeats+"\n");
        for(int iteration=0;iteration<repeats;iteration++) {
            var terrain=new TerrainProfile(42,SpiralParameters.DEFAULT,climate);
            for(int site=0;site<ORIGINS.length;site++) {
                long start=System.nanoTime();long fingerprint=run(terrain,ORIGINS[site],8);
                String row="serial iteration="+iteration+" site="+site+" ms="+(System.nanoTime()-start)/1e6+" fingerprint="+Long.toUnsignedString(fingerprint,16);
                report.append(row).append('\n');System.out.println(row);
            }
        }
        var terrain=new TerrainProfile(42,SpiralParameters.DEFAULT,climate);var workers=Executors.newFixedThreadPool(4);
        try {
            long start=System.nanoTime();List<Future<Long>> results=new ArrayList<>();
            for(var origin:ORIGINS)results.add(workers.submit(()->run(terrain,origin,8)));
            for(var result:results)sink^=result.get();
            report.append("parallel4 ms=").append((System.nanoTime()-start)/1e6).append(" fingerprint=").append(Long.toUnsignedString(sink,16)).append('\n');
        }finally{workers.shutdownNow();}
        Files.writeString(out.resolve("timing.txt"),report);System.out.print(report);
    }
    private static long run(TerrainProfile terrain,int[] origin,int chunks) {
        long fingerprint=0xcbf29ce484222325L;
        for(int cx=0;cx<chunks;cx++)for(int cz=0;cz<chunks;cz++)for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
            var c=terrain.sample(origin[0]+cx*16+x,origin[1]+cz*16+z);
            for(long v:new long[]{c.arm(),c.top(),c.bottom(),c.fluidLevel(),c.fluid().ordinal(),c.theme().ordinal(),
                    Double.doubleToLongBits(c.edgeDistance()),c.shore()?1:0,Double.doubleToLongBits(c.riverDistance()),c.cell()})
                fingerprint=(fingerprint^v)*0x100000001b3L;
        }
        return fingerprint;
    }
}
