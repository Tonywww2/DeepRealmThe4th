package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.hydrology.*;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.*;
import java.awt.geom.Line2D;
import java.nio.file.*;
import java.util.*;
import java.util.stream.IntStream;

/** V2 morphology experiment; preserves the previous experiment and the live 0.1.5 terrain. */
public final class NaturalHydrologyVerification {
    private static int checks;
    private static void require(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args[1]);Files.createDirectories(out);
        ClimateSnapshot climate=ClimateSnapshot.read(Path.of(args[0]));Files.writeString(out.resolve("climate.tsv"),climate.serialize());
        StringBuilder report=new StringBuilder("NATURAL_HYDROLOGY_V2_LOCAL_TESTS\nproductionIntegration=false\nparentDomainStitching=NOT_SOLVED\n");
        List<DrainageGraph> examples=new ArrayList<>();List<RiverBanks> footprints=new ArrayList<>();
        double oldAnisotropy=0,newAnisotropy=0,oldGridBias=0,newGridBias=0;
        for(long seed:new long[]{42,0,-739221}){
            BaseTerrain old=new BaseTerrain(seed,SpiralParameters.DEFAULT),base=BaseTerrain.naturalPrototype(seed,SpiralParameters.DEFAULT);
            for(int x=-512;x<=512;x+=13)for(int z=-512;z<=512;z+=13){
                var a=old.sample(x,z);var b=base.sample(x,z);
                require(a.land()==b.land()&&a.arm()==b.arm(),"No new tears or changed spiral mask");
                if(a.arm()==3||a.arm()==7||Math.hypot(x-8,z-8)<=80)require(a.equals(b),"Keep center, infernal and ocean columns exact");
            }
            for(int scene=0;scene<3;scene++){
                var domain=HydrologyPrototypeVerification.domain(base,scene);
                long start=System.nanoTime();var graph=DrainageGraph.buildNatural(seed,base,climate,domain);
                HydrologyPrototypeVerification.verifyGraph(graph,base);verifyEdges(graph);
                RiverBanks banks=RiverBanks.build(seed,base,graph);verifyBanks(banks);
                if(scene==2)require(graph.nodes().stream().noneMatch(DrainageGraph.Node::wet),"Arid arm cannot invent water");
                double[] before=reliefStats(old,domain),after=reliefStats(base,domain);
                oldAnisotropy+=before[0];newAnisotropy+=after[0];
                require(after[1]<=6,"Prototype one-block slope bound: "+Arrays.toString(after));
                var grid=DrainageGraph.build(seed,base,climate,domain);
                double biasBefore=bias(grid),biasAfter=bias(graph);
                oldGridBias+=biasBefore;newGridBias+=biasAfter;
                double asym=banks.sections().stream().mapToDouble(s->Math.abs(s.left()-s.right())/(s.left()+s.right())).average().orElse(0);
                if(scene!=2)require(asym>.025,"Independent banks must not collapse to a mirrored ribbon");
                report.append(String.format(Locale.ROOT,"seed=%d scene=%d terrainDirectionality=%.4f->%.4f highlandFraction=%.4f->%.4f maxSlope=%.1f grid8Fold=%.4f->%.4f bankAsymmetry=%.4f patches=%d unsupportedBanks=%d pondingOver4=%d graph=%016x banks=%016x ms=%.1f\n",
                        seed,scene,before[0],after[0],before[2],after[2],after[1],biasBefore,biasAfter,asym,banks.patches().size(),
                        banks.patches().stream().filter(p->!p.supported()).count(),banks.patches().stream().filter(p->p.excessPonding()>4).count(),
                        graph.fingerprint(),banks.fingerprint(),(System.nanoTime()-start)/1e6));
                if(seed==42){examples.add(graph);footprints.add(banks);}
            }
        }
        require(newAnisotropy<oldAnisotropy,"Aggregate ridge directionality should decrease");
        require(newGridBias<oldGridBias,"Aggregate artificial eight-direction routing bias should decrease");
        BaseTerrain natural=BaseTerrain.naturalPrototype(42,SpiralParameters.DEFAULT);
        var original=examples.get(0);var rebuilt=DrainageGraph.buildNatural(42,natural,climate,original.domain());
        require(original.fingerprint()==rebuilt.fingerprint(),"Graph rebuild consistency");
        var rebuiltBanks=RiverBanks.build(42,natural,rebuilt);
        require(footprints.get(0).patches().equals(rebuiltBanks.patches())
                &&footprints.get(0).sections().equals(rebuiltBanks.sections()),"Full bank reconstruction consistency");
        var expected=IntStream.range(0,2048).mapToObj(i->natural.sample(i*19-16000,i*37-32000)).toList();
        IntStream.range(0,2048).parallel().forEach(j->{int i=2047-j;if(!expected.get(i).equals(natural.sample(i*19-16000,i*37-32000)))throw new AssertionError("Concurrent / cache eviction sampling");});
        var dry=DrainageGraph.buildNatural(42,natural,climate.scaleRain(.5),original.domain());
        for(int i=0;i<dry.nodes().size();i++){
            require(dry.nodes().get(i).downstream()==original.nodes().get(i).downstream(),"Rain does not change topology");
            require(dry.nodes().get(i).discharge()<=original.nodes().get(i).discharge()+1e-6,"Less rain cannot add flow");
        }
        NaturalHydrologyRenderer.render(out,examples,footprints);
        report.append("V2_LOCAL_TESTS_OK assertions=").append(checks)
                .append("\nINTEGRATION_GATE=FAIL: global parent stitching, ponding and block-water remain unresolved\n");
        Files.writeString(out.resolve("verification.txt"),report.toString());System.out.println(report);
    }
    private static void verifyEdges(DrainageGraph graph){
        var nodes=graph.nodes();List<Integer> edges=new ArrayList<>();
        for(int i=0;i<nodes.size();i++)if(nodes.get(i).wet()&&nodes.get(i).downstream()>=0)edges.add(i);
        for(int a=0;a<edges.size();a++)for(int b=a+1;b<edges.size();b++){
            int i=edges.get(a),j=edges.get(b),ip=nodes.get(i).downstream(),jp=nodes.get(j).downstream();
            if(i==jp||j==ip||ip==jp)continue;
            var n=nodes.get(i);var p=nodes.get(ip);var e=nodes.get(j);var f=nodes.get(jp);
            require(!Line2D.linesIntersect(n.x(),n.z(),p.x(),p.z(),e.x(),e.z(),f.x(),f.z()),"Unmerged channel crossing");
        }
    }
    private static void verifyBanks(RiverBanks banks){
        for(var p:banks.patches()){
            require(p.levelB()<=p.levelA()+1e-8,"Shared monotone longitudinal water");
            require(p.water().size()>=3&&p.bank().size()>=3,"No degenerate bank proposal");
            for(var v:p.water())require(Double.isFinite(v.x())&&Double.isFinite(v.z()),"Finite geometry");
            for(int i=0;i<p.water().size();i++)for(int j=i+2;j<p.water().size();j++){
                if(i==0&&j==p.water().size()-1)continue;
                var a=p.water().get(i);var b=p.water().get((i+1)%p.water().size());var c=p.water().get(j);var d=p.water().get((j+1)%p.water().size());
                require(!Line2D.linesIntersect(a.x(),a.z(),b.x(),b.z(),c.x(),c.z(),d.x(),d.z()),"Self-intersecting bank polygon");
            }
        }
        for(var s:banks.sections())require(s.left()>0&&s.right()>0&&s.leftBank()>s.left()&&s.rightBank()>s.right(),"Positive water width and separate banks");
    }
    private static double[] reliefStats(BaseTerrain base,DrainageGraph.Domain d){
        double xx=0,zz=0,xz=0,max=0,high=0,count=0;for(int x=d.x();x<d.x()+d.span();x+=17)for(int z=d.z();z<d.z()+d.span();z+=17){
            var c=base.sample(x,z);if(!c.land()||c.edgeDistance()<60)continue;
            double dx=(base.sample(x+8,z).top()-base.sample(x-8,z).top())/16.0;
            double dz=(base.sample(x,z+8).top()-base.sample(x,z-8).top())/16.0;
            xx+=dx*dx;zz+=dz*dz;xz+=dx*dz;
            count++;if(c.top()-base.layout().baseHeight(Math.hypot(x-8,z-8))>30)high++;
            max=Math.max(max,Math.max(Math.abs(base.sample(x+1,z).top()-c.top()),Math.abs(base.sample(x,z+1).top()-c.top())));
        }
        return new double[]{Math.hypot(xx-zz,2*xz)/Math.max(1e-9,xx+zz),max,high/Math.max(1,count)};
    }
    private static double bias(DrainageGraph graph){double a=0,b=0,w=0;
        for(var n:graph.nodes())if(n.wet()&&n.downstream()>=0){var p=graph.nodes().get(n.downstream());double angle=Math.atan2(p.z()-n.z(),p.x()-n.x()),len=Math.hypot(p.x()-n.x(),p.z()-n.z());a+=len*Math.cos(8*angle);b+=len*Math.sin(8*angle);w+=len;}
        return Math.hypot(a,b)/Math.max(1e-9,w);
    }
}
