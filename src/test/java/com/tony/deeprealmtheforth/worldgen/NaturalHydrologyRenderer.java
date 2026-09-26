package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.hydrology.*;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/** Same-coordinate scientific previews; terrain pixels sample the actual continuous H0, not a coarse display mesh. */
final class NaturalHydrologyRenderer {
    private static final Color BG=new Color(19,27,37),INK=new Color(227,235,240);
    private static final String[] TITLES={"湿润温带 / 丛林","远端山地","干旱陆地股"};
    private record View(double x,double z,int span,int pixels) {}
    static void render(Path out,List<DrainageGraph> graphs,List<RiverBanks> banks)throws Exception{
        BaseTerrain old=new BaseTerrain(42,SpiralParameters.DEFAULT),natural=BaseTerrain.naturalPrototype(42,SpiralParameters.DEFAULT);
        BufferedImage[] terrain=new BufferedImage[3];
        var comparison=canvas(1880,1390);var g=graphics(comparison);
        text(g,"地形 V2 · 同种子、同坐标、同光照",24,38,27,true);
        text(g,"上：旧连续地形 H0　下：有限分叉脊线 + 不同侧坡 + 世界坐标起伏；漩涡陆地轮廓保持不变",24,72,17,false);
        for(int scene=0;scene<3;scene++){
            var d=graphs.get(scene).domain();View view=new View(d.x()+d.span()/2.0,d.z()+d.span()/2.0,d.span(),592);int x=24+scene*620;
            text(g,TITLES[scene]+" / 2048 格",x,104,20,true);
            g.drawImage(ground(old,view),x,121,null);
            text(g,"V2 / seed 42 / 起点 "+d.x()+", "+d.z(),x,746,16,true);
            terrain[scene]=ground(natural,view);g.drawImage(terrain[scene],x,762,null);
        }
        text(g,"实际连续高度取样，不是 AI 概念图，也没有用渲染网格插值掩盖条纹。离线候选，尚未改动正式世界。",24,1380,16,true);
        g.dispose();save(comparison,out.resolve("01-地形同坐标对照.png"));
        var full=canvas(1880,820);g=graphics(full);
        text(g,"V2 完整河网 · 地形决定走向，群系气候决定来水",24,38,26,true);
        text(g,"非规则三角网；没有统一圆帽和椭圆终点湖。橙线标出分析积水超过 4 格的待处理河段。",24,72,17,false);
        for(int scene=0;scene<3;scene++){
            var d=graphs.get(scene).domain();View view=new View(d.x()+d.span()/2.0,d.z()+d.span()/2.0,d.span(),592);int x=24+scene*620;
            text(g,TITLES[scene],x,105,20,true);g.drawImage(terrain[scene],x,123,null);
            footprint(g,banks.get(scene),view,x,123,true);
            text(g,scene==2?"无降水 → 不凭空制造常年河流":"蓝：水面提案　米色：岸带　橙：深洼警告",x,747,16,false);
        }
        text(g,"仍是有限域形态实验：父域接缝、湖泊收支、河床刻蚀和方块水未验收。不要把蓝色直接当作可生成的水。",24,792,17,true);
        g.dispose();save(full,out.resolve("02-V2完整河网.png"));

        var close=canvas(1880,1430);g=graphics(close);
        text(g,"河道轮廓 V2 · 同一排水图的受控形态对比",24,40,26,true);
        text(g,"上：对称宽度 + 圆帽参照；下：两岸独立、岩口收窄、楔形源头与折角汇口。上排不是旧存档截图。",24,73,17,false);
        int[] cases={0,1,0};
        for(int panel=0;panel<3;panel++){
            int scene=cases[panel];var graph=graphs.get(scene);var center=detail(graph,panel==2);
            View view=new View(center.x(),center.z(),384,592);int x=24+panel*620;
            BufferedImage land=ground(natural,view);text(g,panel==2?"源头 / 384 格":"汇流 / 384 格",x,109,20,true);
            g.drawImage(land,x,126,null);roundControl(g,graph,view,x,126);
            text(g,"V2 轮廓提案 / 中心 "+(int)center.x()+", "+(int)center.z(),x,754,16,true);
            g.drawImage(land,x,773,null);footprint(g,banks.get(scene),view,x,773,false);
        }
        text(g,"下排是算法生成的岸线多边形，尚未模拟侵蚀或方块水；通过形态确认后仍需接缝与水体安全验收。",24,1412,17,true);
        g.dispose();save(close,out.resolve("03-河岸形态受控对照.png"));
    }
    private static DrainageGraph.Node detail(DrainageGraph graph,boolean head){
        var d=graph.domain();DrainageGraph.Node best=null;double score=-Double.MAX_VALUE;
        for(var n:graph.nodes())if(n.wet()&&n.downstream()>=0&&(head?n.incoming()==0:n.incoming()>=2)
                &&n.x()>d.x()+210&&n.x()<d.x()+d.span()-210&&n.z()>d.z()+210&&n.z()<d.z()+d.span()-210){
            double value=(head?0:Math.log(n.discharge()))-Math.max(0,n.water()-n.height())*.5;
            if(value>score){score=value;best=n;}
        }
        if(best==null)throw new IllegalStateException("No nontrivial detail sample");return best;
    }
    private static BufferedImage ground(BaseTerrain base,View v){
        // Do not oversample integer block coordinates: repeated samples cause false checker shading.
        int resolution=Math.min(v.pixels,v.span),n=resolution+2;double step=v.span/(double)resolution;double[][] h=new double[n][n];int[][] arm=new int[n][n];
        for(int j=0;j<n;j++)for(int i=0;i<n;i++){
            int x=(int)Math.floor(v.x-v.span*.5+(i-1)*step),z=(int)Math.floor(v.z-v.span*.5+(j-1)*step);
            var s=base.shape(x,z);h[j][i]=s.point().land()?s.top():-64;arm[j][i]=s.point().land()?s.point().arm():0;
        }
        var image=new BufferedImage(resolution,resolution,BufferedImage.TYPE_INT_RGB);
        for(int j=1;j<n-1;j++)for(int i=1;i<n-1;i++){
            if(arm[j][i]==0){image.setRGB(i-1,j-1,new Color(33,40,49).getRGB());continue;}
            double dx=(h[j][i+1]-h[j][i-1])/(2*step),dz=(h[j+1][i]-h[j-1][i])/(2*step);
            double shade=Math.max(.43,Math.min(1.23,.64+.58*(1-.55*dx-.65*dz)/Math.sqrt(1+dx*dx+dz*dz)));
            Color low=arm[j][i]==1?new Color(183,155,108):new Color(105,134,91);
            Color c=lerp(low,new Color(179,173,153),(h[j][i]-94)/64);
            int r=(int)Math.min(255,c.getRed()*shade),b=(int)Math.min(255,c.getBlue()*shade),green=(int)Math.min(255,c.getGreen()*shade);
            image.setRGB(i-1,j-1,new Color(r,green,b).getRGB());
        }
        if(resolution==v.pixels)return image;
        var display=new BufferedImage(v.pixels,v.pixels,BufferedImage.TYPE_INT_RGB);var g=display.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image,0,0,v.pixels,v.pixels,null);g.dispose();return display;
    }
    private static void footprint(Graphics2D g,RiverBanks banks,View v,int x,int y,boolean warnings){
        Shape clip=g.getClip();g.clipRect(x,y,v.pixels,v.pixels);
        Path2D bankLayer=new Path2D.Double(Path2D.WIND_NON_ZERO),waterLayer=new Path2D.Double(Path2D.WIND_NON_ZERO);
        for(var p:banks.patches())if(p.supported()) {bankLayer.append(polygon(p.bank(),v,x,y),false);waterLayer.append(polygon(p.water(),v,x,y),false);}
        // One consistently oriented fill per layer avoids antialias seams between shared patch edges.
        g.setColor(new Color(172,162,124));g.fill(bankLayer);g.setColor(new Color(45,119,145));g.fill(waterLayer);
        if(warnings){g.setColor(new Color(229,159,76));g.setStroke(new BasicStroke(.7f));
            for(var p:banks.patches())if(p.supported()&&p.excessPonding()>4)g.draw(polygon(p.water(),v,x,y));}
        g.setClip(clip);
    }
    private static Path2D polygon(List<RiverBanks.Point> points,View v,int ox,int oy){
        Path2D p=new Path2D.Double();boolean first=true;double scale=v.pixels/(double)v.span;
        double area=0;for(int i=0;i<points.size();i++){var a=points.get(i);var b=points.get((i+1)%points.size());area+=a.x()*b.z()-a.z()*b.x();}
        for(int i=0;i<points.size();i++){var point=points.get(area>0?points.size()-1-i:i);double x=ox+(point.x()-v.x+v.span*.5)*scale,y=oy+(point.z()-v.z+v.span*.5)*scale;
            if(first){p.moveTo(x,y);first=false;}else p.lineTo(x,y);}p.closePath();return p;
    }
    private static void roundControl(Graphics2D g,DrainageGraph graph,View v,int ox,int oy){
        Shape clip=g.getClip();g.clipRect(ox,oy,v.pixels,v.pixels);double scale=v.pixels/(double)v.span;
        for(var n:graph.nodes())if(n.wet()&&n.downstream()>=0){var p=graph.nodes().get(n.downstream());
            double x=ox+(n.x()-v.x+v.span*.5)*scale,y=oy+(n.z()-v.z+v.span*.5)*scale;
            double px=ox+(p.x()-v.x+v.span*.5)*scale,py=oy+(p.z()-v.z+v.span*.5)*scale;
            g.setStroke(new BasicStroke((float)((n.halfWidth()*2+6)*scale),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(172,162,124));g.draw(new Line2D.Double(x,y,px,py));
        }
        for(var n:graph.nodes())if(n.wet()&&n.downstream()>=0){var p=graph.nodes().get(n.downstream());
            g.setStroke(new BasicStroke((float)(n.halfWidth()*2*scale),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(45,119,145));
            g.draw(new Line2D.Double(ox+(n.x()-v.x+v.span*.5)*scale,oy+(n.z()-v.z+v.span*.5)*scale,
                    ox+(p.x()-v.x+v.span*.5)*scale,oy+(p.z()-v.z+v.span*.5)*scale));}
        g.setClip(clip);
    }
    private static Color lerp(Color a,Color b,double t){t=Math.max(0,Math.min(1,t));return new Color((int)(a.getRed()+(b.getRed()-a.getRed())*t),(int)(a.getGreen()+(b.getGreen()-a.getGreen())*t),(int)(a.getBlue()+(b.getBlue()-a.getBlue())*t));}
    private static BufferedImage canvas(int width,int height){var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(BG);g.fillRect(0,0,width,height);g.dispose();return image;}
    private static Graphics2D graphics(BufferedImage image){var g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);return g;}
    private static void text(Graphics2D g,String s,int x,int y,int size,boolean bold){g.setColor(INK);g.setFont(new Font("Microsoft YaHei",bold?Font.BOLD:Font.PLAIN,size));g.drawString(s,x,y);}
    private static void save(BufferedImage image,Path path)throws Exception{ImageIO.write(image,"png",path.toFile());}
}
