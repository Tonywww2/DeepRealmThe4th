package com.tonywww.deeprealm4th.worldgen.hydrology;

import com.tonywww.deeprealm4th.worldgen.terrain.BaseTerrain;
import com.tonywww.deeprealm4th.worldgen.terrain.SeededNoise;
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
        GlobalDrainage.Site[] near=new GlobalDrainage.Site[81];long[] owners=new long[81];
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++) {
            long k=GlobalDrainage.key(root.gx()+dx,root.gz()+dz);
            int index=(dx+4)*9+dz+4;
            near[index]=graph.site(k);owners[index]=graph.root(k);
        }
        for(int j=1;j<SIDE-1;j++)for(int i=1;i<SIDE-1;i++) {
            int k=j*SIDE+i,x=ox+i*STEP,z=oz+j*STEP;var c=terrain.sample(x,z);heights[k]=c.top();
            if(!c.land()||c.arm()!=root.arm()||c.edgeDistance()<24)continue;
            int closest=nearestSiteIndex(near,root.gx(),root.gz(),x,z);
            long owner=closest<0?GlobalDrainage.NONE:owners[closest];
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
        double[] signed=shorelineDistances(wet);
        // Re-measure the interpolated water at actual block centers; lake budget uses
        // this footprint, not the number of coarse cells requested by the grower.
        double area=interpolatedArea(signed);
        return new LakeMask(ox,oz,signed,area,low,high);
    }

    /** Exact nearest member of the original 9x9 lattice, with the same tie order. */
    public static int nearestSiteIndex(GlobalDrainage.Site[] near,int rootGX,int rootGZ,int x,int z) {
        double nearest=Double.POSITIVE_INFINITY;int closest=-1;
        // A site is jittered by at most .45*STEP per axis. The nearest
        // nominal lattice site lies within 86 blocks, whereas any site
        // three cells away on one axis lies at least 131 blocks away.
        // Thus this 5x5 window contains the exact winner among the 9x9 sites.
        int gx=Math.floorDiv(x+GlobalDrainage.STEP/2,GlobalDrainage.STEP)-rootGX;
        int gz=Math.floorDiv(z+GlobalDrainage.STEP/2,GlobalDrainage.STEP)-rootGZ;
        for(int dx=Math.max(-4,gx-2);dx<=Math.min(4,gx+2);dx++)
            for(int dz=Math.max(-4,gz-2);dz<=Math.min(4,gz+2);dz++) {
                int index=(dx+4)*9+dz+4;var site=near[index];
                double d=(site.x()-x)*(site.x()-x)+(site.z()-z)*(site.z()-z);
                if(d<nearest){nearest=d;closest=index;}
            }
        return closest;
    }

    /** Same block-center footprint as distance(), skipping cells whose four corners agree. */
    public static double interpolatedArea(double[] signed) {
        if(signed.length!=SIDE*SIDE)throw new IllegalArgumentException("Expected a 63x63 lake mask");
        LakeMask draft=new LakeMask(0,0,signed,0,0,0);
        double area=0;
        for(int i=0;i<SIDE-1;i++)for(int j=0;j<SIDE-1;j++) {
            double a=signed[j*SIDE+i],b=signed[j*SIDE+i+1];
            double c=signed[(j+1)*SIDE+i],d=signed[(j+1)*SIDE+i+1];
            if(a<0&&b<0&&c<0&&d<0){area+=STEP*STEP;continue;}
            if(a>=0&&b>=0&&c>=0&&d>=0)continue;
            for(int dx=0;dx<STEP;dx++)for(int dz=0;dz<STEP;dz++)
                if(draft.distance(i*STEP+dx+.5,j*STEP+dz+.5)<0)area++;
        }
        return area;
    }

    /** Exact bounded distance transform; exposed for brute-force parity regression tests. */
    public static double[] shorelineDistances(boolean[] wet) {
        if(wet.length!=SIDE*SIDE)throw new IllegalArgumentException("Expected a 63x63 lake mask");
        int[] horizontal=new int[wet.length];Arrays.fill(horizontal,SIDE);
        for(int j=1;j<SIDE-1;j++)for(int i=1;i<SIDE-1;i++) {
            int k=j*SIDE+i;
            if(wet[k]!=wet[k-1]||wet[k]!=wet[k+1]||wet[k]!=wet[k-SIDE]||wet[k]!=wet[k+SIDE])horizontal[k]=0;
        }
        // Nearest boundary X on each row. Only that point can win at any given Z.
        for(int j=0;j<SIDE;j++) {
            int row=j*SIDE;
            for(int i=1;i<SIDE;i++)horizontal[row+i]=Math.min(horizontal[row+i],horizontal[row+i-1]+1);
            for(int i=SIDE-2;i>=0;i--)horizontal[row+i]=Math.min(horizontal[row+i],horizontal[row+i+1]+1);
        }
        double[] signed=new double[wet.length];
        for(int j=0;j<SIDE;j++)for(int i=0;i<SIDE;i++) {
            int k=j*SIDE+i;double min=64;
            // Original distances clamp at 64 blocks: rows >=16 cells away cannot
            // lower the result. Retain the same hypot lookup (bit-for-bit rounding).
            for(int row=Math.max(0,j-15);row<=Math.min(SIDE-1,j+15);row++) {
                int dx=horizontal[row*SIDE+i];
                if(dx<16)min=Math.min(min,DISTANCES[dx][Math.abs(j-row)]);
            }
            signed[k]=(wet[k]?-1:1)*(min+STEP*.5);
        }
        return signed;
    }
}
