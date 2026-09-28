package com.tonywww.deeprealm4th.worldgen.hydrology;

import com.tonywww.deeprealm4th.worldgen.terrain.BaseTerrain;
import com.tonywww.deeprealm4th.worldgen.terrain.SeededNoise;
import java.util.*;

/** World-coordinate drainage, independent of chunk, preview window and cache boundaries.
 * Locally certified empty-circle edges form the global Delaunay graph. A bounded potential gives
 * an acyclic graph; complete reverse catchments are solved, never a clipped halo.
 */
public final class GlobalDrainage {
    public static final int STEP = 64, MAX_NODES = 4096;
    public static final long NONE = Long.MIN_VALUE;
    /** Start smaller headwaters earlier; bank width and depth still use the old flow scale. */
    public static final double HEADWATER_FLOW = 8000;
    public static final double CHANNEL_FLOW_SCALE = 12000;
    private static final int MAX_RANK = 16000;
    public record Site(long id, int gx, int gz, double x, double z, double height,
                       int rank, int arm, boolean active) {}
    public record Flow(Site site, long downstream, double local, double loss,
                       double discharge, int order, int incoming) {
        public boolean wet() { return discharge >= HEADWATER_FLOW; }
    }
    public record Basin(long id, List<Flow> nodes, boolean complete, double supply, double loss) {
        public Basin { nodes = List.copyOf(nodes); }
        public Flow root() { return nodes.get(nodes.size() - 1); }
    }
    private final long seed;
    private final BaseTerrain terrain;
    private final ClimateSnapshot climate;
    private final SingleFlightCache<Site> sites = new SingleFlightCache<>(32768);
    private final SingleFlightCache<long[]> edges = new SingleFlightCache<>(32768);
    private final SingleFlightCache<Long> parents = new SingleFlightCache<>(32768), roots = new SingleFlightCache<>(32768);
    private final SingleFlightCache<Basin> basins = new SingleFlightCache<>(128);

    public GlobalDrainage(long seed, BaseTerrain terrain, ClimateSnapshot climate) {
        this.seed = seed; this.terrain = terrain; this.climate = climate;
    }
    public static long key(int x, int z) { return ((long)x << 32) ^ (z & 0xffffffffL); }
    public static int gx(long key) { return (int)(key >> 32); }
    public static int gz(long key) { return (int)key; }
    private double[] position(long id) {
        int x = gx(id), z = gz(id);
        long h = SeededNoise.hash(seed ^ 0x693B8E1, x, z);
        return new double[]{(x + .90 * (SeededNoise.unit(h) - .5)) * STEP,
                (z + .90 * (SeededNoise.unit(SeededNoise.hash(h, 4, 19)) - .5)) * STEP};
    }
    public Site site(long id) { return sites.get(id, () -> calculateSite(id)); }
    private Site calculateSite(long id) {
        double[] p = position(id);
        int x = (int)Math.floor(p[0]), z = (int)Math.floor(p[1]);
        var c = terrain.sample(x, z);
        var layout = terrain.layout().parameters();
        double radius = Math.hypot(x - layout.centerX(), z - layout.centerZ());
        boolean active = c.land() && (c.arm() == 1 || c.arm() == 5) && c.edgeDistance() > 76
                && radius > layout.plungeRadius() + 200;
        // Suppress block quantization and tiny soil hummocks in the routing potential.
        double height = c.top();
        if (active) {
            height = terrain.drainageHeight(x,z);
        }
        height = Math.max(-64, Math.min(424, height));
        int tie = (int)(SeededNoise.hash(seed ^ 83749, gx(id), gz(id)) & 15);
        int rank = (int)Math.floor((height+64)*2)*16 + tie;
        Site result = new Site(id, gx(id), gz(id), p[0], p[1], height, rank, c.arm(), active);
        return result;
    }
    /** Whole infinite jitter lattice, not a triangulation cropped to a query window. */
    public long[] neighbors(long id) { return edges.get(id, () -> calculateNeighbors(id)).clone(); }
    private long[] calculateNeighbors(long id) {
        double[] origin = position(id);
        List<Long> keys = new ArrayList<>(48); List<double[]> points = new ArrayList<>(48);
        for(int dx=-3;dx<=3;dx++) for(int dz=-3;dz<=3;dz++) if(dx!=0||dz!=0) {
            long k=key(gx(id)+dx,gz(id)+dz); keys.add(k); points.add(position(k));
        }
        List<Long> result = new ArrayList<>();
        // Covering radius R <= sqrt(2)*.95*STEP. An empty circle has radius <= R;
        // both its other endpoint and every potential blocker lie within 2R of us.
        // Thus this 7x7 lattice neighborhood certifies GLOBAL edges, not tile edges.
        double radius = Math.sqrt(2)*.95*STEP, maxLength=2*radius;
        for(int i=0;i<keys.size();i++) {
            double[] p=points.get(i); double dx=p[0]-origin[0],dz=p[1]-origin[1];
            if(dx*dx+dz*dz>maxLength*maxLength) continue;
            double length=Math.hypot(dx,dz), nx=-dz/length,nz=dx/length;
            double halfX=dx*.5,halfZ=dz*.5;
            double limit=Math.sqrt(Math.max(0,radius*radius-length*length*.25));
            double low=-limit,high=limit;
            for(int j=0;j<keys.size();j++) if(j!=i) {
                double[] q=points.get(j);
                double qx=q[0]-origin[0]-halfX,qz=q[1]-origin[1]-halfZ;
                double rhs=qx*qx+qz*qz-length*length*.25;
                double projection=2*(qx*nx+qz*nz);
                // Center = midpoint + t*normal; q must remain outside the circle.
                if(Math.abs(projection)<1e-10) {if(rhs<1e-8){low=1;high=0;break;}}
                else if(projection>0)high=Math.min(high,rhs/projection);
                else low=Math.max(low,rhs/projection);
                if(high-low<=1e-8)break;
            }
            // A zero-length dual edge is omitted: cocircular diagonals cannot cross.
            if(high-low>1e-8) result.add(keys.get(i));
        }
        long[] answer=result.stream().sorted().mapToLong(Long::longValue).toArray();
        return answer;
    }
    public long parent(long id) { return parents.get(id, () -> calculateParent(id)); }
    private long calculateParent(long id) {
        Site a=site(id); long best=NONE; double score=Double.NEGATIVE_INFINITY;
        if(a.active()) for(long k:neighbors(id)) {
            Site b=site(k); if(!b.active()||b.arm()!=a.arm()||b.rank()>=a.rank()) continue;
            double length=Math.hypot(b.x()-a.x(),b.z()-a.z());
            // A gentler downhill edge tends to stay in a valley and reaches a
            // terminal basin later than the steepest descent. Rank still strictly
            // decreases, so the global drainage graph remains acyclic.
            double slope=(a.rank()-b.rank())/length;
            double candidate=-slope;
            if(candidate<score) continue;
            boolean supported=true;
            for(int s=1;s<Math.ceil(length/8);s++) {
                double t=s/Math.ceil(length/8);
                var p=terrain.field().sample(a.x()+(b.x()-a.x())*t,a.z()+(b.z()-a.z())*t);
                if(!p.land()||p.arm()!=a.arm()||p.edgeDistance()<64) {supported=false;break;}
            }
            if(supported && (candidate>score||best==NONE||k<best)) {best=k;score=candidate;}
        }
        return best;
    }
    public long root(long id) { return roots.get(id, () -> calculateRoot(id)); }
    private long calculateRoot(long id) {
        long epoch=roots.epoch();
        List<Long> visited=new ArrayList<>(); long current=id;
        for(int n=0;n<=MAX_RANK;n++) {
            Long known=roots.completed(current); if(known!=null){current=known;break;}
            visited.add(current); long down=parent(current);
            if(down==NONE) break;
            if(site(down).rank()>=site(current).rank()) throw new IllegalStateException("Non-descending global drainage");
            current=down;
            if(n==MAX_RANK) throw new IllegalStateException("Drainage potential bound exceeded");
        }
        for(long k:visited) roots.remember(k,current,epoch);
        return current;
    }
    public Basin basin(long site) {
        long id=root(site); return basins.get(id, () -> calculateBasin(id));
    }
    private Basin calculateBasin(long id) {
        ArrayDeque<Long> queue=new ArrayDeque<>(); queue.add(id);
        List<Site> members=new ArrayList<>();
        // In a strict single-parent DAG every reverse child is visited only once.
        while(!queue.isEmpty() && members.size()<=MAX_NODES) {
            long k=queue.removeFirst(); members.add(site(k));
            for(long n:neighbors(k)) if(parent(n)==k) queue.addLast(n);
        }
        if(members.size()>MAX_NODES) {
            return new Basin(id,List.of(),false,0,0);
        }
        members.sort(Comparator.comparingInt(Site::rank).reversed().thenComparingLong(Site::id));
        Map<Long,Double> incomingFlow=new HashMap<>(); Map<Long,Integer> order=new HashMap<>(),equal=new HashMap<>(),incoming=new HashMap<>();
        List<Flow> flow=new ArrayList<>(); double supply=0,totalLoss=0;
        for(Site n:members) {
            long down=parent(n.id());
            double local=n.active()?climate.sample(terrain,(int)Math.floor(n.x()),(int)Math.floor(n.z())).runoff()*STEP*STEP:0;
            double input=local+incomingFlow.getOrDefault(n.id(),0.0);
            double loss=down==NONE?0:Math.min(input,.055*Math.hypot(n.x()-site(down).x(),n.z()-site(down).z()));
            double q=input-loss; int strahler=Math.max(1,order.getOrDefault(n.id(),1)+(equal.getOrDefault(n.id(),0)>1?1:0));
            flow.add(new Flow(n,down,local,loss,q,strahler,incoming.getOrDefault(n.id(),0)));
            supply+=local; totalLoss+=loss;
            if(down!=NONE) {
                incomingFlow.merge(down,q,Double::sum);
                if(q>=HEADWATER_FLOW) {
                    incoming.merge(down,1,Integer::sum);
                    int old=order.getOrDefault(down,0);
                    if(strahler>old){order.put(down,strahler);equal.put(down,1);}
                    else if(strahler==old)equal.merge(down,1,Integer::sum);
                }
            }
        }
        return new Basin(id,flow,true,supply,totalLoss);
    }
    public void clearCaches() { sites.clear();edges.clear();parents.clear();roots.clear();basins.clear(); }
}
