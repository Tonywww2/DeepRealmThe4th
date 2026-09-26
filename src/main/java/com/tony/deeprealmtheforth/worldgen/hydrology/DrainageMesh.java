package com.tony.deeprealmtheforth.worldgen.hydrology;

import java.math.BigDecimal;
import java.util.*;

/** Bounded offline Bowyer-Watson mesh. Near-zero predicates use exact decimal arithmetic on binary inputs. */
final class DrainageMesh {
    private record Triangle(int a,int b,int c) {}
    private DrainageMesh() {}
    static int[][] triangulate(double[] worldX,double[] worldZ) {
        int count=worldX.length;
        if(count>17000)throw new IllegalArgumentException("Offline triangulation limited to 17000 points");
        double[] x=Arrays.copyOf(worldX,count+3),z=Arrays.copyOf(worldZ,count+3);
        double minX=Arrays.stream(worldX).min().orElseThrow(),minZ=Arrays.stream(worldZ).min().orElseThrow();
        double span=Math.max(Arrays.stream(worldX).max().orElseThrow()-minX,Arrays.stream(worldZ).max().orElseThrow()-minZ);
        for(int i=0;i<count;i++){x[i]-=minX;z[i]-=minZ;}
        x[count]=-8*span;z[count]=-4*span;x[count+1]=9*span;z[count+1]=-4*span;x[count+2]=span*.5;z[count+2]=9*span;
        List<Triangle> triangles=new ArrayList<>();triangles.add(new Triangle(count,count+1,count+2));
        Integer[] insertion=new Integer[count];for(int i=0;i<count;i++)insertion[i]=i;
        Arrays.sort(insertion,Comparator.<Integer>comparingDouble(i->x[i]).thenComparingDouble(i->z[i]).thenComparingInt(i->i));
        for(int p:insertion) {
            Map<Long,Integer> boundary=new TreeMap<>();
            Iterator<Triangle> it=triangles.iterator();
            while(it.hasNext()) {
                Triangle t=it.next();
                if(inside(t,p,x,z)>0){edge(boundary,t.a,t.b);edge(boundary,t.b,t.c);edge(boundary,t.c,t.a);it.remove();}
            }
            for(var edge:boundary.entrySet())if(edge.getValue()==1){
                int a=(int)(edge.getKey()>>>32),b=(int)(long)edge.getKey();double orientation=orient(a,b,p,x,z);
                if(orientation>0)triangles.add(new Triangle(a,b,p));else if(orientation<0)triangles.add(new Triangle(b,a,p));
            }
        }
        List<SortedSet<Integer>> neighbors=new ArrayList<>();for(int i=0;i<count;i++)neighbors.add(new TreeSet<>());
        for(Triangle t:triangles)if(t.a<count&&t.b<count&&t.c<count){
            connect(neighbors,t.a,t.b);connect(neighbors,t.b,t.c);connect(neighbors,t.c,t.a);
        }
        int[][] result=new int[count][];for(int i=0;i<count;i++){
            result[i]=neighbors.get(i).stream().mapToInt(Integer::intValue).toArray();
            if(result[i].length==0)throw new IllegalStateException("Untriangulated point "+i);
        }
        return result;
    }
    private static void edge(Map<Long,Integer> map,int a,int b){long key=((long)Math.min(a,b)<<32)|(Math.max(a,b)&0xffffffffL);map.merge(key,1,Integer::sum);}
    private static void connect(List<SortedSet<Integer>> n,int a,int b){n.get(a).add(b);n.get(b).add(a);}
    private static double orient(int a,int b,int c,double[] x,double[] z){
        double left=(x[b]-x[a])*(z[c]-z[a]),right=(z[b]-z[a])*(x[c]-x[a]),d=left-right;
        if(Math.abs(d)>1e-13*(Math.abs(left)+Math.abs(right)))return d;
        return bd(x[b]).subtract(bd(x[a])).multiply(bd(z[c]).subtract(bd(z[a])))
                .subtract(bd(z[b]).subtract(bd(z[a])).multiply(bd(x[c]).subtract(bd(x[a])))).signum();
    }
    private static int inside(Triangle t,int p,double[] x,double[] z){
        double ax=x[t.a]-x[p],az=z[t.a]-z[p],bx=x[t.b]-x[p],bz=z[t.b]-z[p],cx=x[t.c]-x[p],cz=z[t.c]-z[p];
        double a=(ax*ax+az*az)*(bx*cz-bz*cx),b=(bx*bx+bz*bz)*(ax*cz-az*cx),c=(cx*cx+cz*cz)*(ax*bz-az*bx),det=a-b+c;
        // Bound the uncancelled products, not a/b/c after their cross products
        // have cancelled. Otherwise almost collinear inputs can skip the exact path.
        double permanent=(ax*ax+az*az)*(Math.abs(bx*cz)+Math.abs(bz*cx))
                +(bx*bx+bz*bz)*(Math.abs(ax*cz)+Math.abs(az*cx))
                +(cx*cx+cz*cz)*(Math.abs(ax*bz)+Math.abs(az*bx));
        if(Math.abs(det)>1e-12*permanent)return det>0?1:-1;
        BigDecimal ex=bd(x[t.a]).subtract(bd(x[p])),ez=bd(z[t.a]).subtract(bd(z[p])),
                fx=bd(x[t.b]).subtract(bd(x[p])),fz=bd(z[t.b]).subtract(bd(z[p])),
                gx=bd(x[t.c]).subtract(bd(x[p])),gz=bd(z[t.c]).subtract(bd(z[p]));
        return norm(ex,ez).multiply(cross(fx,fz,gx,gz)).subtract(norm(fx,fz).multiply(cross(ex,ez,gx,gz)))
                .add(norm(gx,gz).multiply(cross(ex,ez,fx,fz))).signum();
    }
    private static BigDecimal bd(double d){return new BigDecimal(d);}
    private static BigDecimal norm(BigDecimal x,BigDecimal z){return x.multiply(x).add(z.multiply(z));}
    private static BigDecimal cross(BigDecimal x,BigDecimal z,BigDecimal a,BigDecimal b){return x.multiply(b).subtract(z.multiply(a));}
}
