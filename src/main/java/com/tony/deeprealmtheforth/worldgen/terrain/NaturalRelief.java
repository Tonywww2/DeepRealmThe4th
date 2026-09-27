package com.tony.deeprealmtheforth.worldgen.terrain;

import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import java.util.*;

/** Finite branching ridge masses. */
public final class NaturalRelief {
    private static final int CELL = 640;
    private final long seed;
    private final SpiralParameters parameters;
    private final ThreadLocal<Map<Long, List<Ridge>>> cache = ThreadLocal.withInitial(() ->
            new LinkedHashMap<>(128, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Long,List<Ridge>> e) { return size() > 128; }
            });
    public NaturalRelief(long seed, SpiralParameters parameters) {
        this.seed = seed; this.parameters = parameters;
    }
    private record Ridge(double ax, double az, double bx, double bz, double ah, double bh,
                         double leftWidth, double rightWidth) {}
    public record Relief(double height, double mountain, double drainage) {}
    public Relief sample(double x, double z, boolean arid) {
        double wx = x + 38 * SeededNoise.fractal(seed ^ 73921, x, z, 360, 2);
        double wz = z + 38 * SeededNoise.fractal(seed ^ 31913, x, z, 360, 2);
        double upland = 32 * SeededNoise.fractal(seed ^ 51279, wx, wz, 850, 2);
        double hills = 9.5 * SeededNoise.fractal(seed ^ 71139, wx, wz, 106, 3)
                + 1.8 * SeededNoise.fractal(seed ^ 12481, x, z, 28, 2);
        double flank=1+.22*SeededNoise.fractal(seed^82193,x,z,83,2)
                +.075*SeededNoise.fractal(seed^78917,x,z,29,2);
        hills+=1.5*SeededNoise.fractal(seed^928771,x,z,13,2);
        int gx = (int)Math.floor(wx / CELL), gz = (int)Math.floor(wz / CELL);
        double mountain = 0;
        Map<Long,List<Ridge>> memo = cache.get();
        for (int dx=-1; dx<=1; dx++) for (int dz=-1; dz<=1; dz++) {
            int cx=gx+dx, cz=gz+dz; long key=((long)cx<<32)^(cz&0xffffffffL);
            for (Ridge r : memo.computeIfAbsent(key, unused -> ridges(cx,cz))) {
                double vx=r.bx-r.ax, vz=r.bz-r.az, length=Math.hypot(vx,vz);
                double t=Math.max(0,Math.min(1,((wx-r.ax)*vx+(wz-r.az)*vz)/(length*length)));
                double distance=Math.hypot(wx-r.ax-t*vx,wz-r.az-t*vz);
                double side=(vx*(wz-r.az)-vz*(wx-r.ax))/length;
                // Blend handedness near a ridge end: a hard sign switch with unequal
                // widths would create a discontinuity along the segment's extension.
                double blend=Math.max(0,Math.min(1,.5+side/(48+distance*.3)));
                blend=blend*blend*(3-2*blend);
                double width=r.rightWidth+(r.leftWidth-r.rightWidth)*blend;
                width*=flank;
                // Piecewise sloping faces: a visible crest, softer talus, unequal flanks.
                double cross=Math.max(0,1-distance/width);
                double face=cross*(.60+.40*cross);
                mountain=Math.max(mountain,(r.ah+(r.bh-r.ah)*t)*face);
            }
        }
        // Coherent weathering adds shoulders, not independent one-block spikes.
        double weathering = 1 + .16 * SeededNoise.fractal(seed ^ 55187, wx, wz, 63, 2);
        double rise=mountain*weathering;
        double macro=upland*(arid?.72:1)+rise*(arid?.40:1);
        return new Relief(macro+hills*(arid?.7:1), rise, macro);
    }
    private List<Ridge> ridges(int gx, int gz) {
        long id=SeededNoise.hash(seed ^ 41681,gx,gz);
        if (SeededNoise.unit(id)<.24) return List.of();
        double cx=(gx+.5+random(id,1)*.38)*CELL, cz=(gz+.5+random(id,2)*.38)*CELL;
        double angle=Math.atan2(cz-parameters.centerZ(),cx-parameters.centerX())-Math.atan(parameters.twist())+random(id,3)*1.9;
        double tx=Math.cos(angle),tz=Math.sin(angle),nx=-tz,nz=tx;
        double length=300+140*(random(id,4)+.5), bend=50*random(id,5);
        double[] along={-length*.62,-length*.13,length*.26,length*.59};
        double[] across={50*random(id,6),bend,65*random(id,7),35*random(id,8)};
        double[] heights={0,42+55*(random(id,9)+.5),32+45*(random(id,10)+.5),0};
        double[] x=new double[4],z=new double[4];
        for(int i=0;i<4;i++){x[i]=cx+along[i]*tx+across[i]*nx;z[i]=cz+along[i]*tz+across[i]*nz;}
        List<Ridge> result=new ArrayList<>();
        for(int i=0;i<3;i++) result.add(new Ridge(x[i],z[i],x[i+1],z[i+1],heights[i],heights[i+1],
                1.65*(112+65*(random(id,11+i)+.5)),1.65*(85+70*(random(id,16+i)+.5))));
        int branches=(int)(4*(random(id,227)+.5));
        for(int i=0;i<branches;i++) {
            double side=random(id,21+i)>0?1:-1;
            double reach=135+75*(random(id,31+i)+.5);
            double t=.12+.76*(random(id,71+i)+.5),sx=x[1]+(x[2]-x[1])*t,sz=z[1]+(z[2]-z[1])*t;
            double branchAngle=angle+side*(.5+1.05*(random(id,81+i)+.5));
            result.add(new Ridge(sx,sz,sx+Math.cos(branchAngle)*reach,
                    sz+Math.sin(branchAngle)*reach,(heights[1]+(heights[2]-heights[1])*t)*.82,0,
                    1.5*(65+35*(random(id,51+i)+.5)),1.5*(85+45*(random(id,61+i)+.5))));
        }
        return List.copyOf(result);
    }
    private static double random(long seed,int salt){return SeededNoise.unit(SeededNoise.hash(seed,salt,941))-.5;}
}
