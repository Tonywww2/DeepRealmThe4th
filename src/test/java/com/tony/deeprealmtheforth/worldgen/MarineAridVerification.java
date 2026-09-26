package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.hydrology.ClimateSnapshot;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Actual H0/final-column samples, no hand-drawn desired results. */
public final class MarineAridVerification {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        var climate=ClimateSnapshot.read(Path.of(args[0]));var out=Path.of(args[1]);Files.createDirectories(out);
        var report=new StringBuilder("MARINE_ARID_V5\n");
        for(long seed:new long[]{0,42,-739221}) {
            var old=BaseTerrain.naturalWorld(seed,SpiralParameters.DEFAULT,4);
            var base=BaseTerrain.naturalWorld(seed,SpiralParameters.DEFAULT,5);
            var finalTerrain=new TerrainProfile(seed,SpiralParameters.DEFAULT,climate,5);
            int ocean=0,wet=0,deep=0,beach=0,plateau=0,mountain=0,flat=0,maxSlope=0,minBed=64;
            for(int x=-4096;x<=4096;x+=16)for(int z=-4096;z<=4096;z+=16) {
                var c=base.sample(x,z);var previous=old.sample(x,z);
                check(c.land()==previous.land(),"V5 changed arm footprint");
                if(c.arm()==3||c.arm()==5)check(c.equals(previous),"Unrelated arm H0 changed");
                if(!c.land()||c.edgeDistance()<80||Math.hypot(x-8,z-8)<240)continue;
                int slope=Math.max(Math.abs(c.top()-base.sample(x+1,z).top()),Math.abs(c.top()-base.sample(x,z+1).top()));
                if(c.arm()==1||c.arm()==7){maxSlope=Math.max(maxSlope,slope);check(slope<=8,"V5 one-block spike "+x+","+z+" delta="+slope);}
                if(c.arm()==7) {
                    ocean++;if(c.wet()){wet++;minBed=Math.min(minBed,c.top());if(c.top()<40)deep++;}
                    else if(c.beach()>.6)beach++;
                }
                if(c.theme()==TerrainProfile.Theme.PLATEAU){plateau++;if(slope<=1)flat++;}
                if(c.theme()==TerrainProfile.Theme.ARID_MOUNTAIN)mountain++;
            }
            int waterChecks=0;
            for(int x=-1536;x<=1536;x+=16)for(int z=-1536;z<=1536;z+=16) {
                if(base.field().sample(x+.5,z+.5).arm()!=7)continue;
                var c=finalTerrain.sample(x,z);if(!c.wet())continue;waterChecks++;
                for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++) {
                    var n=finalTerrain.sample(x+dx,z+dz);
                    check(n.land()&&(n.top()>=c.fluidLevel()||n.wet()&&n.fluidLevel()==c.fluidLevel()),"V5 ocean containment");
                }
            }
            for(int x=-64;x<=80;x+=2)for(int z=-64;z<=80;z+=2) {
                if(Math.hypot(x+.5-8,z+.5-8)>80)continue;
                var a=old.sample(x,z);var b=base.sample(x,z);
                check(a.land()==b.land()&&a.top()==b.top()&&a.bottom()==b.bottom(),"Center changed");
            }
            check(wet>ocean*.35&&wet<ocean*.9,"Ocean land/water diversity");
            check(deep>wet*.15&&minBed<10,"Missing deep basins deep="+deep+" wet="+wet+" min="+minBed);check(beach>100,"Missing wide beach samples");
            check(plateau>200&&mountain>100&&flat>plateau*.65,"Missing plateau/mountain/flat cap");
            check(waterChecks>100,"Insufficient sea-wall tests");
            // Exercise the new cell caches with negative/chunk-boundary coordinates in different worker orders.
            var parallel=BaseTerrain.naturalWorld(seed,SpiralParameters.DEFAULT,5);
            var points=new ArrayList<int[]>();
            for(int arm:new int[]{1,7}) {
                double[] center=base.layout().armCenter(arm,4096);
                for(int dx=-320;dx<=320;dx+=32)for(int dz=-320;dz<=320;dz+=32)
                    points.add(new int[]{Math.floorDiv((int)center[0]+dx,16)*16-1,(int)center[1]+dz});
            }
            var expected=points.stream().map(p->base.sample(p[0],p[1])).toList();
            var workers=java.util.concurrent.Executors.newFixedThreadPool(4);
            try {
                var futures=new ArrayList<java.util.concurrent.Future<TerrainProfile.Column>>();
                for(int i=points.size()-1;i>=0;i--){var p=points.get(i);futures.add(workers.submit(()->parallel.sample(p[0],p[1])));}
                for(int i=0;i<futures.size();i++)check(expected.get(expected.size()-1-i).equals(futures.get(i).get()),"Parallel marine/arid H0 mismatch");
            }finally{workers.shutdownNow();}
            report.append("seed=").append(seed).append(" ocean=").append(ocean).append(" wet=").append(wet)
                    .append(" deep=").append(deep).append(" wideBeach=").append(beach).append(" minBed=").append(minBed)
                    .append(" plateau=").append(plateau).append(" flatCap=").append(flat).append(" aridMountain=").append(mountain)
                    .append(" maxCardinalRise=").append(maxSlope).append(" waterChecks=").append(waterChecks).append('\n');
        }
        var old=BaseTerrain.naturalWorld(42,SpiralParameters.DEFAULT,4);
        var base=BaseTerrain.naturalWorld(42,SpiralParameters.DEFAULT,5);
        int[] coast=find(base,7,false),mesa=find(base,1,true);
        int[][] sites={{coast[0]-768,coast[1]-768,1536},{coast[0]-256,coast[1]-256,512},{mesa[0]-384,mesa[1]-384,768}};
        var image=new BufferedImage(1060,1730,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        g.setColor(new Color(20,29,39));g.fillRect(0,0,1060,1730);g.setColor(Color.WHITE);
        g.setFont(new Font("Microsoft YaHei",Font.BOLD,23));g.drawString("海洋 / 干旱山地：左 V4，右 V5；种子 42",24,36);
        g.setFont(new Font("Microsoft YaHei",Font.PLAIN,16));g.drawString("实际地形模型取样，非游戏截图；水下显示海底高度，不是水面。",24,65);
        String[] names={"大陆架、深海盆与岛群","海岸近景：宽沙滩与岩岸","台地与干旱山脊"};
        for(int i=0;i<sites.length;i++) {
            int y=100+i*535;g.setColor(Color.WHITE);g.drawString(names[i]+" / "+Arrays.toString(sites[i]),24,y);
            g.drawImage(map(old,sites[i]),24,y+12,null);g.drawImage(map(base,sites[i]),542,y+12,null);
            report.append("preview=").append(Arrays.toString(sites[i])).append('\n');
        }
        g.dispose();ImageIO.write(image,"png",out.resolve("V4-V5海洋与台地.png").toFile());
        sections(out.resolve("V5高度剖面.png"),old,base,sites);
        report.append("checks=").append(checks).append('\n');Files.writeString(out.resolve("verification.txt"),report);System.out.print(report);
    }
    private static int[] find(BaseTerrain base,int arm,boolean mesa) {
        for(int x=-5000;x<=5000;x+=32)for(int z=-5000;z<=5000;z+=32) {
            var c=base.sample(x,z);if(!c.land()||c.arm()!=arm||c.edgeDistance()<550)continue;
            if(mesa?c.theme()==TerrainProfile.Theme.PLATEAU:!c.wet()&&c.beach()>.7&&c.top()<69)return new int[]{x,z};
        }
        throw new AssertionError("No preview site");
    }
    private static BufferedImage map(BaseTerrain base,int[] site) {
        int size=494;var image=new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);
        for(int i=0;i<size;i++)for(int j=0;j<size;j++) {
            int x=site[0]+i*site[2]/size,z=site[1]+j*site[2]/size;var c=base.sample(x,z);
            if(!c.land()){image.setRGB(i,j,0x141d27);continue;}
            Color color=c.wet()?new Color(31,80+Math.max(0,c.top())/2,137+Math.max(0,c.top()))
                    :c.arm()==1?c.theme()==TerrainProfile.Theme.PLATEAU?new Color(202,130,74):new Color(179,162,107)
                    :c.beach()>.5?new Color(226,211,155):new Color(119,160,105);
            double shade=Math.max(.4,Math.min(1.4,.85+.10*(base.sample(x-2,z).top()-base.sample(x+2,z).top())
                    +.07*(base.sample(x,z-2).top()-base.sample(x,z+2).top())));
            image.setRGB(i,j,(clamp(color.getRed()*shade)<<16)|(clamp(color.getGreen()*shade)<<8)|clamp(color.getBlue()*shade));
        }
        return image;
    }
    private static int clamp(double v){return Math.max(0,Math.min(255,(int)v));}
    private static void sections(Path path,BaseTerrain old,BaseTerrain base,int[][] sites)throws Exception {
        var image=new BufferedImage(1100,650,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        g.setColor(new Color(20,29,39));g.fillRect(0,0,1100,650);g.setColor(Color.WHITE);
        g.setFont(new Font("Microsoft YaHei",Font.PLAIN,19));g.drawString("实际 H0 剖面 / 灰色 V4，橙色 V5，蓝色海平面 / 种子 42",26,34);
        for(int row=0;row<2;row++) {
            int[] site=sites[row==0?0:2];int z=site[1]+site[2]/2,top=72+row*290,bottom=top+220;
            g.setFont(new Font("Microsoft YaHei",Font.PLAIN,15));g.setColor(Color.WHITE);
            g.drawString((row==0?"海洋":"台地")+"  z="+z+" / x="+site[0]+" … "+(site[0]+site[2]),26,top-10);
            for(int y=-40;y<=180;y+=40) {
                g.setColor(new Color(54,64,73));g.drawLine(65,bottom-y,1060,bottom-y);g.setColor(Color.LIGHT_GRAY);g.drawString("Y"+y,15,bottom-y+5);
            }
            g.setColor(new Color(67,147,211));g.drawLine(65,bottom-64,1060,bottom-64);
            for(int version=0;version<2;version++) {
                var terrain=version==0?old:base;g.setColor(version==0?new Color(153,161,170):new Color(246,176,87));
                g.setStroke(new BasicStroke(version==0?1.4f:2f));int previousY=0;boolean connected=false;
                for(int px=0;px<995;px++) {
                    int x=site[0]+px*site[2]/995;var c=terrain.sample(x,z);
                    if(c.land()&&connected)g.drawLine(64+px,previousY,65+px,bottom-c.top());
                    previousY=bottom-c.top();connected=c.land();
                }
            }
        }
        g.dispose();ImageIO.write(image,"png",path.toFile());
    }
}
