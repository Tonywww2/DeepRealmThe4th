package com.tony.deeprealmtheforth.worldgen.hydrology;

import com.tony.deeprealmtheforth.worldgen.terrain.BaseTerrain;
import com.tony.deeprealmtheforth.worldgen.terrain.SeededNoise;
import java.util.*;

/** Immutable complete-catchment geometry. Chunk-sized tiles are only spatial indices. */
public final class WatershedRivers {
    public static final int MAX_CUT = 24;
    private static final int TILE = 64;
    public record Knot(double x,double z,double water,double left,double right,double depth) {}
    public record Reach(long id,long downstream,long basin,List<Knot> knots,
                        double minX,double minZ,double maxX,double maxZ) {
        public Reach { knots=List.copyOf(knots); }
    }
    public record Lake(long basin,double x,double z,LakeMask mask,
                       int water,double area,double inflow,double evaporation,double seepage,double capacity) {}
    public record Model(long id,List<Reach> reaches,Lake lake,String status) {
        public Model { reaches=List.copyOf(reaches); }
        public boolean accepted(){return status.equals("ACCEPTED");}
    }
    public record Sample(double distance,int water,double depth,long basin,double containment) {
        public static final Sample NONE=new Sample(Double.POSITIVE_INFINITY,-64,0,0,-64);
        public boolean wet(){return distance<0;}
    }
    private record Segment(Knot a,Knot b,long basin,double dx,double dz,double length,double minX,double minZ,double maxX,double maxZ) {
        static Segment of(Knot a,Knot b,long basin) {
            double padding=80+Math.max(Math.max(a.left,a.right),Math.max(b.left,b.right));
            return new Segment(a,b,basin,b.x-a.x,b.z-a.z,Math.hypot(b.x-a.x,b.z-a.z),
                    Math.min(a.x,b.x)-padding,Math.min(a.z,b.z)-padding,Math.max(a.x,b.x)+padding,Math.max(a.z,b.z)+padding);
        }
    }
    private record Tile(List<Segment> segments,List<Lake> lakes) {}
    private record Corridor(GlobalDrainage.Site a,GlobalDrainage.Site b) {}
    private final long seed;
    private final BaseTerrain terrain;
    private final ClimateSnapshot climate;
    private final GlobalDrainage drainage;
    private final Map<Long,Model> models=bounded(192);
    private final Map<Long,Tile> tiles=bounded(512);
    private volatile long cacheEpoch;
    private final ThreadLocal<LastTile> lastTile=ThreadLocal.withInitial(LastTile::new);
    private static final class LastTile {long key,epoch;Tile tile;}
    public WatershedRivers(long seed,BaseTerrain terrain,ClimateSnapshot climate) {
        this.seed=seed;this.terrain=terrain;this.climate=climate;
        drainage=new GlobalDrainage(seed,terrain,climate);
    }
    private static <V> Map<Long,V> bounded(int limit) {
        return new LinkedHashMap<>(limit,.75f,true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Long,V> e){return size()>limit;}
        };
    }
    public GlobalDrainage drainage(){return drainage;}
    public synchronized void clearCaches(){models.clear();tiles.clear();drainage.clearCaches();cacheEpoch++;}
    public synchronized Model model(long site) {
        long id=drainage.root(site);Model cached=models.get(id);if(cached!=null)return cached;
        Model result=build(drainage.basin(id));models.put(id,result);return result;
    }
    private static Model rejected(long id,String why){return new Model(id,List.of(),null,why);}
    private Model build(GlobalDrainage.Basin basin) {
        if(!basin.complete())return rejected(basin.id(),"CAPACITY");
        if(!basin.root().wet())return rejected(basin.id(),"DRY");
        var wet=basin.nodes().stream().filter(n->n.wet()&&n.downstream()!=GlobalDrainage.NONE).toList();
        if(wet.isEmpty())return rejected(basin.id(),"NO_CHANNEL");
        Map<Long,GlobalDrainage.Flow> nodes=new HashMap<>();for(var n:basin.nodes())nodes.put(n.site().id(),n);
        Map<Long,GlobalDrainage.Flow> dominant=new HashMap<>();
        for(var n:wet){var old=dominant.get(n.downstream());if(old==null||old.discharge()<n.discharge())dominant.put(n.downstream(),n);}
        Map<Long,List<Knot>> paths=new HashMap<>();Map<Long,Double> caps=new HashMap<>(),levels=new HashMap<>();
        for(var n:basin.nodes())caps.put(n.site().id(),Math.floor(n.site().height())-3);
        // First fix geometry, then lower profile ceilings where a bank would otherwise leak.
        for(var n:wet) {
            var a=n.site();var b=nodes.get(n.downstream()).site();
            List<Corridor> corridors=corridors(a,b);
            double length=Math.hypot(b.x()-a.x(),b.z()-a.z());
            double[] ta=tangent(n,nodes,dominant),tb=tangent(nodes.get(n.downstream()),nodes,dominant);
            int pieces=Math.max(4,(int)Math.ceil(length/5));List<Knot> path=new ArrayList<>();
            double lowerBy=0;
            for(int i=0;i<=pieces;i++) {
                double t=i/(double)pieces,t2=t*t,t3=t2*t;
                double x=(2*t3-3*t2+1)*a.x()+(t3-2*t2+t)*length*.65*ta[0]
                        +(-2*t3+3*t2)*b.x()+(t3-t2)*length*.65*tb[0];
                double z=(2*t3-3*t2+1)*a.z()+(t3-2*t2+t)*length*.65*ta[1]
                        +(-2*t3+3*t2)*b.z()+(t3-t2)*length*.65*tb[1];
                double dx=(b.x()-a.x())/length,dz=(b.z()-a.z())/length;
                double nx=-dz,nz=dx;
                double cx=a.x()+(b.x()-a.x())*t,cz=a.z()+(b.z()-a.z())*t;
                // Retain forward progress, bound deviation inside the original planar corridor.
                double offset=(x-cx)*nx+(z-cz)*nz,clearance=Math.min(14,length*.18);
                double separation=Double.POSITIVE_INFINITY;
                for(var other:corridors) {
                    separation=Math.min(separation,distance(cx,cz,other.a.x(),other.a.z(),other.b.x(),other.b.z()));
                }
                clearance=Math.min(clearance,.18*separation);
                double envelope=4*t*(1-t);
                double search=clearance*envelope;
                offset=Math.max(-search,Math.min(search,offset));
                // Follow local low ground within the fixed corridor, not an arbitrary sine wave.
                double best=Double.POSITIVE_INFINITY,chosen=offset;
                for(int k=-2;k<=2;k++) {
                    double candidate=Math.max(-search,Math.min(search,offset+k*search*.35));
                    double height=terrain.shape((int)Math.floor(cx+nx*candidate),(int)Math.floor(cz+nz*candidate)).top();
                    double cost=height+.055*(candidate-offset)*(candidate-offset);
                    if(cost<best){best=cost;chosen=candidate;}
                }
                x=cx+nx*chosen;z=cz+nz*chosen;
                double q=n.discharge()+(nodes.get(n.downstream()).discharge()-n.discharge())*t;
                double width=Math.min(13,1.5+1.55*Math.pow(q/GlobalDrainage.WET_FLOW,.42));
                if(n.incoming()==0)width*=.46+.54*Math.min(1,t*length/48);
                double left=Math.max(1.4,width*(1+.30*SeededNoise.fractal(seed^88129,x+nx*13,z+nz*13,39,2)));
                double right=Math.max(1.4,width*(1+.30*SeededNoise.fractal(seed^97813,x-nx*13,z-nz*13,53,2)));
                // Apply the same bound across catchments, not just within this model.
                double corridorWidth=Math.max(1.2,.24*separation);
                left=Math.min(left,corridorWidth);right=Math.min(right,corridorWidth);
                double water=lerp(Math.floor(a.height())-3,Math.floor(b.height())-3,t);
                for(int side:new int[]{-1,1}) {
                    double w=side>0?left:right;
                    double ground=terrain.shape((int)Math.floor(x+nx*side*(w+3)),(int)Math.floor(z+nz*side*(w+3))).top();
                    lowerBy=Math.max(lowerBy,water-ground);
                }
                path.add(new Knot(x,z,water,left,right,Math.min(5,2.0+Math.log1p(q/GlobalDrainage.WET_FLOW))));
            }
            paths.put(a.id(),path);
            caps.merge(a.id(),Math.floor(a.height())-3-Math.ceil(lowerBy),Math::min);
            caps.merge(b.id(),Math.floor(b.height())-3-Math.ceil(lowerBy),Math::min);
        }
        for(var n:basin.nodes()) {
            long id=n.site().id();double level=Math.min(caps.get(id),levels.getOrDefault(id,Double.POSITIVE_INFINITY));
            levels.put(id,level);
            if(n.downstream()!=GlobalDrainage.NONE)levels.merge(n.downstream(),level,Math::min);
        }
        List<Reach> reaches=new ArrayList<>();
        for(var n:wet) {
            var raw=paths.get(n.site().id());List<Knot> path=new ArrayList<>();
            for(int i=0;i<raw.size();i++) {
                Knot k=raw.get(i);double level=lerp(levels.get(n.site().id()),levels.get(n.downstream()),i/(double)(raw.size()-1));
                double h=terrain.shape((int)Math.floor(k.x),(int)Math.floor(k.z)).top();
                if(h-level+k.depth>MAX_CUT||level-h>3)return rejected(basin.id(),"INCISION");
                var mask=terrain.field().sample(k.x,k.z);
                if(!mask.land()||mask.edgeDistance()<32)return rejected(basin.id(),"VOID_MARGIN");
                path.add(new Knot(k.x,k.z,level,k.left,k.right,k.depth));
            }
            double minX=path.stream().mapToDouble(Knot::x).min().orElseThrow()-48;
            double minZ=path.stream().mapToDouble(Knot::z).min().orElseThrow()-48;
            double maxX=path.stream().mapToDouble(Knot::x).max().orElseThrow()+48;
            double maxZ=path.stream().mapToDouble(Knot::z).max().orElseThrow()+48;
            reaches.add(new Reach(n.site().id(),n.downstream(),basin.id(),path,minX,minZ,maxX,maxZ));
        }
        var root=basin.root();var s=root.site();
        double evaporation=climate.sample(terrain,(int)Math.floor(s.x()),(int)Math.floor(s.z())).evaporation();
        double capacity=18+8*SeededNoise.unit(SeededNoise.hash(seed^71281,s.gx(),s.gz()));
        LakeMask mask=LakeMask.create(seed,terrain,drainage,basin,Math.max(180,root.discharge()/(capacity*.72+evaporation)));
        if(mask==null)return rejected(basin.id(),"LAKE_CAPACITY");
        int oldWater=(int)Math.floor(levels.get(s.id()));
        int water=(int)Math.floor(Math.min(oldWater,mask.minGround()));
        if(mask.maxGround()-water+5>MAX_CUT)return rejected(basin.id(),"LAKE_DEPTH");
        double area=mask.area();
        double evaporationLoss=Math.min(root.discharge(),area*evaporation);
        double seepage=root.discharge()-evaporationLoss;
        if(area==0||seepage>area*capacity*.90)return rejected(basin.id(),"LAKE_BALANCE");
        Lake lake=new Lake(basin.id(),s.x(),s.z(),mask,water,area,root.discharge(),evaporationLoss,seepage,capacity);
        if(water!=oldWater) {
            List<Reach> adjusted=new ArrayList<>();
            for(Reach r:reaches) {
                if(r.downstream!=s.id()){adjusted.add(r);continue;}
                List<Knot> path=new ArrayList<>();
                for(int i=0;i<r.knots.size();i++) {
                    Knot k=r.knots.get(i);double level=k.water+(water-oldWater)*i/(r.knots.size()-1.0);
                    if(terrain.shape((int)Math.floor(k.x),(int)Math.floor(k.z)).top()-level+k.depth>MAX_CUT)return rejected(basin.id(),"LAKE_INLET");
                    path.add(new Knot(k.x,k.z,level,k.left,k.right,k.depth));
                }
                adjusted.add(new Reach(r.id,r.downstream,r.basin,path,r.minX,r.minZ,r.maxX,r.maxZ));
            }
            reaches=adjusted;
        }
        return new Model(basin.id(),reaches,lake,"ACCEPTED");
    }
    private List<Corridor> corridors(GlobalDrainage.Site a,GlobalDrainage.Site b) {
        List<Corridor> result=new ArrayList<>();
        for(int gx=Math.min(a.gx(),b.gx())-3;gx<=Math.max(a.gx(),b.gx())+3;gx++)
            for(int gz=Math.min(a.gz(),b.gz())-3;gz<=Math.max(a.gz(),b.gz())+3;gz++) {
                long id=GlobalDrainage.key(gx,gz),parent=drainage.parent(id);
                if(parent==GlobalDrainage.NONE||id==a.id()||id==b.id()||parent==a.id()||parent==b.id())continue;
                result.add(new Corridor(drainage.site(id),drainage.site(parent)));
            }
        return result;
    }
    private static double[] tangent(GlobalDrainage.Flow node,Map<Long,GlobalDrainage.Flow> nodes,Map<Long,GlobalDrainage.Flow> dominant) {
        var s=node.site();double x=0,z=0;
        var up=dominant.get(s.id());if(up!=null){double l=Math.hypot(s.x()-up.site().x(),s.z()-up.site().z());x+=(s.x()-up.site().x())/l;z+=(s.z()-up.site().z())/l;}
        if(node.downstream()!=GlobalDrainage.NONE){var down=nodes.get(node.downstream()).site();double l=Math.hypot(down.x()-s.x(),down.z()-s.z());x+=(down.x()-s.x())/l;z+=(down.z()-s.z())/l;}
        double length=Math.hypot(x,z);return length<1e-8?new double[]{1,0}:new double[]{x/length,z/length};
    }
    private static double distance(double x,double z,double ax,double az,double bx,double bz) {
        double dx=bx-ax,dz=bz-az,t=Math.max(0,Math.min(1,((x-ax)*dx+(z-az)*dz)/(dx*dx+dz*dz)));
        return Math.hypot(x-ax-t*dx,z-az-t*dz);
    }
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    public double lakeDistance(Lake lake,double x,double z,double ground) {
        return lake.mask.distance(x,z);
    }
    private synchronized Tile tile(int x,int z) {
        int tx=Math.floorDiv(x,TILE),tz=Math.floorDiv(z,TILE);long key=GlobalDrainage.key(tx,tz);
        Tile cached=tiles.get(key);if(cached!=null)return cached;
        Set<Long> seen=new HashSet<>();List<Reach> reaches=new ArrayList<>();List<Lake> lakes=new ArrayList<>();
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++) {
            long site=GlobalDrainage.key(tx+dx,tz+dz),root=drainage.root(site);if(!seen.add(root))continue;
            Model model=model(root);if(!model.accepted())continue;
            for(Reach r:model.reaches)if(r.maxX>=tx*TILE&&r.minX<=(tx+1)*TILE&&r.maxZ>=tz*TILE&&r.minZ<=(tz+1)*TILE)reaches.add(r);
            Lake l=model.lake;
            if(l.mask.maxX()+32>=tx*TILE&&l.mask.minX()-32<=(tx+1)*TILE&&l.mask.maxZ()+32>=tz*TILE&&l.mask.minZ()-32<=(tz+1)*TILE)lakes.add(l);
        }
        reaches.sort(Comparator.comparingLong(Reach::id));lakes.sort(Comparator.comparingLong(Lake::basin));
        List<Segment> segments=new ArrayList<>();
        for(Reach r:reaches)for(int i=1;i<r.knots.size();i++)segments.add(Segment.of(r.knots.get(i-1),r.knots.get(i),r.basin));
        Tile result=new Tile(List.copyOf(segments),List.copyOf(lakes));tiles.put(key,result);return result;
    }
    public Sample sample(int blockX,int blockZ,double ground) {
        long key=GlobalDrainage.key(Math.floorDiv(blockX,TILE),Math.floorDiv(blockZ,TILE));
        LastTile last=lastTile.get();long epoch=cacheEpoch;
        if(last.tile==null||last.key!=key||last.epoch!=epoch){last.tile=tile(blockX,blockZ);last.key=key;last.epoch=epoch;}
        Tile tile=last.tile;double x=blockX+.5,z=blockZ+.5;
        double best=Double.POSITIVE_INFINITY,depth=0,ceiling=-64;int water=-64;long basin=0;
        for(Segment segment:tile.segments) {
            if(x<segment.minX||x>segment.maxX||z<segment.minZ||z>segment.maxZ)continue;
            Knot a=segment.a,b=segment.b;
            double dx=segment.dx,dz=segment.dz,length=segment.length;
            double along=((x-a.x)*dx+(z-a.z)*dz)/length,t=Math.max(0,Math.min(1,along/length));
            double side=((z-a.z)*dx-(x-a.x)*dz)/length;
            double width=side>=0?lerp(a.left,b.left,t):lerp(a.right,b.right,t);
            double d=Math.max(Math.abs(side)-width,Math.max(-along,along-length));
            int level=(int)Math.floor(lerp(a.water,b.water,t));
            if(d<9)ceiling=Math.max(ceiling,level);
            if(d<0 ? best>=0||level>water||level==water&&d<best : best>=0&&d<best) {
                best=d;water=level;depth=lerp(a.depth,b.depth,t);basin=segment.basin;
            }
        }
        for(Lake lake:tile.lakes) {
            double d=lakeDistance(lake,x,z,ground);
            if(d<9)ceiling=Math.max(ceiling,lake.water);
            if(d<0 ? best>=0||lake.water>water||lake.water==water&&d<best : best>=0&&d<best){best=d;water=lake.water;depth=5;basin=lake.basin;}
        }
        return best>40?Sample.NONE:new Sample(best,water,depth,basin,ceiling);
    }
}
