package com.tony.deeprealmtheforth.worldgen.hydrology;

import com.tony.deeprealmtheforth.worldgen.terrain.BaseTerrain;
import com.tony.deeprealmtheforth.worldgen.terrain.SeededNoise;
import java.util.*;

/** Terrain-guided connected depression footprint, not a perturbed circle or ellipse. */
public final class LakeMask {
    private static final int STEP=4,HALF=31,SIDE=HALF*2+1;
    // Geometry-only lookup, independent of world/seed. Preserve exactly the former
    // hypot(dx,dz)*STEP values, including floating-point rounding.
    private static final double[][] DISTANCES=distances();
    private static double[][] distances() {
        double[][] table=new double[SIDE][SIDE];
        for(int x=0;x<SIDE;x++)for(int z=0;z<SIDE;z++)table[x][z]=Math.hypot(x,z)*STEP;
        return table;
    }
    private final int x,z;
    private final double[] distance;
    private final double area,minGround,maxGround;
    private LakeMask(int x,int z,double[] distance,double area,double min,double max) {
        this.x=x;this.z=z;this.distance=distance.clone();this.area=area;minGround=min;maxGround=max;
    }
    public double area(){return area;}
    public double minGround(){return minGround;}
    public double maxGround(){return maxGround;}
    public int minX(){return x;} public int minZ(){return z;}
    public int maxX(){return x+(SIDE-1)*STEP;} public int maxZ(){return z+(SIDE-1)*STEP;}
    public double distance(double worldX,double worldZ) {
        double px=(worldX-x)/STEP,pz=(worldZ-z)/STEP;
        int ix=(int)Math.floor(px),iz=(int)Math.floor(pz);
        if(ix<0||iz<0||ix>=SIDE-1||iz>=SIDE-1)return 64;
        double tx=px-ix,tz=pz-iz;
        double a=distance[iz*SIDE+ix]*(1-tx)+distance[iz*SIDE+ix+1]*tx;
        double b=distance[(iz+1)*SIDE+ix]*(1-tx)+distance[(iz+1)*SIDE+ix+1]*tx;
        return a*(1-tz)+b*tz;
    }
    private record Entry(int index,double cost) {}
    public static LakeMask create(long seed,BaseTerrain terrain,GlobalDrainage graph,GlobalDrainage.Basin basin,double targetArea) {
        var root=basin.root().site();int ox=(int)Math.floor(root.x()/STEP)*STEP-HALF*STEP,
                oz=(int)Math.floor(root.z()/STEP)*STEP-HALF*STEP;
        int count=SIDE*SIDE;double[] heights=new double[count],cost=new double[count];boolean[] allowed=new boolean[count];
        List<GlobalDrainage.Site> near=new ArrayList<>();Map<Long,Long> owners=new HashMap<>();
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++) {
            long k=GlobalDrainage.key(root.gx()+dx,root.gz()+dz);near.add(graph.site(k));owners.put(k,graph.root(k));
        }
        for(int j=1;j<SIDE-1;j++)for(int i=1;i<SIDE-1;i++) {
            int k=j*SIDE+i,x=ox+i*STEP,z=oz+j*STEP;var c=terrain.sample(x,z);heights[k]=c.top();
            if(!c.land()||c.arm()!=root.arm()||c.edgeDistance()<24)continue;
            double nearest=Double.POSITIVE_INFINITY;long owner=GlobalDrainage.NONE;
            for(var site:near){double d=(site.x()-x)*(site.x()-x)+(site.z()-z)*(site.z()-z);if(d<nearest){nearest=d;owner=owners.get(site.id());}}
            if(owner!=basin.id())continue;
            allowed[k]=true;
            // A weak travel penalty keeps the finite proposal local; actual low ground
            // dominates which bays and fingers are filled, not distance from a circle center.
            cost[k]=c.top()+.025*Math.hypot(x-root.x(),z-root.z())
                    +1.8*SeededNoise.fractal(seed^918331,x,z,41,2);
        }
        int start=HALF*SIDE+HALF;
        if(!allowed[start])return null;
        PriorityQueue<Entry> queue=new PriorityQueue<>(Comparator.comparingDouble(Entry::cost).thenComparingInt(Entry::index));
        boolean[] seen=new boolean[count],wet=new boolean[count];queue.add(new Entry(start,cost[start]));seen[start]=true;
        int target=(int)Math.ceil(targetArea/(STEP*STEP)*1.14),used=0;
        double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;
        while(!queue.isEmpty()&&used<target) {
            Entry e=queue.remove();int k=e.index;wet[k]=true;used++;low=Math.min(low,heights[k]);high=Math.max(high,heights[k]);
            for(int offset:new int[]{-1,1,-SIDE,SIDE}) {
                int n=k+offset;
                if(n<0||n>=count||seen[n]||!allowed[n])continue;
                seen[n]=true;queue.add(new Entry(n,Math.max(e.cost,cost[n])));
            }
        }
        if(used<target)return null;
        // An allocation boundary is not a shoreline. Reject instead of drawing a square edge.
        for(int i=0;i<SIDE;i++)if(wet[SIDE+i]||wet[(SIDE-2)*SIDE+i]||wet[i*SIDE+1]||wet[i*SIDE+SIDE-2])return null;
        double[] signed=new double[count];List<Integer> boundary=new ArrayList<>();
        for(int j=1;j<SIDE-1;j++)for(int i=1;i<SIDE-1;i++) {
            int k=j*SIDE+i;
            if(wet[k]!=wet[k-1]||wet[k]!=wet[k+1]||wet[k]!=wet[k-SIDE]||wet[k]!=wet[k+SIDE])boundary.add(k);
        }
        for(int j=0;j<SIDE;j++)for(int i=0;i<SIDE;i++) {
            int k=j*SIDE+i;double min=64;
            for(int edge:boundary)min=Math.min(min,DISTANCES[Math.abs(i-edge%SIDE)][Math.abs(j-edge/SIDE)]);
            signed[k]=(wet[k]?-1:1)*(min+STEP*.5);
        }
        // Re-measure the interpolated water at actual block centers; lake budget uses
        // this footprint, not the number of coarse cells requested by the grower.
        LakeMask draft=new LakeMask(ox,oz,signed,0,low,high);double area=0;
        for(int x=ox;x<draft.maxX();x++)for(int z=oz;z<draft.maxZ();z++)if(draft.distance(x+.5,z+.5)<0)area++;
        return new LakeMask(ox,oz,signed,area,low,high);
    }
}
