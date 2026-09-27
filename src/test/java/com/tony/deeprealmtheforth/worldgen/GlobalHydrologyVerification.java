package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.hydrology.*;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.BaseTerrain;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import java.nio.file.*;
import java.util.*;

/** Checks the current watershed and generated river columns against captured registry climate. */
public final class GlobalHydrologyVerification {
    private static int checks;
    private static void require(boolean ok,String text) { checks++;if(!ok)throw new AssertionError(text); }
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[1]);Files.createDirectories(out);
        var climate=ClimateSnapshot.read(Path.of(args[0]));
        var base=new BaseTerrain(42,SpiralParameters.DEFAULT);
        var graph=new GlobalDrainage(42,base,climate);
        Set<Long> visited=new HashSet<>();StringBuilder report=new StringBuilder("GLOBAL_DRAINAGE_CURRENT\n");
        List<GlobalDrainage.Basin> basins=new ArrayList<>(); long begin=System.nanoTime();
        for(int[] domain:new int[][]{{3232,-3648},{544,-9888},{-5312,1568}}) {
            int count=0,wet=0,maxNodes=0,maxOrder=0,rejected=0;
            for(int x=domain[0]+128;x<domain[0]+2048;x+=384)for(int z=domain[1]+128;z<domain[1]+2048;z+=384) {
                long key=GlobalDrainage.key(Math.floorDiv(x,GlobalDrainage.STEP),Math.floorDiv(z,GlobalDrainage.STEP));
                var basin=graph.basin(key);if(!visited.add(basin.id()))continue;
                count++;if(!basin.complete()){rejected++;continue;}
                verify(graph,basin);basins.add(basin);
                wet+=basin.nodes().stream().filter(GlobalDrainage.Flow::wet).count();
                maxNodes=Math.max(maxNodes,basin.nodes().size());
                maxOrder=Math.max(maxOrder,basin.nodes().stream().mapToInt(GlobalDrainage.Flow::order).max().orElse(0));
            }
            String line="origin="+Arrays.toString(domain)+" basins="+count+" wetNodes="+wet+" largest="+maxNodes+" order="+maxOrder+" rejected="+rejected;
            report.append(line).append('\n');System.out.println(line);
        }
        require(basins.stream().anyMatch(b->b.nodes().stream().anyMatch(n->n.incoming()>1)),"Real tributary junctions");
        var rivers=new WatershedRivers(42,base,climate);Map<String,Integer> status=new TreeMap<>();
        int acceptedReaches=0;List<WatershedRivers.Model> models=new ArrayList<>();
        for(var basin:basins) {
            var model=rivers.model(basin.id());status.merge(model.status(),1,Integer::sum);
            if(model.accepted()) {
                models.add(model);
                acceptedReaches+=model.reaches().size();var lake=model.lake();
                require(Math.abs(lake.inflow()-lake.evaporation()-lake.seepage())<1e-5,"Terminal water balance");
                require(lake.seepage()<lake.capacity()*lake.area(),"Finite groundwater capacity");
                for(var reach:model.reaches())for(int i=1;i<reach.knots().size();i++)
                    require(reach.knots().get(i).water()<=reach.knots().get(i-1).water()+1e-8,"Monotone refined reach");
            }
        }
        report.append("geometry=").append(status).append(" acceptedReaches=").append(acceptedReaches).append('\n');
        System.out.println("geometry="+status+" acceptedReaches="+acceptedReaches);
        require(acceptedReaches>10,"World integration must retain meaningful river networks");
        verifyCrossings(models);
        var terrain=new TerrainProfile(42,SpiralParameters.DEFAULT,climate);
        int waterColumns=0,escaped=0,maxCut=0,maxRaise=0;
        for(int x=3232;x<5280;x+=4)for(int z=-3648;z<-1600;z+=4) {
            var c=terrain.sample(x,z);var h=terrain.baseTerrain().sample(x,z);
            require(c.land()==h.land(),"Integrated land mask unchanged");
            if(!c.land())continue;
            maxCut=Math.max(maxCut,h.top()-c.top());maxRaise=Math.max(maxRaise,c.top()-h.top());
            if(c.wet()) {
                waterColumns++;
                require(c.bottom()==h.bottom()&&c.top()>=c.bottom()+3,"Protected original bottom under river");
                for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
                    var n=terrain.sample(x+d[0],z+d[1]);
                    if(!n.wet()&&(!n.land()||n.top()<c.fluidLevel()))escaped++;
                }
            }
        }
        report.append("columns wet=").append(waterColumns).append(" leaks=").append(escaped).append(" maxCut=").append(maxCut).append(" maxRaise=").append(maxRaise).append('\n');
        System.out.println("columns wet="+waterColumns+" leaks="+escaped+" maxCut="+maxCut+" maxRaise="+maxRaise);
        require(waterColumns>100,"Actual river columns exist");require(escaped==0,"No immediate lateral water leak");
        require(maxCut<=WatershedRivers.MAX_CUT+2,"Bounded incision in final columns");
        require(maxRaise<=4,"No tall artificial bank levees");
        verifyWaterConnectivity(terrain,models,report);
        verifyOrdering(climate,report);
        Collections.reverse(basins);graph.clearCaches();
        for(var basin:basins.subList(0,Math.min(12,basins.size())))require(basin.equals(graph.basin(basin.id())),"Complete basin rebuild independent of window/order");
        report.append("checks=").append(checks).append(" ms=").append((System.nanoTime()-begin)/1e6).append('\n');
        Files.writeString(out.resolve("global-verification.txt"),report.toString());System.out.println(report);
    }
    private static void verify(GlobalDrainage graph,GlobalDrainage.Basin basin) {
        Map<Long,GlobalDrainage.Flow> byId=new HashMap<>();for(var n:basin.nodes())byId.put(n.site().id(),n);
        require(Math.abs(basin.supply()-basin.loss()-basin.root().discharge())<1e-5,"Whole catchment water accounting");
        for(var n:basin.nodes()) {
            require(graph.root(n.site().id())==basin.id(),"Unique global sink");
            if(n.downstream()!=GlobalDrainage.NONE) {
                require(byId.containsKey(n.downstream()),"No truncated downstream");
                require(n.site().rank()>byId.get(n.downstream()).site().rank(),"Strict global potential");
            }
            for(long k:graph.neighbors(n.site().id())) {
                require(Arrays.stream(graph.neighbors(k)).anyMatch(id->id==n.site().id()),"Symmetric global empty-circle edge");
                if(graph.parent(k)==n.site().id())require(byId.containsKey(k),"No omitted upstream catchment");
            }
        }
    }
    private static void verifyCrossings(List<WatershedRivers.Model> models) {
        var reaches=models.stream().flatMap(m->m.reaches().stream()).toList();
        for(int i=0;i<reaches.size();i++)for(int j=i+1;j<reaches.size();j++) {
            var a=reaches.get(i);var b=reaches.get(j);
            if(a.id()==b.id()||a.downstream()==b.id()||b.downstream()==a.id()||a.downstream()==b.downstream())continue;
            if(a.maxX()<b.minX()||b.maxX()<a.minX()||a.maxZ()<b.minZ()||b.maxZ()<a.minZ())continue;
            for(int m=1;m<a.knots().size();m++)for(int n=1;n<b.knots().size();n++) {
                var aa=a.knots().get(m-1);var ab=a.knots().get(m);var ba=b.knots().get(n-1);var bb=b.knots().get(n);
                require(!java.awt.geom.Line2D.linesIntersect(aa.x(),aa.z(),ab.x(),ab.z(),ba.x(),ba.z(),bb.x(),bb.z()),
                        "Nonincident refined channels must not cross: "+a.id()+" / "+b.id());
            }
        }
        for(int i=0;i<models.size();i++)for(int j=i+1;j<models.size();j++) {
            var a=models.get(i);var b=models.get(j);var la=a.lake().mask();var lb=b.lake().mask();
            int x0=(int)Math.max(la.minX(),lb.minX()),x1=(int)Math.min(la.maxX(),lb.maxX());
            int z0=(int)Math.max(la.minZ(),lb.minZ()),z1=(int)Math.min(la.maxZ(),lb.maxZ());
            for(int x=x0;x<=x1;x+=2)for(int z=z0;z<=z1;z+=2)
                require(!(la.distance(x+.5,z+.5)<0&&lb.distance(x+.5,z+.5)<0),"Separate terminal lakes overlap");
        }
    }
    private static void verifyOrdering(ClimateSnapshot climate,StringBuilder report) throws Exception {
        for(long seed:new long[]{0,42,-739221}) {
            var serial=new TerrainProfile(seed,SpiralParameters.DEFAULT,climate);
            var concurrent=new TerrainProfile(seed,SpiralParameters.DEFAULT,climate);
            var center=serial.layout().armCenter(5,4096);
            List<int[]> points=new ArrayList<>();
            for(int dx=-192;dx<=192;dx+=32)for(int dz=-192;dz<=192;dz+=32)
                for(int offset:new int[]{-1,0,1})points.add(new int[]{Math.floorDiv((int)center[0]+dx,64)*64+offset,(int)center[1]+dz});
            Map<Long,TerrainProfile.Column> expected=new HashMap<>();
            for(int[] p:points)expected.put(GlobalDrainage.key(p[0],p[1]),serial.sample(p[0],p[1]));
            Collections.reverse(points);
            var pool=java.util.concurrent.Executors.newFixedThreadPool(4);
            try {
                List<java.util.concurrent.Future<TerrainProfile.Column>> futures=new ArrayList<>();
                for(int[] p:points)futures.add(pool.submit(()->concurrent.sample(p[0],p[1])));
                for(int i=0;i<points.size();i++) {
                    int[] p=points.get(i);
                    require(expected.get(GlobalDrainage.key(p[0],p[1])).equals(futures.get(i).get()),"Concurrent final columns / tile boundaries");
                }
            } finally {pool.shutdownNow();}
            var graph=serial.hydrology().drainage();
            long site=GlobalDrainage.key(Math.floorDiv((int)center[0],64),Math.floorDiv((int)center[1],64));
            var basin=graph.basin(site);verify(graph,basin);serial.hydrology().clearCaches();
            require(basin.equals(graph.basin(site)),"Cache eviction preserves full catchment across seeds");
            String line="seed="+seed+" concurrentColumns="+points.size()+" completeBasinNodes="+basin.nodes().size();
            report.append(line).append('\n');System.out.println(line);
        }
    }
    private static void verifyWaterConnectivity(TerrainProfile terrain,List<WatershedRivers.Model> models,StringBuilder report) {
        var selected=models.stream().sorted(Comparator.comparingInt((WatershedRivers.Model m)->m.reaches().size()).reversed()).limit(3).toList();
        int total=0;
        for(var model:selected) {
            Set<Long> visited=new HashSet<>(),connected=new HashSet<>();ArrayDeque<Long> queue=new ArrayDeque<>();
            int rx=(int)Math.floor(model.lake().x()),rz=(int)Math.floor(model.lake().z());
            queue.add(GlobalDrainage.key(rx,rz));
            while(!queue.isEmpty()) {
                long key=queue.removeFirst();if(!visited.add(key))continue;
                int x=GlobalDrainage.gx(key),z=GlobalDrainage.gz(key);var c=terrain.sample(x,z);
                if(!c.wet())continue;
                var sample=terrain.hydrology().sample(x,z,c.top());
                require(sample.basin()==model.id(),"Different catchments accidentally merged at "+x+","+z);
                connected.add(key);
                require(connected.size()<150000,"Bounded water component");
                var h=terrain.baseTerrain().sample(x,z);
                require(h.top()-c.top()<=WatershedRivers.MAX_CUT+2,"Dense connected water incision bound");
                for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}})queue.add(GlobalDrainage.key(x+d[0],z+d[1]));
            }
            require(!connected.isEmpty(),"Terminal lake root actually contains block water");
            for(var r:model.reaches())for(var k:r.knots()) {
                int x=(int)Math.floor(k.x()),z=(int)Math.floor(k.z());boolean found=false;
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)found|=connected.contains(GlobalDrainage.key(x+dx,z+dz));
                require(found,"Refined channel disconnected from its terminal lake: "+x+","+z);
            }
            total+=connected.size();
        }
        report.append("connectedWaterComponents=").append(selected.size()).append(" denseWetColumns=").append(total).append('\n');
        System.out.println("connectedWaterComponents="+selected.size()+" denseWetColumns="+total);
    }
}
