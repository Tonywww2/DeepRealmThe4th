package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.hydrology.*;
import com.tony.deeprealmtheforth.worldgen.hydrology.DrainageGraph.*;
import com.tony.deeprealmtheforth.worldgen.terrain.BaseTerrain;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Draws solver data, not AI art and not a promise of final Minecraft block geometry. */
final class HydrologyPrototypeRenderer {
    private static final Color BG = new Color(19,27,37), TEXT = new Color(224,233,239);
    private static final String[] TITLES = {"湿润温带 / 丛林", "远端山地", "干旱陆地股"};
    static void render(Path out, ClimateSnapshot climate, List<DrainageGraph> graphs, List<BaseTerrain> bases,
                       DrainageGraph adjacent) throws Exception {
        networks(out.resolve("01-完整水系.png"), graphs);
        climate(out.resolve("02-群系气候.png"), graphs);
        terrain(out.resolve("03-地形与纵剖面.png"), graphs);
        boundaries(out.resolve("04-分区边界诊断.png"), graphs.get(0), adjacent);
    }
    private static BufferedImage canvas(int width, int height) {
        var image = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics(); g.setColor(BG); g.fillRect(0,0,width,height); g.dispose(); return image;
    }
    private static Graphics2D graphics(BufferedImage image) {
        var g=image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(TEXT); return g;
    }
    private static void text(Graphics2D g,String s,int x,int y,int size,boolean bold) {
        g.setFont(new Font("Microsoft YaHei",bold?Font.BOLD:Font.PLAIN,size));g.setColor(TEXT);g.drawString(s,x,y);
    }
    private static void save(BufferedImage image,Path path) throws Exception { ImageIO.write(image,"png",path.toFile()); }
    private static void networks(Path file,List<DrainageGraph> graphs) throws Exception {
        var image=canvas(1900,910);var g=graphics(image);
        text(g,"主干 + 支流 · 第一轮真实算法样品",24,40,27,true);
        text(g,"seed 42  |  每幅 2048 × 2048 格  |  32 格抖动三角网  |  宽度随有效汇流量变化",24,71,17,false);
        for(int scene=0;scene<3;scene++) {
            var graph=graphs.get(scene);int ox=24+scene*625;
            text(g,TITLES[scene],ox,112,23,true);
            text(g,"起点 ("+graph.domain().x()+", "+graph.domain().z()+")",ox,138,15,false);
            map(g,graph,ox,154,600,0,true);
            long heads=graph.nodes().stream().filter(n->n.wet()&&n.incoming()==0).count();
            long joins=graph.nodes().stream().filter(n->n.wet()&&n.incoming()>1).count();
            int order=graph.nodes().stream().mapToInt(Node::order).max().orElse(0);
            text(g,"源头 "+heads+"   汇口 "+joins+"   最高河级 "+order,ox,783,17,false);
            text(g,scene==2?"无本地降水 → 无凭空产生的常年河流":"蓝色：有水河段；亮度和宽度表示河级 / 流量",ox,811,15,false);
        }
        text(g,"白点：源头   青点：汇口   橙圈：盆地终点候选   红叉：出口未确定   淡蓝面：分析积水候选",24,851,17,false);
        text(g,"原型诊断图，并非游戏截图。尚未平滑河岸 / 模拟方块水；有限父域拼接未通过，禁止接入正式生成。",24,884,17,true);
        g.dispose();save(image,file);
    }
    private static void climate(Path file,List<DrainageGraph> graphs) throws Exception {
        var image=canvas(1550,1210);var g=graphics(image);
        text(g,"群系气候输入 · 读取真实注册表，不按群系名称编造参数",24,38,24,true);
        text(g,"有限邻域混合仅用于供水；不会改变游戏中的群系温度。温度不是 ℃，供水和径流均为无量纲量。",24,68,16,false);
        String[] titles={"群系基础温度 T","降水供给 P","有效径流 R"};
        for(int col=0;col<3;col++) text(g,titles[col],24+col*510,104,21,true);
        for(int row=0;row<3;row++) {
            int y=125+row*345; var graph=graphs.get(row);
            for(int col=0;col<3;col++) {
                map(g,graph,24+col*510,y,305,col+1,false);
                text(g,TITLES[row],340+col*510,y+28,16,true);
                double min=Double.POSITIVE_INFINITY,max=-Double.POSITIVE_INFINITY;
                for(var n:graph.nodes()) if(n.active()) {double v=scalar(n,col+1);min=Math.min(min,v);max=Math.max(max,v);}
                text(g,String.format(Locale.ROOT,"%.2f ~ %.2f",min,max),340+col*510,y+58,15,false);
                text(g,col==0?"蓝：冷 / 橙：热":col==1?"褐：干 / 蓝：湿":"褐：无 / 绿：多",340+col*510,y+91,13,false);
            }
        }
        text(g,"当前默认群系未增补：暖高山仍保持其真实温度；冻河 / 新冷山群系候选留待确认后设计。",24,1190,17,false);
        g.dispose();save(image,file);
    }
    private static double scalar(Node n,int mode) {
        return switch(mode){case 1->n.climate().temperature();case 2->n.climate().supply();default->n.climate().runoff();};
    }
    private static Color lerp(Color a,Color b,double t) {
        t=Math.max(0,Math.min(1,t));return new Color((int)(a.getRed()+(b.getRed()-a.getRed())*t),
                (int)(a.getGreen()+(b.getGreen()-a.getGreen())*t),(int)(a.getBlue()+(b.getBlue()-a.getBlue())*t));
    }
    private static Color landColor(Node n,double shade) { return landColor(n, n.height(), shade); }
    private static Color landColor(Node n,double height,double shade) {
        if(!n.active())return new Color(35,43,51);
        Color low=n.climate().runoff()<.02?new Color(181,151,97):new Color(99,124,82);
        Color high=new Color(181,177,155);
        Color c=lerp(low,high,(height-94)/68);
        return new Color((int)Math.min(255,c.getRed()*shade),(int)Math.min(255,c.getGreen()*shade),(int)Math.min(255,c.getBlue()*shade));
    }
    private static void map(Graphics2D g,DrainageGraph graph,int ox,int oy,int pixels,int mode,boolean rivers) {
        int side=graph.domain().side(); double scale=pixels/(double)graph.domain().span();
        var img=new BufferedImage(pixels,pixels,BufferedImage.TYPE_INT_RGB);
        for(int py=0;py<pixels;py++)for(int px=0;px<pixels;px++) {
            double gx=px*(side-1.0)/pixels,gz=py*(side-1.0)/pixels;
            int ix=(int)gx,iz=(int)gz,k=iz*side+ix;double u=gx-ix,v=gz-iz;
            Node a=graph.nodes().get(k),b=graph.nodes().get(k+1),c=graph.nodes().get(k+side),d=graph.nodes().get(k+side+1);
            Color color;
            if(!a.active())color=new Color(35,43,51);
            else if(mode!=0) {
                double av=scalar(a,mode),bv=b.active()?scalar(b,mode):av,cv=c.active()?scalar(c,mode):av,dv=d.active()?scalar(d,mode):av;
                double value=(av*(1-u)+bv*u)*(1-v)+(cv*(1-u)+dv*u)*v;
                color=mode==1?lerp(new Color(75,155,192),new Color(222,128,61),value/2):
                        lerp(new Color(160,119,70),mode==2?new Color(65,146,196):new Color(65,176,132),value/(mode==2?1:.65));
            } else {
                double h=(a.height()*(1-u)+b.height()*u)*(1-v)+(c.height()*(1-u)+d.height()*u)*v;
                double dx=(a.height()-b.height())*(1-v)+(c.height()-d.height())*v;
                double dz=(a.height()-c.height())*(1-u)+(b.height()-d.height())*u;
                double shade=Math.max(.55,Math.min(1.25,1+dx*.026+dz*.015));
                color=landColor(a,h,shade);
                if(h%10<.38)color=lerp(color,new Color(50,63,54),.23);
                if(a.wet()&&a.water()>h+1)color=lerp(color,new Color(113,195,205),.45);
            }
            img.setRGB(px,py,color.getRGB());
        }
        g.drawImage(img,ox,oy,null);
        if(rivers) {
            Shape clip=g.getClip();g.clipRect(ox,oy,pixels,pixels);
            List<Node> lines=graph.nodes().stream().filter(n->n.wet()&&n.downstream()>=0).sorted(Comparator.comparingInt(Node::order)).toList();
            for(Node n:lines) {
                Node p=graph.nodes().get(n.downstream());
                float width=(float)Math.max(1,2*n.halfWidth()*scale);
                g.setStroke(new BasicStroke(width+1.3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(24,67,83));
                Line2D line=new Line2D.Double(ox+(n.x()-graph.domain().x())*scale,oy+(n.z()-graph.domain().z())*scale,
                        ox+(p.x()-graph.domain().x())*scale,oy+(p.z()-graph.domain().z())*scale);
                g.draw(line);g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.setColor(lerp(new Color(61,149,182),new Color(137,221,237),(n.order()-1)/4.0));g.draw(line);
            }
            for(Node n:graph.nodes())if(n.active()) {
                int x=ox+(int)((n.x()-graph.domain().x())*scale),y=oy+(int)((n.z()-graph.domain().z())*scale);
                if(n.wet()&&n.incoming()==0){g.setColor(new Color(240,248,237));g.fillOval(x-2,y-2,4,4);}
                if(n.wet()&&n.incoming()>1){g.setColor(new Color(129,253,225));g.fillOval(x-2,y-2,4,4);}
                if(n.terminal()!=Terminal.NONE){g.setStroke(new BasicStroke(2));g.setColor(new Color(255,182,90));g.drawOval(x-5,y-5,10,10);
                    if(n.terminal()==Terminal.UNRESOLVED_BOUNDARY){g.setColor(new Color(255,98,98));g.drawLine(x-5,y-5,x+5,y+5);g.drawLine(x-5,y+5,x+5,y-5);}}
            }
            g.setClip(clip);
        }
        g.setColor(new Color(96,111,124));g.setStroke(new BasicStroke(1));g.drawRect(ox,oy,pixels,pixels);
    }
    private static void terrain(Path file,List<DrainageGraph> graphs) throws Exception {
        var image=canvas(1860,1040);var g=graphics(image);
        text(g,"地形与水系纵剖面 · 诊断分析积水，而不是把地面强行填平",24,39,26,true);
        text(g,"斜视图：原有无河流地形 H0 + 排水网络；竖向放大 2.5 倍。蓝线为分析水位，不是已经生成的方块水。",24,73,16,false);
        for(int scene=0;scene<3;scene++) {
            int ox=20+scene*615;var graph=graphs.get(scene);int side=graph.domain().side();
            text(g,TITLES[scene],ox+12,112,22,true);
            double scale=560.0/(2*graph.domain().span());
            for(int sum=0;sum<2*(side-1);sum++)for(int iz=0;iz<side-1;iz++) {
                int ix=sum-iz;if(ix<0||ix>=side-1)continue;int k=iz*side+ix;
                int[] indexes={k,k+1,k+side+1,k+side};var polygon=new Polygon();
                for(int index:indexes){Node n=graph.nodes().get(index);var p=project(n.x(),n.z(),n.height(),graph,ox,scale);polygon.addPoint(p[0],p[1]);}
                Node a=graph.nodes().get(k),b=graph.nodes().get(k+1),c=graph.nodes().get(k+side);
                double shade=Math.max(.55,Math.min(1.2,1+(a.height()-b.height())*.026+(a.height()-c.height())*.015));
                g.setColor(landColor(a,shade));g.fill(polygon);
            }
            for(Node n:graph.nodes())if(n.wet()&&n.downstream()>=0){Node p=graph.nodes().get(n.downstream());
                int[] a=project(n.x(),n.z(),n.water()+2,graph,ox,scale),b=project(p.x(),p.z(),p.water()+2,graph,ox,scale);
                g.setColor(new Color(104,211,235));g.setStroke(new BasicStroke((float)Math.max(1,n.halfWidth()*scale)));
                g.drawLine(a[0],a[1],b[0],b[1]);}
            profile(g,graph,ox+15,570,565,250);
            text(g,"未实现：河岸断面、细化曲流、湖泊收支与水流 tick",ox+10,885,15,false);
            text(g,"分析洼地过深处必须重选路 / 分流域，不能照搬成湖。",ox+10,916,15,false);
        }
        text(g,"当前样品用于判断“先汇流、再塑形”的方向；不代表河岸自然度或无限世界连续性已经达标。",24,997,19,true);
        g.dispose();save(image,file);
    }
    private static int[] project(double x,double z,double h,DrainageGraph graph,int ox,double scale) {
        double u=x-graph.domain().x(),v=z-graph.domain().z();
        return new int[]{ox+300+(int)((u-v)*scale),180+(int)((u+v)*scale*.52-(h-60)*scale*2.5)};
    }
    private static void profile(Graphics2D g,DrainageGraph graph,int ox,int oy,int width,int height) {
        var nodes=graph.nodes();int end=-1;double best=0;
        for(int i=0;i<nodes.size();i++)if(nodes.get(i).wet()&&nodes.get(i).downstream()<0&&nodes.get(i).discharge()>best){best=nodes.get(i).discharge();end=i;}
        if(end<0){text(g,"没有常年河流：不绘制虚假的水位线",ox,oy+100,20,true);return;}
        List<Integer> path=new ArrayList<>();int k=end;
        while(k>=0){path.add(k);int child=-1;double q=0;for(int i=0;i<nodes.size();i++)if(nodes.get(i).downstream()==k&&nodes.get(i).wet()&&nodes.get(i).discharge()>q){q=nodes.get(i).discharge();child=i;}k=child;}
        Collections.reverse(path);double[] distance=new double[path.size()];double min=1e9,max=-1e9;
        for(int i=0;i<path.size();i++){Node n=nodes.get(path.get(i));min=Math.min(min,Math.min(n.height(),n.water()));max=Math.max(max,Math.max(n.height(),n.water()));if(i>0){Node p=nodes.get(path.get(i-1));distance[i]=distance[i-1]+Math.hypot(n.x()-p.x(),n.z()-p.z());}}
        min-=3;max+=3;double length=Math.max(1,distance[distance.length-1]);
        text(g,"最大主干：从源头 → 终点候选",ox,oy-18,18,true);
        for(int tick=0;tick<5;tick++){int y=oy+tick*height/4;g.setColor(new Color(52,65,76));g.drawLine(ox,y,ox+width,y);text(g,String.format(Locale.ROOT,"Y%.0f",max-(max-min)*tick/4),ox+3,y+17,13,false);}
        for(int mode=0;mode<2;mode++){Path2D line=new Path2D.Double();for(int i=0;i<path.size();i++){Node n=nodes.get(path.get(i));double x=ox+width*distance[i]/length,y=oy+height*(max-(mode==0?n.height():n.water()))/(max-min);if(i==0)line.moveTo(x,y);else line.lineTo(x,y);}
            g.setColor(mode==0?new Color(203,177,108):new Color(96,220,248));g.setStroke(new BasicStroke(2.5f));g.draw(line);}
        text(g,String.format(Locale.ROOT,"0                 沿河距离 %.0f 格     金：H0 / 蓝：水位",length),ox,oy+height+27,14,false);
    }
    private static void boundaries(Path file,DrainageGraph parent,DrainageGraph adjacent) throws Exception {
        var image=canvas(1400,835);var g=graphics(image);
        text(g,"分区验证：已通过的范围，与尚未解决的问题",24,42,26,true);
        text(g,"同一父图 → 两个子区：共用河段 / 水位 / 流量",24,84,20,true);
        text(g,"两个独立父域：没有共同上游，边界仍不连续",720,84,20,true);
        map(g,parent,24,110,620,0,true);
        g.setColor(new Color(255,222,119));g.setStroke(new BasicStroke(2));g.drawLine(334,110,334,730);
        var d=parent.domain();for(var c:parent.verticalContracts(d.x()+d.span()/2.0)){
            int y=110+(int)((c.z()-d.z())/d.span()*620);g.setColor(new Color(255,241,152));g.fillOval(330,y-4,8,8);}
        map(g,parent,720,110,300,0,true);map(g,adjacent,1020,110,300,0,true);
        g.setColor(new Color(255,101,101));g.setStroke(new BasicStroke(3));g.drawLine(1020,110,1020,410);
        text(g,"右侧保留实际失败案例，不将计算方格边缘伪装成河口。",720,469,17,false);
        text(g,"PASS：独立重算父图、逆序、并发、丢弃后重建一致。",720,513,17,false);
        text(g,"FAIL：相邻父域上游面积 / 水量 / 水位还没有共享契约。",720,554,17,true);
        text(g,"后续必须增加全局宏观排水骨架；在此之前不能发布。",720,597,17,false);
        text(g,"该诊断明确区分“子区裁切一致”与“无限世界父域连续”，前者不能证明后者。",24,794,19,true);
        g.dispose();save(image,file);
    }
}
