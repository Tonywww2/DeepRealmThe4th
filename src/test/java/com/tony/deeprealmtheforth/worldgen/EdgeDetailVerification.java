package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.hydrology.ClimateSnapshot;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Coordinate-derived comparisons, not artistic mockups or game screenshots. */
public final class EdgeDetailVerification {
    private static int checks;
    private static void check(boolean condition,String message) {
        checks++;if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception {
        var climate=ClimateSnapshot.read(Path.of(args[0]));Path out=Path.of(args[1]);Files.createDirectories(out);
        StringBuilder report=new StringBuilder("EDGE_DETAIL_V4\n");
        for(long seed:new long[]{0,42,-739221}) {
            var before=BaseTerrain.naturalWorld(seed,SpiralParameters.DEFAULT);
            var after=BaseTerrain.naturalWorld(seed,SpiralParameters.DEFAULT,4);
            int oldLand=0,newLand=0,interior=0,eroded=0,maxSlope=0,oldSlope=0,biomeChanges=0;
            for(int x=-2048;x<2048;x+=8)for(int z=-2048;z<2048;z+=8) {
                var a=before.field().sample(x+.5,z+.5);var b=after.field().sample(x+.5,z+.5);
                if(a.land())oldLand++;if(b.land())newLand++;
                check(!b.land()||a.land(),"Erosion created new disconnected land");
                if(a.land()&&!b.land())eroded++;
                if(a.land()&&a.edgeDistance()>=40) {
                    check(b.land(),"An interior hole was introduced");interior++;
                    if((a.arm()==1||a.arm()==5)&&a.radius()>256) {
                        int delta=Math.abs(after.sample(x,z).top()-after.sample(x+1,z).top());
                        delta=Math.max(delta,Math.abs(after.sample(x,z).top()-after.sample(x,z+1).top()));
                        maxSlope=Math.max(maxSlope,delta);
                        oldSlope=Math.max(oldSlope,Math.abs(before.sample(x,z).top()-before.sample(x+1,z).top()));
                        oldSlope=Math.max(oldSlope,Math.abs(before.sample(x,z).top()-before.sample(x,z+1).top()));
                        if(!climate.biomeAt(before,x,z).equals(climate.biomeAt(after,x,z)))biomeChanges++;
                    }
                }
            }
            check(eroded>0,"Missing outer-edge detail");
            check(newLand>oldLand*.97,"More than 3% of original land was eroded");
            check(maxSlope<=8,"One-block terrain spike: "+maxSlope);
            check(biomeChanges>100,"Biome fine-scale perturbation was not exercised");
            // V4 also erodes ocean/void edges. Verify the unchanged three-block
            // protection algorithm against that NEW mask, not just legacy terrain.
            var finalTerrain=new TerrainProfile(seed,SpiralParameters.DEFAULT,climate,4);
            int oceanWater=0;
            for(int x=-1024;x<=1024;x+=8)for(int z=-1024;z<=1024;z+=8) {
                if(after.field().sample(x+.5,z+.5).arm()!=7)continue;
                var c=finalTerrain.sample(x,z);if(!c.wet())continue;oceanWater++;
                for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++) {
                    var n=finalTerrain.sample(x+dx,z+dz);
                    check(n.land()&&(n.top()>=c.fluidLevel()||n.wet()&&n.fluidLevel()==c.fluidLevel()),
                            "V4 ocean lost its three-block containment at "+x+","+z);
                }
            }
            check(oceanWater>100,"Insufficient V4 ocean coverage");
            for(int x=-64;x<=80;x+=2)for(int z=-64;z<=80;z+=2) {
                if(Math.hypot(x+.5-8,z+.5-8)>80)continue;
                var a=before.sample(x,z);var b=after.sample(x,z);
                check(a.land()==b.land()&&a.top()==b.top()&&a.bottom()==b.bottom(),"Central cliff geometry changed");
            }
            report.append("seed=").append(seed).append(" oldLand=").append(oldLand).append(" newLand=").append(newLand)
                    .append(" interiorPreserved=").append(interior).append(" eroded=").append(eroded)
                    .append(" maxCardinalRiseV3=").append(oldSlope).append(" maxCardinalRiseV4=").append(maxSlope)
                    .append(" changedBiomeSamples=").append(biomeChanges).append(" oceanWaterSamples=").append(oceanWater).append('\n');
        }
        var a=BaseTerrain.naturalWorld(42,SpiralParameters.DEFAULT);
        var b=BaseTerrain.naturalWorld(42,SpiralParameters.DEFAULT,4);
        int[] edge=findEdge(a);
        int[][] sites={{edge[0]-192,edge[1]-192,384},{-880,80,768},{544,-9888,768}};
        BufferedImage image=new BufferedImage(960,1590,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(new Color(20,29,39));g.fillRect(0,0,960,1590);
        g.setFont(new Font("Microsoft YaHei",Font.BOLD,24));g.setColor(Color.WHITE);
        g.drawString("边缘自然化 / 左 V3，右 V4 / 同坐标、种子 42",24,36);
        g.setFont(new Font("Microsoft YaHei",Font.PLAIN,16));
        g.drawString("真实 H0 与群系选择器取样，非游戏截图；颜色表示群系，明暗表示地形。",24,65);
        String[] names={"陆地外缘：窄边带侧蚀，内部不断裂","群系斑块：共享小尺度坐标扰动","山体侧翼：不规则山脚与低幅起伏"};
        for(int row=0;row<sites.length;row++) {
            int[] site=sites[row];int y=98+row*493;
            g.setColor(Color.WHITE);g.drawString(names[row]+" / "+site[0]+", "+site[1]+" / "+site[2]+" 格",24,y);
            g.drawImage(map(a,climate,site,row==1),24,y+12,null);
            g.drawImage(map(b,climate,site,row==1),488,y+12,null);
            report.append("previewOrigin=").append(java.util.Arrays.toString(site)).append('\n');
            if(row==1) {
                int[] statsA=coherence(a,climate,site),statsB=coherence(b,climate,site);
                // Isolated single-quart islands are not the intended form of detail.
                check(statsB[1]<statsB[2]/100,"Excessive isolated biome quart cells");
                report.append("biomeQuart V3 transitions/isolated/cells=").append(java.util.Arrays.toString(statsA))
                        .append(" V4=").append(java.util.Arrays.toString(statsB)).append('\n');
            }
        }
        g.dispose();ImageIO.write(image,"png",out.resolve("03-V3-V4边缘对照.png").toFile());
        report.append("checks=").append(checks).append('\n');
        Files.writeString(out.resolve("edge-verification.txt"),report);System.out.print(report);
    }
    private static int[] findEdge(BaseTerrain base) {
        for(double t=0;t<Math.PI*2;t+=.001) {
            int x=(int)(8+2048*Math.cos(t)),z=(int)(8+2048*Math.sin(t));var p=base.field().sample(x+.5,z+.5);
            if(p.arm()==5&&p.land()&&p.edgeDistance()>8&&p.edgeDistance()<12)return new int[]{x,z};
        }
        throw new AssertionError("No outer edge preview");
    }
    private static int[] coherence(BaseTerrain base,ClimateSnapshot climate,int[] site) {
        int transitions=0,isolated=0,cells=0;
        for(int x=site[0]+2;x<site[0]+site[2];x+=4)for(int z=site[1]+2;z<site[1]+site[2];z+=4) {
            if(!base.sample(x,z).land())continue;
            String id=climate.biomeAt(base,x,z);int different=0;
            for(int[] d:new int[][]{{-4,0},{4,0},{0,-4},{0,4}}) {
                if(!climate.biomeAt(base,x+d[0],z+d[1]).equals(id))different++;
            }
            transitions+=different;if(different==4)isolated++;cells++;
        }
        return new int[]{transitions,isolated,cells};
    }
    private static BufferedImage map(BaseTerrain base,ClimateSnapshot climate,int[] site,boolean biomes) {
        int size=448;BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);
        for(int px=0;px<size;px++)for(int pz=0;pz<size;pz++) {
            int x=site[0]+px*site[2]/size,z=site[1]+pz*site[2]/size;var c=base.sample(x,z);
            if(!c.land()){image.setRGB(px,pz,0x141e29);continue;}
            double shade=Math.max(.35,Math.min(1.3,.85+.09*(base.sample(x-2,z).top()-base.sample(x+2,z).top())
                    +.06*(base.sample(x,z-2).top()-base.sample(x,z+2).top())));
            Color color=biomes?Color.getHSBColor((climate.biomeAt(base,x,z).hashCode()&65535)/65536f,.45f,.90f):new Color(142,169,122);
            if(biomes)shade=.82+.18*shade;
            image.setRGB(px,pz,(clamp(color.getRed()*shade)<<16)|(clamp(color.getGreen()*shade)<<8)|clamp(color.getBlue()*shade));
        }
        return image;
    }
    private static int clamp(double v){return Math.max(0,Math.min(255,(int)v));}
}
