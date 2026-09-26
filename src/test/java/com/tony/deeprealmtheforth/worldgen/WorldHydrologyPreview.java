package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import com.tony.deeprealmtheforth.worldgen.hydrology.WatershedRivers;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Draws the same final columns used by fillFromNoise and height queries. */
final class WorldHydrologyPreview {
    private static final Font TITLE=new Font("Microsoft YaHei",Font.BOLD,25),LABEL=new Font("Microsoft YaHei",Font.PLAIN,17);
    static void render(Path folder,TerrainProfile terrain,java.util.List<WatershedRivers.Model> models)throws Exception {
        String version="V"+terrain.generationVersion();
        BufferedImage image=new BufferedImage(1880,800,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setColor(new Color(19,28,38));g.fillRect(0,0,image.getWidth(),image.getHeight());
        g.setColor(Color.WHITE);g.setFont(TITLE);g.drawString(version+" 最终地形列 · 全局主干—支流与内流湖",24,40);
        g.setFont(LABEL);g.drawString("种子 42 / 实际生成模型取样，非游戏截图。蓝色为列模型中的水，不是额外画上的河线。",24,72);
        int[][] origins={{3232,-3648},{544,-9888},{-5312,1568}};String[] names={"湿润温带 / 丛林","远端山地","干旱股：按群系供水"};
        for(int i=0;i<3;i++){
            g.setColor(Color.WHITE);g.drawString(names[i]+" / 2048 格",24+i*620,106);
            g.drawImage(map(terrain,origins[i][0],origins[i][1],2048,592),24+i*620,122,null);
        }
        g.setColor(Color.LIGHT_GRAY);g.drawString("区块只读取全局模型；保留中心 Y60 断崖、陆地范围、低径向增幅和海洋保护。",24,752);g.dispose();
        ImageIO.write(image,"png",folder.resolve("01-"+version+"最终地形列.png").toFile());
        var selected=models.stream().filter(WatershedRivers.Model::accepted)
                .sorted(Comparator.comparingInt((WatershedRivers.Model m)->m.reaches().size()).reversed()).limit(3).toList();
        image=new BufferedImage(1880,800,BufferedImage.TYPE_INT_RGB);g=image.createGraphics();
        g.setColor(new Color(19,28,38));g.fillRect(0,0,image.getWidth(),image.getHeight());g.setColor(Color.WHITE);g.setFont(TITLE);
        g.drawString(version+" 河谷近景 · 实际岸坡与河床高度",24,40);g.setFont(LABEL);
        g.drawString("没有给所有拐点统一倒圆；河线沿固定排水走廊细化，两岸不同，水位随下游变化。",24,74);
        for(int i=0;i<selected.size();i++) {
            var model=selected.get(i);var lake=model.lake();int x=(int)lake.x()-256,z=(int)lake.z()-256;
            g.setColor(Color.WHITE);g.drawString("终点附近 / 起点 "+x+", "+z+" / 512 格",24+i*620,108);
            g.drawImage(map(terrain,x,z,512,592),24+i*620,122,null);
        }
        g.setColor(Color.LIGHT_GRAY);g.drawString("本图展示最终地形列；流水 tick、冻结与结构保护结果请分别查看运行时报告。",24,752);g.dispose();
        ImageIO.write(image,"png",folder.resolve("02-"+version+"河谷近景.png").toFile());
    }
    private static BufferedImage map(TerrainProfile terrain,int ox,int oz,int span,int size) {
        int resolution=Math.min(span,size);BufferedImage result=new BufferedImage(resolution,resolution,BufferedImage.TYPE_INT_RGB);
        int offset=Math.max(1,span/resolution);
        for(int px=0;px<resolution;px++)for(int pz=0;pz<resolution;pz++) {
            int x=ox+px*span/resolution,z=oz+pz*span/resolution;var c=terrain.sample(x,z);
            if(!c.land()){result.setRGB(px,pz,0x141e29);continue;}
            double shade=Math.max(.40,Math.min(1.35, .88+.075*(terrain.sample(x-offset,z).top()-terrain.sample(x+offset,z).top())
                    +.04*(terrain.sample(x,z-offset).top()-terrain.sample(x,z+offset).top())));
            int r=c.arm()==1?203:129,b=c.arm()==1?120:103,green=c.arm()==1?177:159;
            double rise=c.top()-terrain.layout().baseHeight(Math.hypot(x-8,z-8));
            double stone=Math.max(0,Math.min(.75,(rise-26)/55));
            r=(int)(r+(186-r)*stone);green=(int)(green+(189-green)*stone);b=(int)(b+(178-b)*stone);
            if(c.wet()){r=48;green=113;b=137;shade=.80+.035*Math.min(6,c.fluidLevel()-c.top());}
            else if(c.riverDistance()<3){r=174;green=166;b=134;}
            result.setRGB(px,pz,(clamp(r*shade)<<16)|(clamp(green*shade)<<8)|clamp(b*shade));
        }
        if(resolution==size)return result;
        BufferedImage enlarged=new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);Graphics2D g=enlarged.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(result,0,0,size,size,null);g.dispose();return enlarged;
    }
    private static int clamp(double value){return Math.max(0,Math.min(255,(int)value));}
}
