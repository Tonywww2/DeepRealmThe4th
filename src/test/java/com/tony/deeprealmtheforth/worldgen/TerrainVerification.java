package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.layout.SpiralLayout;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import com.tony.deeprealmtheforth.worldgen.terrain.SeededNoise;
import com.tony.deeprealmtheforth.travel.LandingSearch;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import javax.imageio.ImageIO;

/** Dependency-free executable verification, run by both nodes' check tasks. */
public final class TerrainVerification {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        verifyLandingSearch();
        SpiralLayout layout = new SpiralLayout(SpiralParameters.DEFAULT);
        check(SpiralParameters.DEFAULT.landFraction() == 0.75, "75 percent nominal land coverage");
        check(SpiralParameters.DEFAULT.twist() == 2 * SpiralParameters.LEGACY.twist(), "Twist doubled");
        verifyBandWidths(layout);
        verifyLegacyLayout();
        verifyCliffAndNoise(layout);
        verifyContinuousArms(layout);
        for (int radius : new int[]{32, 64, 128, 256, 1024, 4096}) {
            for (int arm = 1; arm <= 8; arm++) {
                double[] point = layout.armCenter(arm, radius);
                check(layout.sample(point[0], point[1]).arm() == arm, "Spiral arm numbering");
                check(layout.sample(point[0], point[1]).land() == ((arm & 1) == 1), "Alternating land/void");
            }
        }
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) check(!layout.sample(x + .5, z + .5).land(), "Central chunk is void");
        for (int r = 17; r < 12000; r++) check(layout.baseHeight(r) >= layout.baseHeight(r - 1), "Radial monotonicity");
        check(layout.baseHeight(56) - layout.baseHeight(30) > 110, "Visible plunge inside the seven-chunk center");
        check(layout.baseHeight(1100) - layout.baseHeight(1000) < 10, "Gentle outer slope");
        for (int r : new int[]{64, 86, 128, 256, 512, 1024, 4096}) {
            double ratio = (layout.baseHeight(r + 1) - layout.baseHeight(r)) / (layout.radialHeight(r + 1) - layout.radialHeight(r));
            check(Math.abs(ratio - .1) < 1e-7, "Outer radial slope is exactly ten percent: " + ratio);
        }
        double[] outer = layout.armCenter(1, 512), inner = layout.armCenter(1, 128);
        double cross = (outer[0] - 8) * (inner[1] - 8) - (outer[1] - 8) * (inner[0] - 8);
        check(cross > 0, "Clockwise inward rotation with +Z down");

        EnumSet<TerrainProfile.Theme> themes = EnumSet.noneOf(TerrainProfile.Theme.class);
        int wetColumns = 0, differences = 0, landColumns = 0, totalColumns = 0, shores = 0, warpedEdges = 0;
        for (long seed : new long[]{0, 42, -739221}) {
            TerrainProfile terrain = new TerrainProfile(seed, SpiralParameters.DEFAULT);
            TerrainProfile other = new TerrainProfile(seed + 1, SpiralParameters.DEFAULT);
            for (int x = -2048; x <= 2048; x += 13) {
                for (int z = -2048; z <= 2048; z += 13) {
                    TerrainProfile.Column c = terrain.sample(x, z);
                    // A circular annulus avoids square-corner bias in rotating unequal-width bands.
                    double sampleRadius = Math.hypot(x - 8, z - 8);
                    if (sampleRadius >= 128 && sampleRadius <= 2000) {
                        totalColumns++;
                        if (c.land()) landColumns++;
                    }
                    if (!c.land() && terrain.layout().sample(x + .5, z + .5).land()) warpedEdges++;
                    check(c.land() == terrain.field().sample(x + .5, z + .5).land(), "One final mask for all consumers");
                    check(c.equals(terrain.sample(x, z)), "Repeatable sampling");
                    themes.add(c.theme());
                    if (!c.equals(other.sample(x, z))) differences++;
                    if (!c.land()) { check((c.arm() & 1) == 0 || Math.hypot(x + .5 - 8, z + .5 - 8) <= 24 || c.edgeDistance() <= 1.5, "Void layout"); continue; }
                    check(c.bottom() >= -64 && c.top() >= c.bottom() && c.surface() < 448, "Vertical bounds");
                    if (c.shore()) {
                        shores++;
                        check(c.top() >= TerrainProfile.SEA_LEVEL && !c.wet(), "Dry sandstone shell at or above water");
                    }
                    if (c.wet()) {
                        wetColumns++;
                        check(c.top() - c.bottom() >= 3, "Solid fluid floor");
                        if (c.arm() == 7) {
                            check(c.fluidLevel() == TerrainProfile.SEA_LEVEL, "Continuous ocean sea level");
                            // Conservative Chebyshev dilation guarantees three cells even at diagonal corners.
                            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                                TerrainProfile.Column n = terrain.sample(x + dx, z + dz);
                                check(n.land() && (n.wet() || n.shore()), "Ocean has a three-wide continuous shell");
                            }
                        }
                        for (int[] offset : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                            TerrainProfile.Column n = terrain.sample(x + offset[0], z + offset[1]);
                            check(n.land() && (n.top() >= c.fluidLevel() || n.wet()
                                    && n.fluid() == c.fluid() && n.fluidLevel() == c.fluidLevel()), "Fluid contained by bank or same basin");
                        }
                    }
                }
            }
        }
        check(wetColumns > 1000, "Nontrivial rivers/oceans");
        check(differences > 10000, "World seed changes detail");
        check(themes.containsAll(EnumSet.range(TerrainProfile.Theme.VOID,TerrainProfile.Theme.ISLAND)), "Every legacy terrain theme is reachable: " + themes);
        double coverage = landColumns / (double) totalColumns;
        check(coverage > .72 && coverage < .78, "Continuous bands retain approximately 75 percent land: " + coverage);
        check(shores > 100 && warpedEdges > 1000, "Both protected shore and seeded edge variation exist");

        // Sample the same columns concurrently and in reverse order, including chunk seams.
        TerrainProfile parallel = new TerrainProfile(42, SpiralParameters.DEFAULT);
        var columns = java.util.stream.IntStream.range(0, 8192)
                .mapToObj(i -> parallel.sample(i - 4096, (i * 71) % 8192 - 4096)).toList();
        java.util.stream.IntStream.range(0, 8192).parallel().forEach(j -> {
            int i = 8191 - j;
            if (!columns.get(i).equals(parallel.sample(i - 4096, (i * 71) % 8192 - 4096))) throw new AssertionError("Concurrent sampling differs");
        });

        Path out = Path.of(args[0]); Files.createDirectories(out);
        BiomeDistributionVerification.run(out, false);
        TerrainReliefVerification.run(out, false);
        ShoreTransitionVerification.run(out, false);
        overview(out.resolve("overview.png"), parallel);
        profile(out.resolve("radial-profile.png"), layout);
        TerrainVisualPreview.render(out.resolve("relief.png"), parallel, "Continuous arms / 7-chunk center / 3-chunk core");
        fluidProbes(out.resolve("fluid-probes.json"), parallel);
        String report = "TERRAIN_VERIFICATION_OK\nassertions=" + assertions + "\nseeds=0,42,-739221\nwetColumns=" + wetColumns
                + "\nseedDifferences=" + differences + "\nallThemes=" + themes
                + "\nlandFraction=" + layout.parameters().landFraction() + "\ntwist=" + layout.parameters().twist()
                + "\nmeasuredLandCoverage=" + coverage + "\nshoreColumns=" + shores + "\nwarpedEdgeColumns=" + warpedEdges
                + "\ncenterDiameter=112\nvoidDiameter=48\ninteriorFractures=false"
                + "\ncliffRimRadius=56\ncliffRimBaselineY=" + layout.baseHeight(56)
                + "\nouterRadialSlope=10%\noceanWallMinimumWidth=3\noceanSeaLevel=" + TerrainProfile.SEA_LEVEL
                + "\nnominalArmFractions=3/16,1/16,3/16,1/16,3/16,1/16,3/16,1/16\n";
        Files.writeString(out.resolve("verification.txt"), report);
        System.out.println(report);
    }

    private static void check(boolean value, String message) { assertions++; if (!value) throw new AssertionError(message); }

    private static void verifyCliffAndNoise(SpiralLayout layout) {
        check(layout.baseHeight(56) == 60, "Cliff upper rim is anchored to Y60 at radius56");
        check(layout.baseHeight(56) - layout.baseHeight(30) >= 110, "Tall central cliff");
        for (int join : new int[]{30, 56}) {
            double left = (layout.baseHeight(join) - layout.baseHeight(join - .001)) / .001;
            double right = (layout.baseHeight(join + .001) - layout.baseHeight(join)) / .001;
            check(Math.abs(left - right) < .002, "Continuous baseline slope at " + join);
        }
        int rimColumns = 0;
        for (long seed : new long[]{0, 42, -739221}) {
            TerrainProfile terrain = new TerrainProfile(seed, SpiralParameters.DEFAULT);
            for (int degree = 0; degree < 360; degree++) {
                double angle = Math.toRadians(degree);
                int x = (int) Math.floor(8 + Math.cos(angle) * 56);
                int z = (int) Math.floor(8 + Math.sin(angle) * 56);
                var column = terrain.sample(x, z);
                if (column.land()) {
                    rimColumns++;
                    check(column.top() >= 55 && column.top() <= 65, "Actual cliff rim near Y60: " + column);
                    check(!column.wet(), "Keep the central cliff rim dry");
                }
            }
            for (int i = -256; i <= 256; i++) {
                double a = SeededNoise.fractal(seed, i * 17, i * 31, 62, 4);
                double b = SeededNoise.fractal(seed, i * 17 + .00001, i * 31, 62, 4);
                check(Double.isFinite(a) && Math.abs(a) <= 1.01, "Bounded gradient octaves");
                check(Math.abs(a - b) < .0001, "Continuous gradient octaves across lattice boundaries");
            }
        }
        check(rimColumns > 600, "Cliff rim spans all four land bands");
    }

    private static void verifyContinuousArms(SpiralLayout layout) {
        check(layout.parameters().plungeRadius() * 2 == 7 * 16, "Seven-chunk central diameter");
        check(layout.parameters().coreRadius() * 2 == 3 * 16, "Three-chunk void diameter");
        for (long seed : new long[]{0, 42, -739221}) {
            var terrain = new TerrainProfile(seed, layout.parameters());
            for (int x = -16; x < 32; x++) for (int z = -16; z < 32; z++) {
                if (Math.hypot(x + .5 - 8, z + .5 - 8) <= 24)
                    check(!terrain.sample(x, z).land(), "Expanded circular core remains empty");
            }
            // Interior tracks on both sides of each centerline, not just the final mask's own flags.
            // Former tangent tears intersected these tracks; warped edges stay well away from them.
            for (int arm = 1; arm <= 8; arm += 2) for (int r = 128; r <= 8192; r += 3) {
                double[] center = layout.armCenter(arm, r);
                double angle = Math.atan2(center[1] - 8, center[0] - 8);
                for (double offset : new double[]{-.12, 0, .12}) {
                    var p = terrain.field().sample(8 + r * Math.cos(angle + offset), 8 + r * Math.sin(angle + offset));
                    check(p.arm() == arm && p.land(), "No void tears in arm interior: seed=" + seed + " arm=" + arm + " r=" + r);
                }
            }
        }
    }

    private static void verifyBandWidths(SpiralLayout layout) {
        int samples = 16000;
        for (int radius : new int[]{64, 256, 1024, 4096}) {
            int[] counts = new int[8];
            int solid = 0;
            for (int i = 0; i < samples; i++) {
                double angle = (i + .5) * Math.PI * 2 / samples;
                SpiralLayout.Point p = layout.sample(8 + radius * Math.cos(angle), 8 + radius * Math.sin(angle));
                counts[p.arm() - 1]++;
                if (p.land()) solid++;
                check(p.edgeDistance() >= 0, "Nonnegative distance in unequal bands");
            }
            for (int arm = 1; arm <= 8; arm++) {
                int expected = (arm & 1) == 1 ? samples * 3 / 16 : samples / 16;
                check(Math.abs(counts[arm - 1] - expected) <= 1, "3:1 band area at radius " + radius + " arm " + arm);
            }
            if (radius >= 1024) check(solid / (double) samples > .74 && solid / (double) samples <= .75,
                    "Actual outer land coverage stays near 75 percent with edge clearance");
        }
        double[] outer = layout.armCenter(1, 512), inner = layout.armCenter(1, 256);
        double delta = Math.atan2(inner[1] - 8, inner[0] - 8) - Math.atan2(outer[1] - 8, outer[0] - 8);
        delta = (delta + Math.PI * 2) % (Math.PI * 2);
        check(Math.abs(delta - 1.44 * Math.log(2)) < 1e-12, "Actual radial winding doubled");
    }

    private static void verifyLegacyLayout() {
        SpiralLayout legacy = new SpiralLayout(SpiralParameters.LEGACY);
        for (int x = -1024; x <= 1024; x += 17) for (int z = -1024; z <= 1024; z += 19) {
            double radius = Math.hypot(x - 8, z - 8);
            double phase = Math.atan2(z - 8, x - 8) + .72 * Math.log(Math.max(radius, 16) / 256);
            phase -= Math.floor(phase / (Math.PI * 2)) * Math.PI * 2;
            int arm = Math.min(7, (int) (phase / (Math.PI / 4))) + 1;
            double local = phase - (arm - 1) * Math.PI / 4;
            double edge = radius * Math.min(local, Math.PI / 4 - local) / Math.sqrt(1 + .72 * .72);
            SpiralLayout.Point actual = legacy.sample(x, z);
            check(actual.arm() == arm && Math.abs(actual.edgeDistance() - edge) < 1e-9,
                    "Legacy 50:50 layout remains compatible");
        }
    }

    private static void verifyLandingSearch() {
        Object landing = new Object();
        check(LandingSearch.find(0, 0, 32, (x, z) -> x == 1 && z == 0 ? landing : null) == landing,
                "Landing one block away must not be skipped by parity");
        // Every possible sole landing, including negative coordinates and chunk seams.
        for (int origin : new int[]{-17, -16, 0, 1}) {
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                int targetX = origin + dx, targetZ = origin + dz;
                check(LandingSearch.find(origin, origin, 3,
                        (x, z) -> x == targetX && z == targetZ ? landing : null) == landing,
                        "Find isolated landing at " + targetX + "," + targetZ);
            }
        }
        int[] visits = {0}, previousRing = {0};
        java.util.Set<Long> seen = new java.util.HashSet<>();
        check(LandingSearch.find(-16, 16, 32, (x, z) -> {
            int ring = Math.max(Math.abs(x + 16), Math.abs(z - 16));
            check(ring >= previousRing[0] && ring <= 32, "Bounded outward search order");
            previousRing[0] = ring;
            check(seen.add(((long) x << 32) ^ (z & 0xffffffffL)), "Probe each column only once");
            visits[0]++;
            return null;
        }) == null, "All unsafe columns must refuse landing");
        check(visits[0] == 65 * 65, "Exhaustive search of the full 32-block square");
        int[] centerVisits = {0};
        check(LandingSearch.find(4, -5, 32, (x, z) -> {
            centerVisits[0]++;
            check(x == 4 && z == -5, "Preferred column is checked first");
            return landing;
        }) == landing && centerVisits[0] == 1, "Stop immediately on a safe landing");
        check(LandingSearch.find(0, 0, 32, (x, z) -> x == 1 && z == 0 ? "near"
                : x == -2 && z == 0 ? "far" : null).equals("near"), "Prefer the nearer ring");
        check(LandingSearch.find(0, 0, 0, (x, z) -> x == 1 ? landing : null) == null,
                "Radius zero must not search neighboring columns");
        boolean negativeRejected = false;
        try { LandingSearch.find(0, 0, -1, (x, z) -> landing); }
        catch (IllegalArgumentException expected) { negativeRejected = true; }
        check(negativeRejected, "Reject a negative radius");
    }

    private static void fluidProbes(Path path, TerrainProfile terrain) throws Exception {
        EnumSet<TerrainProfile.Theme> found = EnumSet.noneOf(TerrainProfile.Theme.class);
        java.util.List<String> probes = new java.util.ArrayList<>();
        for (int x = -1024; x <= 1024; x += 3) for (int z = -1024; z <= 1024; z += 3) {
            TerrainProfile.Column c = terrain.sample(x, z);
            if (c.wet() && found.add(c.theme())) probes.add("{\"x\":" + x + ",\"z\":" + z
                    + ",\"y\":" + c.fluidLevel() + ",\"theme\":\"" + c.theme() + "\",\"fluid\":\"" + c.fluid() + "\"}");
        }
        Files.writeString(path, "[\n" + String.join(",\n", probes) + "\n]\n");
    }

    private static Color color(TerrainProfile.Column c) {
        Color base = switch (c.theme()) {
            case VOID -> new Color(12, 19, 30);
            case DESERT -> new Color(217, 184, 112);
            case BADLANDS, PLATEAU -> new Color(188, 108, 58);
            case ARID_MOUNTAIN -> new Color(159, 143, 110);
            case RIVER -> new Color(64, 145, 184);
            case NETHER -> new Color(139, 56, 58);
            case BASALT, VOLCANO -> c.wet() ? new Color(255, 131, 36) : new Color(109, 94, 102);
            case PLAINS -> new Color(143, 186, 94);
            case FOREST -> new Color(70, 135, 86);
            case JUNGLE -> new Color(32, 104, 69);
            case MOUNTAIN -> new Color(172, 182, 177);
            case COAST -> new Color(225, 210, 153);
            case OCEAN -> new Color(44, 127, 184);
            case DEEP_OCEAN -> new Color(29, 74, 135);
            case ISLAND -> new Color(109, 177, 113);
        };
        if (!c.land()) return base;
        double shade = .72 + .28 * Math.max(0, Math.min(1, (c.surface() + 60) / 360.0));
        return new Color((int) (base.getRed() * shade), (int) (base.getGreen() * shade), (int) (base.getBlue() * shade));
    }

    private static void overview(Path path, TerrainProfile terrain) throws Exception {
        BufferedImage image = new BufferedImage(1160, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(20, 28, 40)); g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24)); g.setColor(Color.WHITE);
        g.drawString("THE FOURTH LAYER  /  continuous arms + 7/3-chunk center", 32, 38);
        drawMap(image, terrain, 32, 85, 520, 2048);
        drawMap(image, terrain, 608, 85, 520, 256);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.drawString("Overview: 4096 x 4096 blocks", 32, 70);
        g.drawString("Center: 512 x 512 blocks", 608, 70);
        g.drawString("Seed 42  |  North = -Z (up)  |  Center = (8, 8)", 32, 635);
        g.drawString("1 Arid   3 Infernal   5 Temperate / tropical   7 Oceanic", 32, 662);
        g.drawString("Nominal land 75%  |  Twist 1.44", 790, 635);
        g.dispose(); ImageIO.write(image, "png", path.toFile());
    }

    private static void drawMap(BufferedImage image, TerrainProfile terrain, int ox, int oy, int size, int radius) {
        for (int px = 0; px < size; px++) for (int pz = 0; pz < size; pz++) {
            int x = (int) Math.floor(8 + (px / (double) size - .5) * radius * 2);
            int z = (int) Math.floor(8 + (pz / (double) size - .5) * radius * 2);
            image.setRGB(ox + px, oy + pz, color(terrain.sample(x, z)).getRGB());
        }
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(255, 255, 255, 140)); g.drawLine(ox + size / 2 - 5, oy + size / 2, ox + size / 2 + 5, oy + size / 2);
        g.drawLine(ox + size / 2, oy + size / 2 - 5, ox + size / 2, oy + size / 2 + 5);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        for (int arm = 1; arm <= 8; arm += 2) {
            double[] point = terrain.layout().armCenter(arm, radius * .68);
            int x = ox + (int) ((point[0] - 8) / (radius * 2) * size) + size / 2;
            int z = oy + (int) ((point[1] - 8) / (radius * 2) * size) + size / 2;
            g.setColor(new Color(0, 0, 0, 190)); g.fillOval(x - 13, z - 16, 28, 28);
            g.setColor(Color.WHITE); g.drawString(Integer.toString(arm), x - 5, z + 5);
        }
        g.dispose();
    }

    private static void profile(Path path, SpiralLayout layout) throws Exception {
        BufferedImage image = new BufferedImage(1100, 510, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(20, 28, 40)); g.fillRect(0, 0, 1100, 510);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24)); g.setColor(Color.WHITE);
        g.drawString("Radial baseline  /  local mountains are added separately", 32, 40);
        for (int y = -64; y <= 256; y += 64) {
            int py = 432 - (int) ((y + 64) * 1.05);
            g.setColor(new Color(65, 75, 88)); g.drawLine(76, py, 1050, py);
            g.setColor(Color.LIGHT_GRAY); g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14)); g.drawString("Y " + y, 18, py + 5);
        }
        g.setColor(new Color(229, 115, 98)); int marker = 76 + 56 * 950 / 1024; g.drawLine(marker, 72, marker, 432);
        g.drawString("56-block cliff rim", marker + 8, 83);
        g.setStroke(new BasicStroke(2)); g.setColor(new Color(120, 126, 143));
        for (int r = 17; r <= 1024; r++) {
            double old1 = previousBaseline(layout, r - 1);
            double old2 = previousBaseline(layout, r);
            g.drawLine(76 + (r - 1) * 950 / 1024, 432 - (int) ((old1 + 64) * 1.05),
                    76 + r * 950 / 1024, 432 - (int) ((old2 + 64) * 1.05));
        }
        g.drawString("Gray: B baseline   Green: Y60 rim + 10% outer slope", 545, 95);
        g.setStroke(new BasicStroke(3)); g.setColor(new Color(113, 205, 186));
        for (int r = 17; r <= 1024; r++) {
            int x1 = 76 + (r - 1) * 950 / 1024, x2 = 76 + r * 950 / 1024;
            int y1 = 432 - (int) ((layout.baseHeight(r - 1) + 64) * 1.05), y2 = 432 - (int) ((layout.baseHeight(r) + 64) * 1.05);
            g.drawLine(x1, y1, x2, y2);
        }
        g.setColor(Color.LIGHT_GRAY);
        for (int r : new int[]{0, 128, 256, 512, 768, 1024}) g.drawString(Integer.toString(r), 76 + r * 950 / 1024 - 8, 460);
        g.drawString("Distance from center (blocks); radius <= 24 is the open core", 76, 490);
        g.dispose(); ImageIO.write(image, "png", path.toFile());
    }

    private static double previousBaseline(SpiralLayout layout, double radius) {
        double a = 42, b = 86, ya = layout.radialHeight(a), yb = ya + .55 * (layout.radialHeight(b) - ya);
        double radial;
        if (radius <= a) radial = layout.radialHeight(radius);
        else if (radius >= b) radial = yb + .1 * (layout.radialHeight(radius) - layout.radialHeight(b));
        else {
            double t = (radius - a) / (b - a), t2 = t * t, t3 = t2 * t;
            double ma = 210.0 / 430 * Math.exp(-a / 430), mb = 21.0 / 430 * Math.exp(-b / 430);
            radial = (2 * t3 - 3 * t2 + 1) * ya + (t3 - 2 * t2 + t) * (b - a) * ma
                    + (-2 * t3 + 3 * t2) * yb + (t3 - t2) * (b - a) * mb;
        }
        return radial - 78 * (1 - SpiralLayout.smooth(a, b, radius));
    }
}
