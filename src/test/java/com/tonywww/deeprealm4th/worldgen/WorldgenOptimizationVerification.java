package com.tonywww.deeprealm4th.worldgen;

import com.tonywww.deeprealm4th.worldgen.hydrology.LakeMask;
import java.util.*;

/** Optimized lake transform must reproduce the former all-boundaries scan exactly. */
public final class WorldgenOptimizationVerification {
    public static void main(String[] args) {
        final int side=63;Random random=new Random(431928);
        double[][] distances=new double[side][side];
        for(int x=0;x<side;x++)for(int z=0;z<side;z++)distances[x][z]=Math.hypot(x,z)*4;
        long checked=0,areaChecked=0;
        for(int test=0;test<32;test++) {
            boolean[] wet=new boolean[side*side];
            for(int z=0;z<side;z++)for(int x=0;x<side;x++)wet[z*side+x]=switch(test) {
                case 0 -> false;
                case 1 -> true;
                case 2 -> x==31&&z==31;
                case 3 -> x<31;
                case 4 -> x>2&&x<57&&z>4&&z<42;
                case 5 -> x==z;
                default -> random.nextDouble()<(test-5)/28.0;
            };
            List<Integer> boundary=new ArrayList<>();
            for(int z=1;z<side-1;z++)for(int x=1;x<side-1;x++) {
                int k=z*side+x;
                if(wet[k]!=wet[k-1]||wet[k]!=wet[k+1]||wet[k]!=wet[k-side]||wet[k]!=wet[k+side])boundary.add(k);
            }
            double[] actual=LakeMask.shorelineDistances(wet);
            double expectedArea=0;
            for(int x=0;x<(side-1)*4;x++)for(int z=0;z<(side-1)*4;z++) {
                double px=(x+.5)/4,pz=(z+.5)/4;
                int ix=(int)Math.floor(px),iz=(int)Math.floor(pz);
                double tx=px-ix,tz=pz-iz;
                double a=actual[iz*side+ix]*(1-tx)+actual[iz*side+ix+1]*tx;
                double b=actual[(iz+1)*side+ix]*(1-tx)+actual[(iz+1)*side+ix+1]*tx;
                if(a*(1-tz)+b*tz<0)expectedArea++;
                areaChecked++;
            }
            if(expectedArea!=LakeMask.interpolatedArea(actual))
                throw new AssertionError("Lake area changed: mask="+test);
            for(int z=0;z<side;z++)for(int x=0;x<side;x++) {
                int k=z*side+x;double min=64;
                for(int edge:boundary)min=Math.min(min,distances[Math.abs(x-edge%side)][Math.abs(z-edge/side)]);
                double expected=(wet[k]?-1:1)*(min+2);
                if(Double.doubleToLongBits(expected)!=Double.doubleToLongBits(actual[k]))
                    throw new AssertionError("Lake distance changed: mask="+test+" at="+x+","+z);
                checked++;
            }
        }
        System.out.println("WORLDGEN_OPTIMIZATION_OK exactLakeDistances="+checked+" exactLakeAreaBlocks="+areaChecked);
    }
}
