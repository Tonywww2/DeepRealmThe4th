package com.tony.deeprealmtheforth.worldgen.hydrology;

import com.tony.deeprealmtheforth.worldgen.terrain.BaseTerrain;
import com.tony.deeprealmtheforth.worldgen.terrain.SeededNoise;
import java.util.*;

/** Offline bank footprint proposal. Beveled junctions and tapered heads, no circular caps or spline smoothing. */
public final class RiverBanks {
    public record Point(double x,double z) {}
    public record Section(double x,double z,double water,double left,double right,double leftBank,double rightBank) {}
    public record Patch(List<Point> water,List<Point> bank,double levelA,double levelB,long reach,boolean junction,
                        boolean supported,double excessPonding) {
        public Patch { water=List.copyOf(water);bank=List.copyOf(bank); }
    }
    private final List<Patch> patches;
    private final List<Section> sections;
    private final long fingerprint;
    private RiverBanks(List<Patch> patches,List<Section> sections) {
        this.patches=List.copyOf(patches);this.sections=List.copyOf(sections);
        long h=0xcbf29ce484222325L;
        for(Patch p:patches)for(Point v:p.water)for(double d:new double[]{v.x,v.z})h=(h^Double.doubleToLongBits(d))*0x100000001b3L;
        fingerprint=h;
    }
    public List<Patch> patches(){return patches;}
    public List<Section> sections(){return sections;}
    public long fingerprint(){return fingerprint;}

    public static RiverBanks build(long seed,BaseTerrain base,DrainageGraph graph) {
        var nodes=graph.nodes();int count=nodes.size();
        double[] fromHead=new double[count];int[] sequence=graph.topological();
        for(int i=sequence.length-1;i>=0;i--){int k=sequence[i];var n=nodes.get(k);
            if(n.wet()&&n.downstream()>=0){var p=nodes.get(n.downstream());fromHead[n.downstream()]=Math.max(fromHead[n.downstream()],fromHead[k]+Math.hypot(n.x()-p.x(),n.z()-p.z()));}}
        List<List<Point>> waterJoins=new ArrayList<>(),bankJoins=new ArrayList<>();
        for(int i=0;i<count;i++){waterJoins.add(new ArrayList<>());bankJoins.add(new ArrayList<>());}
        List<Patch> result=new ArrayList<>();List<Section> samples=new ArrayList<>();
        for(int k=0;k<count;k++){
            var n=nodes.get(k);if(!n.wet()||n.downstream()<0)continue;int down=n.downstream();var p=nodes.get(down);
            double dx=p.x()-n.x(),dz=p.z()-n.z(),length=Math.hypot(dx,dz),nx=-dz/length,nz=dx/length;
            // Unequal sample spacing is fixed by reach ID, not by generation order.
            int steps=Math.max(2,(int)Math.ceil(length/(8+3*SeededNoise.unit(n.id()))));
            Section previous=null;
            for(int s=0;s<=steps;s++){
                double t=s/(double)steps,x=n.x()+dx*t,z=n.z()+dz*t;
                double q=n.discharge()+(p.discharge()-n.discharge())*t;
                double taper=Math.min(1,.16+(fromHead[k]+length*t)/42);
                double width=DrainageGraph.width(q)*taper;
                double confinement=.83+.27*(.5+SeededNoise.fractal(seed^81591,x,z,89,2));
                // World-space bank geology at independent offsets; no shared mirrored cross-section.
                double left=width*confinement*roughness(seed^78131,x+nx*19,z+nz*19);
                double right=width*confinement*roughness(seed^19661,x-nx*19,z-nz*19);
                // Reduce accidental contact with unrelated channels. This local cap is
                // not yet a proof that all expanded polygons remain disjoint.
                double separation=Double.POSITIVE_INFINITY;
                for(int other=0;other<count;other++){
                    var e=nodes.get(other);int ed=e.downstream();
                    if(!e.wet()||ed<0||other==k||other==down||ed==k||ed==down)continue;
                    var f=nodes.get(ed);double vx=f.x()-e.x(),vz=f.z()-e.z();
                    double u=Math.max(0,Math.min(1,((x-e.x())*vx+(z-e.z())*vz)/(vx*vx+vz*vz)));
                    separation=Math.min(separation,Math.hypot(x-e.x()-u*vx,z-e.z()-u*vz));
                }
                left=Math.min(left,separation*.42);right=Math.min(right,separation*.42);
                double level=n.water()+(p.water()-n.water())*t;
                double lh=base.sample((int)Math.floor(x+nx*(left+12)),(int)Math.floor(z+nz*(left+12))).top();
                double rh=base.sample((int)Math.floor(x-nx*(right+12)),(int)Math.floor(z-nz*(right+12))).top();
                double lb=left+2.5+Math.min(13,Math.max(0,lh-level)*.7)*(1.15-confinement);
                double rb=right+3+Math.min(15,Math.max(0,rh-level)*.6)*(1.22-confinement);
                Section section=new Section(x,z,level,left,right,lb,rb);samples.add(section);
                if(previous!=null){
                    var water=quad(previous,section,nx,nz,false);
                    var bank=quad(previous,section,nx,nz,true);
                    result.add(patch(base,water,bank,previous.water,section.water,n.id(),false));
                }
                if(s==0){addEnds(waterJoins.get(k),section,nx,nz,false);addEnds(bankJoins.get(k),section,nx,nz,true);}
                if(s==steps){addEnds(waterJoins.get(down),section,nx,nz,false);addEnds(bankJoins.get(down),section,nx,nz,true);}
                previous=section;
            }
        }
        for(int k=0;k<count;k++)if(waterJoins.get(k).size()>=4){var n=nodes.get(k);
            var waterHull=hull(waterJoins.get(k));var bankHull=hull(bankJoins.get(k));
            // Exactly straight joins already share an edge; a zero-area cap adds nothing.
            if(waterHull.size()>=3&&bankHull.size()>=3)
                result.add(patch(base,waterHull,bankHull,n.water(),n.water(),n.id(),true));}
        return new RiverBanks(result,samples);
    }
    private static double roughness(long seed,double x,double z){
        // Broad bars and rock pinches plus restrained short-scale notches. No white-noise serration.
        return Math.max(.53,Math.min(1.48,1+.42*SeededNoise.fractal(seed,x,z,47,2)
                +.19*SeededNoise.fractal(seed^9811,x,z,13,2)));
    }
    private static List<Point> quad(Section a,Section b,double nx,double nz,boolean bank){
        double al=bank?a.leftBank:a.left,ar=bank?a.rightBank:a.right,bl=bank?b.leftBank:b.left,br=bank?b.rightBank:b.right;
        return List.of(new Point(a.x+nx*al,a.z+nz*al),new Point(b.x+nx*bl,b.z+nz*bl),
                new Point(b.x-nx*br,b.z-nz*br),new Point(a.x-nx*ar,a.z-nz*ar));
    }
    private static void addEnds(List<Point> list,Section s,double nx,double nz,boolean bank){
        double l=bank?s.leftBank:s.left,r=bank?s.rightBank:s.right;
        list.add(new Point(s.x+nx*l,s.z+nz*l));list.add(new Point(s.x-nx*r,s.z-nz*r));
    }
    private static Patch patch(BaseTerrain base,List<Point> water,List<Point> bank,double a,double b,long reach,boolean junction){
        boolean supported=true;double pond=0;
        for(int i=0;i<bank.size();i++){
            Point p=bank.get(i),q=bank.get((i+1)%bank.size());int steps=Math.max(1,(int)Math.ceil(Math.hypot(q.x-p.x,q.z-p.z)));
            for(int s=0;s<=steps;s++){
                double t=s/(double)steps,x=p.x+(q.x-p.x)*t,z=p.z+(q.z-p.z)*t;
                var mask=base.field().sample(Math.floor(x)+.5,Math.floor(z)+.5);
                if(!mask.land()||(mask.arm()!=1&&mask.arm()!=5))supported=false;
            }
        }
        for(Point p:water)pond=Math.max(pond,Math.max(a,b)-base.sample((int)Math.floor(p.x),(int)Math.floor(p.z)).top());
        return new Patch(water,bank,a,b,reach,junction,supported,pond);
    }
    private static List<Point> hull(List<Point> points){
        List<Point> sorted=points.stream().distinct().sorted(Comparator.comparingDouble(Point::x).thenComparingDouble(Point::z)).toList();
        if(sorted.size()<3)return sorted;
        List<Point> hull=new ArrayList<>();
        for(Point p:sorted){while(hull.size()>=2&&turn(hull.get(hull.size()-2),hull.get(hull.size()-1),p)<=0)hull.remove(hull.size()-1);hull.add(p);}
        int lower=hull.size();
        for(int i=sorted.size()-2;i>=0;i--){Point p=sorted.get(i);while(hull.size()>lower&&turn(hull.get(hull.size()-2),hull.get(hull.size()-1),p)<=0)hull.remove(hull.size()-1);hull.add(p);}
        hull.remove(hull.size()-1);return List.copyOf(hull);
    }
    private static double turn(Point a,Point b,Point c){return (b.x-a.x)*(c.z-a.z)-(b.z-a.z)*(c.x-a.x);}
}
