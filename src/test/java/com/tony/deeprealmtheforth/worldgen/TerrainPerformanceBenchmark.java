package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.hydrology.ClimateSnapshot;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.management.ManagementFactory;
import jdk.jfr.*;

/** Fixed-workload microbenchmark, not a claim about client FPS or complete native chunk generation. */
public final class TerrainPerformanceBenchmark {
    private static volatile long sink;
    private record Result(String scenario,int repeat,int columns,double millis,double allocatedMiB,long hash) {}
    private interface Sampler { TerrainProfile.Column sample(int x,int z); }
    private static final int[][] DOMAINS={{5088,-3968},{544,-9888},{-5312,1568},{-2048,1024}};
    public static void main(String[] args)throws Exception {
        var climate=ClimateSnapshot.read(Path.of(args[0]));Path out=Path.of(args[1]);Files.createDirectories(out);
        String label=args[2];int version=args.length>3?Integer.parseInt(args[3]):3;List<Result> results=new ArrayList<>();
        try(var recording=new Recording(Configuration.getConfiguration("profile"))) {
            recording.setDestination(out.resolve(label+".jfr"));recording.start();
            // Warm up the same code paths on a disposable profile before timed repetitions.
            long warmup=System.nanoTime();
            do {var t=profile(climate,version);scan(t::sample,DOMAINS,48);} while(System.nanoTime()-warmup<4_000_000_000L);
            for(int repeat=0;repeat<5;repeat++) {
                var h=version==3?BaseTerrain.naturalWorld(42,SpiralParameters.DEFAULT):BaseTerrain.naturalWorld(42,SpiralParameters.DEFAULT,version);
                results.add(measure("H0",repeat,h::sample,DOMAINS,128));
                var t=profile(climate,version);
                results.add(measure("columns-cold",repeat,t::sample,DOMAINS,128));
                results.add(measure("columns-repeat",repeat,t::sample,DOMAINS,128));
                var parallel=profile(climate,version);
                ExecutorService pool=Executors.newFixedThreadPool(4);long start=System.nanoTime();
                try {
                    List<Future<Long>> tasks=new ArrayList<>();
                    for(var domain:DOMAINS)tasks.add(pool.submit(()->scan(parallel::sample,new int[][]{domain},128)));
                    long hash=1;for(var task:tasks)hash=hash*31+task.get();sink=hash;
                    results.add(new Result("columns-parallel4",repeat,65536,(System.nanoTime()-start)/1e6,-1,hash));
                } finally {pool.shutdownNow();}
                System.out.println("PERF repeat="+repeat+" "+results.subList(results.size()-4,results.size()));
            }
            recording.stop();
        }
        StringBuilder text=new StringBuilder("scenario\trepeat\tcolumns\tms\tmainThreadAllocatedMiB\tfingerprint\n");
        for(var r:results)text.append(r.scenario).append('\t').append(r.repeat).append('\t').append(r.columns).append('\t')
                .append(r.millis).append('\t').append(r.allocatedMiB).append('\t').append(Long.toUnsignedString(r.hash,16)).append('\n');
        Files.writeString(out.resolve(label+".tsv"),text);
        System.out.println("PERF_DONE "+label+" version="+version+" jvm="+System.getProperty("java.version")+" seed=42 span=128 domains="+Arrays.deepToString(DOMAINS));
    }
    private static TerrainProfile profile(ClimateSnapshot climate,int version) {
        // The version-3 branch also runs against the untouched 0.1.6 binary for paired A/B tests.
        return version==3?new TerrainProfile(42,SpiralParameters.DEFAULT,climate):new TerrainProfile(42,SpiralParameters.DEFAULT,climate,version);
    }
    private static Result measure(String name,int repeat,Sampler sampler,int[][] domains,int span) {
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        long before=bean.getThreadAllocatedBytes(Thread.currentThread().getId()),start=System.nanoTime();
        long hash=scan(sampler,domains,span);sink=hash;
        return new Result(name,repeat,span*span*domains.length,(System.nanoTime()-start)/1e6,
                (bean.getThreadAllocatedBytes(Thread.currentThread().getId())-before)/1048576.0,hash);
    }
    private static long scan(Sampler sample,int[][] domains,int span) {
        long hash=1;
        for(var p:domains)for(int cx=0;cx<span;cx+=16)for(int cz=0;cz<span;cz+=16)
            for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
                var c=sample.sample(p[0]+cx+x,p[1]+cz+z);
                hash=hash*31+c.arm();hash=hash*31+c.top();hash=hash*31+c.bottom();hash=hash*31+c.fluidLevel();
                hash=hash*31+c.fluid().ordinal();hash=hash*31+c.theme().ordinal();hash=hash*31+c.cell();
                hash=hash*31+Double.doubleToLongBits(c.edgeDistance());hash=hash*31+(c.shore()?1:0);
                hash=hash*31+Double.doubleToLongBits(c.riverDistance());hash=hash*31+Double.doubleToLongBits(c.beach());
            }
        return hash;
    }
}
