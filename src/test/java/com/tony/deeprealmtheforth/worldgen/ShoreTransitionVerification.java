package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Same-coordinate comparison against the archived 0.1.4 JAR; no Minecraft or GPU required. */
public final class ShoreTransitionVerification {
    public static void main(String[] args) throws Exception {
        run(Path.of(args[0]), args.length > 1 && args[1].equals("baseline"));
    }
    public static void run(Path out, boolean baseline) throws Exception {
        Files.createDirectories(out);
        long mask = 0xcbf29ce484222325L, center = mask;
        int beach = 0, dryOcean = 0, shores = 0, snowyBelow120 = 0, bareAbove120 = 0;
        Set<Integer> shoreHeights = new TreeSet<>();
        List<Integer> riverBanks = new ArrayList<>(), islandBanks = new ArrayList<>(), valleySlopes = new ArrayList<>();
        double minSnow = 999, maxSnow = -999;
        for (long seed : new long[]{0, 42, -739221}) {
            var t = new TerrainProfile(seed, SpiralParameters.DEFAULT);
            for (int x = -2048; x <= 2048; x += 8) for (int z = -2048; z <= 2048; z += 8) {
                var c = t.sample(x, z);
                for (long v : new long[]{c.arm(), c.land() ? 1 : 0, Double.doubleToLongBits(c.edgeDistance())})
                    mask = (mask ^ v) * 0x100000001b3L;
                if (!c.land() || Math.hypot(x - 8, z - 8) < 256) continue;
                if (c.arm() == 7 && !c.wet()) {
                    dryOcean++;
                    if (!c.shore() && sandy(c, seed, x, z, baseline)) beach++;
                    if (c.shore()) { shores++; shoreHeights.add(c.top()); }
                }
                if (c.theme() == TerrainProfile.Theme.MOUNTAIN) {
                    double line = baseline ? 121 : SurfaceTransitions.snowLine(seed, x, z);
                    minSnow = Math.min(minSnow, line); maxSnow = Math.max(maxSnow, line);
                    if (c.top() < 120 && c.top() >= line) snowyBelow120++;
                    if (c.top() > 120 && c.top() < line) bareAbove120++;
                }
                if (!c.wet() && (c.arm() == 1 || c.arm() == 5) && c.edgeDistance() > 40) {
                    var p = t.field().relief(x + .5,z + .5);
                    double distance = RiverNetwork.sample(seed,p.u(),p.v(),t.layout()).distance();
                    if (distance >= 0 && distance <= 40) {
                        var e = t.sample(x+1,z); var s = t.sample(x,z+1);
                        if (e.land() && s.land() && !e.wet() && !s.wet())
                            valleySlopes.add(Math.max(Math.abs(c.top()-e.top()),Math.abs(c.top()-s.top())));
                    }
                }
                if (!c.wet()) continue;
                for (int[] d : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
                    var n = t.sample(x + d[0], z + d[1]);
                    if (!n.land() || n.wet() || n.edgeDistance() < 20) continue;
                    if (c.theme() == TerrainProfile.Theme.RIVER) riverBanks.add(n.top() - c.fluidLevel());
                    if (c.arm() == 7) islandBanks.add(n.top() - c.fluidLevel());
                }
            }
            for (int x = -64; x <= 80; x++) for (int z = -64; z <= 80; z++) {
                if (Math.hypot(x + .5 - 8, z + .5 - 8) > 72) continue;
                var c = t.sample(x, z);
                for (long v : new long[]{c.arm(), c.top(), c.bottom(), c.fluidLevel(), c.land() ? 1 : 0})
                    center = (center ^ v) * 0x100000001b3L;
            }
        }
        Collections.sort(riverBanks); Collections.sort(islandBanks); Collections.sort(valleySlopes);
        String report = (baseline ? "SHORE_BASELINE" : "SHORE_TRANSITIONS_OK")
                + "\nmaskFingerprint=" + Long.toUnsignedString(mask, 16)
                + "\ncentralCliffFingerprint=" + Long.toUnsignedString(center, 16)
                + "\nseeds=0,42,-739221; grid +/-2048 step8; statistics outside radius256"
                + "\nbeachWithoutProtectiveShell=" + beach + "\ndryOceanColumns=" + dryOcean
                + "\nbeachFraction=" + beach / (double) dryOcean + "\nshoreColumns=" + shores
                + "\nshoreHeights=" + shoreHeights + "\nsnowLineRange=" + minSnow + ".." + maxSnow
                + "\nsnowBelow120=" + snowyBelow120 + "\nbareMountainAbove120=" + bareAbove120
                + "\nriverBankCount=" + riverBanks.size() + "\nriverBankP95=" + p95(riverBanks)
                + "\nriverBankMax=" + maximum(riverBanks)
                + "\nislandBankCount=" + islandBanks.size() + "\nislandBankP95=" + p95(islandBanks)
                + "\nislandBankMax=" + maximum(islandBanks)
                + "\nvalleySlopeCount=" + valleySlopes.size() + "\nvalleySlopeP95=" + p95(valleySlopes)
                + "\nvalleySlopeMax=" + maximum(valleySlopes) + "\n";
        Files.writeString(out.resolve("shore-verification.txt"), report);
        System.out.print(report);
        if (!baseline) {
            require(mask == 0xe044a615cb00990cL, "Land/void outline unchanged from archived 0.1.4");
            require(center == 0xaced97098c2b22a8L, "Central cliff geometry unchanged from archived 0.1.4");
            require(beach > dryOcean * .18, "Large beaches exist away from the protective shell");
            require(maxSnow - minSnow > 30 && snowyBelow120 > 100 && bareAbove120 > 100, "Irregular snowline");
            require(riverBanks.size() > 500 && p95(riverBanks) <= 2, "Low river/land transition");
            require(islandBanks.size() > 500 && p95(islandBanks) <= 1, "Gradual island/sea transition");
            require(shoreHeights.size() >= 4, "No single-height island rim");
            require(valleySlopes.size() > 5000 && p95(valleySlopes) <= 2 && maximum(valleySlopes) <= 9,
                    "Smooth valley shoulders, not just the first shoreline block");
            verifyCatchments();
            verifyLargeBeach(out);
        }
        render(out.resolve("shore-samples.png"), baseline);
    }
    private static void verifyLargeBeach(Path out) throws Exception {
        var t = new TerrainProfile(42, SpiralParameters.DEFAULT);
        // Find a complete 33x33 sandy square, excluding every protected wall
        // column. Sparse prefilter only accelerates the exhaustive final check.
        for (int x = -2048; x <= 2048; x += 24) for (int z = -2048; z <= 2048; z += 24) {
            if (!beachSquare(t,x,z,16,8) || !beachSquare(t,x,z,16,1)) continue;
            String result = "BEACH_PATCH_OK center=" + x + "," + z + "; seed=42; fullSquare=33x33; excludesShell=true\n";
            Files.writeString(out.resolve("beach-patch.txt"),result); System.out.print(result); return;
        }
        throw new AssertionError("No large continuous sandy beach away from the containment shell");
    }
    private static boolean beachSquare(TerrainProfile t, int x, int z, int r, int step) {
        for (int dx=-r; dx<=r; dx+=step) for (int dz=-r; dz<=r; dz+=step) {
            var c=t.sample(x+dx,z+dz);
            if (!c.land() || c.arm()!=7 || c.wet() || c.shore() || !sandy(c,t.seed(),x+dx,z+dz,false)) return false;
        }
        return true;
    }
    private static int p95(List<Integer> a) { return a.isEmpty() ? 999 : a.get((int) ((a.size() - 1) * .95)); }
    private static int maximum(List<Integer> a) { return a.isEmpty() ? 999 : a.get(a.size() - 1); }
    private static boolean sandy(TerrainProfile.Column c, long seed, int x, int z, boolean baseline) {
        return baseline ? c.theme() == TerrainProfile.Theme.COAST
                : SurfaceTransitions.sand(seed, x, z, c.beach());
    }
    private static void verifyCatchments() {
        var layout = new TerrainProfile(42, SpiralParameters.DEFAULT).layout();
        for (long seed : new long[]{0, 42, -739221}) for (int k = -4; k <= 4; k++)
            for (int v = -1800; v <= 1800; v += 17) for (boolean swap : new boolean[]{false, true}) {
                double a = k * 420 - .00001, b = k * 420 + .00001;
                var l = RiverNetwork.sample(seed, swap ? v : a, swap ? a : v, layout);
                var r = RiverNetwork.sample(seed, swap ? v : b, swap ? b : v, layout);
                require(Math.abs(Math.min(424, l.valleyFloor()) - Math.min(424, r.valleyFloor())) < .001,
                        "Continuous valley across catchment grid");
            }
    }
    private static void render(Path path, boolean baseline) throws Exception {
        var t = new TerrainProfile(42, SpiralParameters.DEFAULT);
        var image = new BufferedImage(1680, 710, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics(); g.setColor(new Color(20,28,40)); g.fillRect(0,0,1680,710);
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        g.drawString((baseline ? "BEFORE 0.1.4" : "AFTER 0.1.5") + " | actual surface model, seed 42", 22, 34);
        // Fixed sites, shared between archived and current algorithms.
        int[][] sites = {{1201,1404}, {-983,-1116}, {-920,200}};
        String[] names = {"Snow / mountain", "River valley (arid)", "Island / wide beach"};
        for (int panel = 0; panel < 3; panel++) {
            int ox = 20 + panel * 560, cx = sites[panel][0], cz = sites[panel][1], span = 384;
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17)); g.setColor(Color.LIGHT_GRAY);
            g.drawString(names[panel] + " (" + cx + ", " + cz + ") / 384 blocks", ox, 65);
            for (int px = 0; px < 540; px++) for (int pz = 0; pz < 540; pz++) {
                int x = cx + px * span / 540 - span / 2, z = cz + pz * span / 540 - span / 2;
                var c = t.sample(x,z);
                Color color = color(c, t.seed(), x, z, baseline);
                if (c.land() && !c.wet()) {
                    var e = t.sample(x+1,z); var s = t.sample(x,z+1);
                    double shade = e.land() && s.land() ? Math.max(.48, Math.min(1.2, .88 + .075 * (s.top()-e.top()))) : .8;
                    color = new Color(Math.min(255,(int)(color.getRed()*shade)),
                            Math.min(255,(int)(color.getGreen()*shade)),Math.min(255,(int)(color.getBlue()*shade)));
                }
                image.setRGB(ox+px,85+pz,color.getRGB());
            }
        }
        g.setColor(Color.LIGHT_GRAY); g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.drawString("White = snow; gray = mountain/gravel; green = grass; pale yellow = sand; blue = water; dark = void.",22,658);
        g.drawString("Algorithm preview, not an in-game screenshot. Native trees, structures and biome tint are not rendered.",22,687);
        g.dispose(); ImageIO.write(image,"png",path.toFile());
    }
    private static Color color(TerrainProfile.Column c, long seed, int x, int z, boolean baseline) {
        if (!c.land()) return new Color(9,15,25);
        if (c.wet()) return c.fluid() == TerrainProfile.Fluid.LAVA ? new Color(240,105,25)
                : new Color(35, 115 + Math.max(0, 20 - (c.fluidLevel()-c.top())) * 3, 187);
        if (c.shore() || c.arm()==7 && sandy(c,seed,x,z,baseline)) return new Color(235,216,159);
        if (!baseline && (c.theme() == TerrainProfile.Theme.RIVER || c.arm() != 7
                && c.riverDistance() < 3 + 3 * SeededNoise.fractal(seed ^ 95731, x, z, 23, 2))) {
            return switch (SurfaceTransitions.sediment(seed,x,z,c.arm()==1)) {
                case 1 -> new Color(235,216,159); case 2 -> new Color(158,164,175); default -> new Color(145,145,133);
            };
        }
        if (c.theme()==TerrainProfile.Theme.MOUNTAIN)
            return (baseline ? c.top()>120 : SurfaceTransitions.snow(seed,x,z,c.top()))
                    ? new Color(239,244,247) : new Color(142,150,148);
        return switch(c.theme()) {
            case DESERT -> new Color(217,184,112);
            case BADLANDS -> new Color(188,108,58);
            case NETHER, BASALT, VOLCANO -> new Color(131,70,72);
            case RIVER -> new Color(145,145,133);
            default -> new Color(96,155,78);
        };
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
