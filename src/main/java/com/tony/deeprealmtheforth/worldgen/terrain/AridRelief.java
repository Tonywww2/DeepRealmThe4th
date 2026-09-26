package com.tony.deeprealmtheforth.worldgen.terrain;

import java.util.*;
import static com.tony.deeprealmtheforth.worldgen.layout.SpiralLayout.smooth;

/** Finite, eroded polygonal mesas between branching dry mountains. V5 only. */
public final class AridRelief {
    private static final int CELL=512;
    private final long seed;
    private final ThreadLocal<Map<Long,Mass>> cache=ThreadLocal.withInitial(()->new LinkedHashMap<>(64,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Long,Mass> entry){return size()>64;}
    });
    private record Mass(double x,double z,double altitude,double[] nx,double[] nz,double[] offset) {}
    public record Sample(double height,double drainage,TerrainProfile.Theme kind) {}
    public AridRelief(long seed){this.seed=seed;}
    public Sample sample(double x,double z,NaturalRelief.Relief base) {
        double uplift=.20*base.mountain();
        double macro=base.drainage()+uplift, height=base.height()+uplift;
        double wx=x+20*SeededNoise.fractal(seed^17493,x,z,83,3)+6*SeededNoise.fractal(seed^79317,x,z,27,2);
        double wz=z+20*SeededNoise.fractal(seed^93871,x,z,83,3)+6*SeededNoise.fractal(seed^63917,x,z,27,2);
        int gx=(int)Math.floor(wx/CELL),gz=(int)Math.floor(wz/CELL);
        double best=0,cap=0;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            int cx=gx+dx,cz=gz+dz;long key=((long)cx<<32)^(cz&0xffffffffL);
            Mass m=cache.get().computeIfAbsent(key,k->mass(cx,cz));
            if(m.altitude==0||Math.abs(wx-m.x)>280||Math.abs(wz-m.z)>280)continue;
            double distance=Double.NEGATIVE_INFINITY;
            for(int i=0;i<m.nx.length;i++)distance=Math.max(distance,(wx-m.x)*m.nx[i]+(wz-m.z)*m.nz[i]-m.offset[i]);
            // Broad debris apron, then a recessed escarpment; no cylindrical buttes or sine terraces.
            double apron=1-smooth(14,79,distance),wall=1-smooth(-12,23,distance);
            double strength=.23*apron+.77*wall;
            double rise=Math.max(0,m.altitude-macro)*strength;
            if(rise>best){best=rise;cap=wall;}
        }
        macro+=best;
        // Remove inherited rolling hills on the cap, but leave low weathered relief.
        height=macro+(height-(base.drainage()+uplift))*(1-cap)
                +cap*(1.8*SeededNoise.fractal(seed^71393,x,z,67,2)+.8*SeededNoise.fractal(seed^23871,x,z,23,2));
        var kind=best>10&&cap>.66?TerrainProfile.Theme.PLATEAU
                :base.mountain()>43?TerrainProfile.Theme.ARID_MOUNTAIN:null;
        return new Sample(height,macro,kind);
    }
    private Mass mass(int x,int z) {
        long h=SeededNoise.hash(seed^543197,x,z);
        if(random(h,0)<.23)return new Mass(0,0,0,new double[0],new double[0],new double[0]);
        double angle=random(h,3)*Math.PI*2;int sides=5+(int)(random(h,4)*3);
        double[] nx=new double[sides],nz=new double[sides],offset=new double[sides];
        for(int i=0;i<sides;i++) {
            double a=angle+i*Math.PI*2/sides;
            nx[i]=Math.cos(a);nz[i]=Math.sin(a);offset[i]=95+85*random(h,10+i);
        }
        return new Mass((x+.27+.46*random(h,1))*CELL,(z+.27+.46*random(h,2))*CELL,
                42+32*random(h,5),nx,nz,offset);
    }
    private static double random(long h,int salt){return SeededNoise.unit(SeededNoise.hash(h,salt,631));}
}
