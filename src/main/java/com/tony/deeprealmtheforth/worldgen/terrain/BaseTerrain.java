package com.tony.deeprealmtheforth.worldgen.terrain;

import com.tony.deeprealmtheforth.worldgen.layout.SpiralLayout;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.biome.BiomeClimate;
import static com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile.*;

/**
 * River-free H0 model. Both the legacy generator and experimental drainage read
 * these exact equations; this class never calls the final generator or river network.
 */
public final class BaseTerrain {
    private final long seed;
    private final SpiralLayout layout;
    private final VortexField field;
    private final BiomeClimate biomes;
    private final NaturalRelief natural;
    private final MarineRelief marine;
    private final AridRelief aridRelief;
    private final int version;
    private final ThreadLocal<Memo> memo=ThreadLocal.withInitial(Memo::new);
    private static final class Memo {
        final long[] keys=new long[8192];
        final Shape[] shapes=new Shape[8192];
        final Column[] columns=new Column[8192];
    }
    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private static int slot(int x,int z){return (int)SeededNoise.hash(9193,x,z)&8191;}
    public BaseTerrain(long seed, SpiralParameters parameters) {
        this(seed, parameters, 1);
    }
    private BaseTerrain(long seed, SpiralParameters parameters, int version) {
        this.version=version;
        this.seed = seed; layout = new SpiralLayout(parameters);
        field = new VortexField(seed, parameters,version>=4); biomes = new BiomeClimate(seed,version>=4);
        natural = version > 1 ? new NaturalRelief(seed, parameters, version >= 3,version>=4) : null;
        marine=version>=5?new MarineRelief(seed):null;
        aridRelief=version>=5?new AridRelief(seed):null;
    }
    /** Explicit opt-in, used only by offline tests. Default/serialized generators remain legacy. */
    public static BaseTerrain naturalPrototype(long seed, SpiralParameters parameters) {
        return new BaseTerrain(seed, parameters, 2);
    }
    public static BaseTerrain naturalWorld(long seed, SpiralParameters parameters) { return new BaseTerrain(seed, parameters, 3); }
    public static BaseTerrain naturalWorld(long seed, SpiralParameters parameters,int version) {
        if(version!=3&&version!=4&&version!=5)throw new IllegalArgumentException("Natural world version must be 3, 4 or 5");
        return new BaseTerrain(seed,parameters,version);
    }
    /** Macro H0 components only: small surface hummocks are not independent drainage basins. */
    public double drainageHeight(int x, int z) {
        var p=field.sample(x+.5,z+.5);
        if(natural==null||!p.land()||(p.arm()!=1&&p.arm()!=5))return shape(x,z).top();
        var candidate=natural.sample(x+.5,z+.5,p.arm()==1);
        return layout.baseHeight(p.radius())+(aridRelief!=null&&p.arm()==1
                ?aridRelief.sample(x+.5,z+.5,candidate).drainage():candidate.drainage());
    }
    public SpiralLayout layout() { return layout; }
    public VortexField field() { return field; }
    public BiomeClimate biomes() { return biomes; }
    public record Shape(VortexField.Point point, double u, double v, double fade,
                        double top, int water, Fluid fluid, Theme theme, long variant, double beach) {}
    public Column sample(int x, int z) {
        if(natural!=null) {
            Shape s=shape(x,z);Memo m=memo.get();int i=slot(x,z);
            if(m.columns[i]==null)m.columns[i]=finish(x,z,s,s.top(),s.water(),s.fluid(),s.theme(),s.variant(),Double.POSITIVE_INFINITY);
            return m.columns[i];
        }
        Shape s = shape(x, z);
        return finish(x, z, s, s.top(), s.water(), s.fluid(), s.theme(), s.variant(), Double.POSITIVE_INFINITY);
    }

    public Shape shape(int x, int z) {
        if(natural==null)return calculateShape(x,z);
        Memo m=memo.get();long key=key(x,z);int i=slot(x,z);
        if(m.shapes[i]!=null&&m.keys[i]==key)return m.shapes[i];
        Shape s=calculateShape(x,z);m.keys[i]=key;m.shapes[i]=s;m.columns[i]=null;return s;
    }
    private Shape calculateShape(int x, int z) {
        VortexField.Point p = field.sample(x + .5, z + .5);
        if (!p.land()) return new Shape(p, 0, 0, 0, -64, -64, Fluid.NONE, Theme.VOID, 0, 0);
        VortexField.Coordinates relief = field.relief(x + .5, z + .5);
        double u = relief.u(), v = relief.v();
        double rim = layout.parameters().plungeRadius();
        double fade = SpiralLayout.smooth(rim, rim + 110, p.radius())
                * SpiralLayout.smooth(1.5, 22, p.edgeDistance());
        if(version>=4 && (p.arm()==1||p.arm()==5) && p.radius()>=rim+144) {
            // Fully natural region: the old relief is blended out entirely. Do not
            // evaluate its many noise fields merely to subtract it again below.
            var candidate=natural.sample(x+.5,z+.5,p.arm()==1);
            double top=layout.baseHeight(p.radius())+3*SeededNoise.fractal(seed^1951,x,z,18,3)+fade*candidate.height();
            double moisture=SeededNoise.sample(seed^883,u,v,210)+.24*SeededNoise.sample(seed^510,u,v,55);
            Theme theme=p.arm()==1?(moisture>.05?Theme.BADLANDS:Theme.DESERT)
                    :candidate.mountain()>30?Theme.MOUNTAIN:moisture<-.23?Theme.PLAINS:moisture>.28?Theme.JUNGLE:Theme.FOREST;
            if(aridRelief!=null&&p.arm()==1) {
                var dry=aridRelief.sample(x+.5,z+.5,candidate);
                top+=fade*(dry.height()-candidate.height());
                if(dry.kind()!=null)theme=dry.kind();
            }
            return new Shape(p,u,v,fade,top,-64,Fluid.NONE,theme,
                    SeededNoise.hash(seed,(int)Math.floor(u/64),(int)Math.floor(v/64)),0);
        }
        double wx = x + 28 * SeededNoise.fractal(seed ^ 7119, x, z, 240, 2);
        double wz = z + 28 * SeededNoise.fractal(seed ^ 1931, x, z, 240, 2);
        // Local terrain is partly world-space, partly flow-aligned: not every hill
        // stretches into the same mathematical spiral, while large ridges still curl.
        double broad = .6 * SeededNoise.fractal(seed, wx, wz, 170, 4)
                + .4 * SeededNoise.fractal(seed ^ 591, u * .7, v, 200, 3);
        double hills = 16 * SeededNoise.fractal(seed ^ 7631, wx, wz, 88, 3)
                + 4 * SeededNoise.fractal(seed ^ 103, u * .7, v, 42, 2);
        double moisture = SeededNoise.sample(seed ^ 883, u, v, 210)
                + .24 * SeededNoise.sample(seed ^ 510, u, v, 55);
        double erosion = SeededNoise.sample(seed ^ 5141, u, v, 240);
        // A rounded, differentiable crest instead of an abs-noise cusp. Large
        // mountains survive; small octaves no longer serrate their summits.
        double ridgeNoise = SeededNoise.fractal(seed ^ 819, u * .55, v, 150, 2);
        double ridge = Math.exp(-6 * ridgeNoise * ridgeNoise);
        double mountain = SpiralLayout.smooth(.05, .6, erosion) * ridge * ridge;
        double flowRelief = 6 * SeededNoise.fractal(seed ^ 9331, u * .38, v, 90, 2);
        double gullies = Math.pow(1 - SpiralLayout.smooth(0, .55,
                Math.abs(SeededNoise.fractal(seed ^ 1681, wx, wz, 130, 2))), 2);
        // Arid terrain keeps rolling dunes and broader badlands uplands, but
        // does not inherit the full 70-block temperate mountain amplification.
        double badlands = SpiralLayout.smooth(-.2, .35, moisture);
        boolean arid = p.arm() == 1;
        double mountainHeight = arid ? 8 + 22 * badlands : 70;
        // Break the circular cliff outline inward without lowering its outer Y=60 rim.
        double cliffWarp = (3 + 5 * SeededNoise.fractal(seed ^ 1291, x, z, 29, 3))
                * SpiralLayout.smooth(layout.parameters().coreRadius() + 4,
                        Math.max(layout.parameters().coreRadius() + 5, rim - 18), p.radius())
                * (1 - SpiralLayout.smooth(rim, rim + 18, p.radius()));
        double rimDetail = 3 * SeededNoise.fractal(seed ^ 1951, x, z, 18, 3)
                * SpiralLayout.smooth(rim - 8, rim, p.radius());
        double top = layout.baseHeight(p.radius() + Math.max(0, cliffWarp)) + rimDetail
                + fade * (broad * (arid ? 19 : 27) + hills * (arid ? .45 : 1)
                + mountain * mountainHeight + flowRelief - gullies * mountain * (arid ? 3 : 8));
        int water = -64; Fluid fluid = Fluid.NONE; Theme theme;
        double beach = 0;
        long variant = SeededNoise.hash(seed, (int) Math.floor(u / 64), (int) Math.floor(v / 64));
        switch (p.arm()) {
            case 1 -> {
                double canyon = 1 - SpiralLayout.smooth(0, .22, Math.abs(SeededNoise.sample(seed ^ 411, u, v, 135)));
                top += fade * (badlands * (12 + broad * 12) - canyon * 17 * badlands);
                theme = moisture > .05 ? Theme.BADLANDS : Theme.DESERT;
            }
            case 3 -> {
                theme = moisture > .05 ? Theme.BASALT : Theme.NETHER;
                top += fade * (Math.abs(broad) * 12 + hills * .3);
                SeededNoise.Cell volcano = SeededNoise.cell(seed ^ 9201, u, v, 320);
                double volcanoRadius = 52 + 23 * SeededNoise.unit(volcano.id());
                double distance = Math.max(0, volcano.distance() + 5 * SeededNoise.fractal(seed ^ 921, u, v, 26, 3));
                double volcanoFade = SpiralLayout.smooth(rim + 90, rim + 180, p.radius());
                if (volcanoFade > 0 && distance < volcanoRadius) {
                    theme = Theme.VOLCANO;
                    double height = 48 + 36 * SeededNoise.unit(SeededNoise.hash(volcano.id(), 19, 3));
                    double craterRadius = 8 + 4 * SeededNoise.unit(SeededNoise.hash(volcano.id(), 37, 1));
                    double cone = Math.pow(Math.max(0, 1 - distance / volcanoRadius), 1.25) * height;
                    double crater = (1 - SpiralLayout.smooth(craterRadius, craterRadius + 15, distance)) * (height * .45);
                    double volcanoTop = layout.baseHeight(p.radius()) + (cone - crater) * fade;
                    top += (volcanoTop - top) * volcanoFade * (1 - SpiralLayout.smooth(volcanoRadius * .55, volcanoRadius, distance));
                    if (distance < craterRadius && fade > .99 && volcanoFade > .999) {
                        water = (int) Math.floor(layout.baseHeight(Math.hypot(volcano.x(), volcano.z())) + height * .5);
                        top = water - 5; fluid = Fluid.LAVA;
                    }
                }
            }
            case 5 -> theme = mountain > .48 ? Theme.MOUNTAIN : moisture < -.23 ? Theme.PLAINS
                    : moisture > .28 ? Theme.JUNGLE : Theme.FOREST;
            case 7 -> {
                if(marine!=null) {
                    var sea=marine.sample(x+.5,z+.5);
                    double warp=38*SeededNoise.fractal(seed^1491,x,z,85,3)
                            *SpiralLayout.smooth(rim+24,rim+80,p.radius());
                    double blend=SpiralLayout.smooth(rim+24,rim+145,p.radius()+warp);
                    top+=(sea.height()-top)*blend;beach=sea.beach();
                    theme=top>=SEA_LEVEL?(beach>.5?Theme.COAST:Theme.ISLAND)
                            :top<SEA_LEVEL-24?Theme.DEEP_OCEAN:Theme.OCEAN;
                    if(blend<.05)theme=Theme.COAST;
                    else if(top<SEA_LEVEL){water=SEA_LEVEL;fluid=Fluid.WATER;}
                    break;
                }
                // Mostly world-space continents: coves and promontories no longer
                // inherit the same stretched spiral contour at every scale.
                double ix = wx + 42 * SeededNoise.fractal(seed ^ 65391, x, z, 170, 2);
                double iz = wz + 42 * SeededNoise.fractal(seed ^ 18751, x, z, 170, 2);
                double island = .76 * SeededNoise.fractal(seed ^ 32181, ix, iz, 210, 3)
                        + .24 * SeededNoise.fractal(seed ^ 4198, ix, iz, 85, 2);
                double inland = (island - .06) * 170;
                double coastWarp = 50 * SeededNoise.fractal(seed ^ 1491, x, z, 85, 3)
                        * SpiralLayout.smooth(rim + 24, rim + 80, p.radius());
                double oceanBlend = SpiralLayout.smooth(rim + 24, rim + 145, p.radius() + coastWarp);
                double width = SurfaceTransitions.beachWidth(seed, x, z);
                double seabed = inland < 0 ? SEA_LEVEL + .55 * inland
                        : SEA_LEVEL + .12 * inland + .46 * Math.max(0, inland - width)
                        * SpiralLayout.smooth(width, width + 40, inland);
                top += (seabed - top) * oceanBlend;
                beach = SurfaceTransitions.beach(seed, x, z, Math.max(inland, (top - SEA_LEVEL) / .20));
                if (oceanBlend < .05) {
                    theme = Theme.COAST; // Dry cliff rim; no fixed-radius seabed jump.
                } else {
                    if (top >= SEA_LEVEL) {
                        theme = beach > .5 ? Theme.COAST : Theme.ISLAND;
                    } else {
                        theme = top < SEA_LEVEL - 24 ? Theme.DEEP_OCEAN : Theme.OCEAN;
                        water = SEA_LEVEL; fluid = Fluid.WATER;
                    }
                }
            }
            default -> throw new IllegalStateException("Even arms must be void");
        }
        if (natural != null && (p.arm() == 1 || p.arm() == 5)) {
            var candidate = natural.sample(x + .5, z + .5, arid);
            double height = layout.baseHeight(p.radius() + Math.max(0, cliffWarp)) + rimDetail + fade * candidate.height();
            double transition = SpiralLayout.smooth(rim + 24, rim + 144, p.radius());
            if(aridRelief!=null&&arid) {
                var dry=aridRelief.sample(x+.5,z+.5,candidate);
                height+=fade*(dry.height()-candidate.height());
                if(transition>.9&&dry.kind()!=null)theme=dry.kind();
            }
            top += (height - top) * transition;
            if (p.arm() == 5 && transition > .999)
                theme = candidate.mountain() > 30 ? Theme.MOUNTAIN : moisture < -.23 ? Theme.PLAINS
                        : moisture > .28 ? Theme.JUNGLE : Theme.FOREST;
        }
        return new Shape(p, u, v, fade, top, water, fluid, theme, variant, beach);
    }

    Column finish(int x, int z, Shape s, double top, int water, Fluid fluid,
                  Theme theme, long variant, double riverDistance) {
        VortexField.Point p = s.point();
        if (!p.land()) return new Column(p.arm(), -64, -64, -64, Fluid.NONE, Theme.VOID, 0,
                p.edgeDistance(), false, Double.POSITIVE_INFINITY, 0);
        double fade = s.fade(), beach = s.beach();
        if (fluid == Fluid.NONE && p.arm() != 7) top -= (1 - SpiralLayout.smooth(1.5, 16, p.edgeDistance())) * 8 * fade;
        int surface = Math.max(-59, Math.min(424, (int) Math.floor(top)));
        int thickness = (int) (9 + 53 * SpiralLayout.smooth(1.5, 28, p.edgeDistance())
                * SpiralLayout.smooth(layout.parameters().coreRadius(), 100, p.radius()));
        theme = biomes.theme(x, z, theme, p.arm(), top - layout.baseHeight(p.radius()));
        return new Column(p.arm(), surface, Math.max(-62, surface - thickness), water, fluid, theme, variant,
                p.edgeDistance(), false, riverDistance, beach);
    }
}
