package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import com.tony.deeprealmtheforth.worldgen.terrain.VortexField;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import javax.imageio.ImageIO;

/** Same sampling protocol also runs against the archived 0.1.2 JAR in baseline mode. */
public final class TerrainReliefVerification {
    private static final long[] SEEDS = {0, 42, -739221};
    private static final TerrainProfile.Theme[] THEMES = {
            TerrainProfile.Theme.DESERT, TerrainProfile.Theme.BADLANDS,
            TerrainProfile.Theme.MOUNTAIN, TerrainProfile.Theme.PLAINS};

    public static void main(String[] args) throws Exception {
        run(Path.of(args[0]), args.length > 1 && args[1].equals("baseline"));
    }

    public static void run(Path out, boolean baseline) throws Exception {
        Files.createDirectories(out);
        var stats = new EnumMap<TerrainProfile.Theme, Stats>(TerrainProfile.Theme.class);
        for (var theme : THEMES) stats.put(theme, new Stats());
        for (long seed : SEEDS) {
            var terrain = new TerrainProfile(seed, SpiralParameters.DEFAULT);
            for (int x = -1536; x <= 1536; x += 7) for (int z = -1536; z <= 1536; z += 7) {
                var c = terrain.sample(x, z);
                if (!stats.containsKey(c.theme()) || c.wet() || c.shore() || c.edgeDistance() < 40
                        || Math.hypot(x - 8, z - 8) < 256) continue;
                var w = terrain.sample(x - 1, z); var e = terrain.sample(x + 1, z);
                var n = terrain.sample(x, z - 1); var s = terrain.sample(x, z + 1);
                if (w.theme() != c.theme() || e.theme() != c.theme()
                        || n.theme() != c.theme() || s.theme() != c.theme()) continue;
                stats.get(c.theme()).add(seed, x, z, c.top(), w.top(), e.top(), n.top(), s.top());
            }
        }
        StringBuilder report = new StringBuilder(baseline ? "RELIEF_BASELINE\n" : "RELIEF_VERIFICATION_OK\n");
        report.append("seeds=0,42,-739221; grid=[-1536,1536], step=7; dry same-theme cardinal neighbors; radius>=256; edge>=40\n");
        for (var entry : stats.entrySet()) {
            var s = entry.getValue();
            report.append(entry.getKey()).append(' ').append(s).append('\n');
            if (!baseline) {
                boolean mountain = entry.getKey() == TerrainProfile.Theme.MOUNTAIN;
                require(s.count > 5000, "Nontrivial sample coverage: " + entry.getKey());
                require(s.percentile(.99) <= (mountain ? 6 : 4), "99th percentile slope: " + entry);
                require(s.max <= (mountain ? 16 : 12), "One-block height jump: " + entry);
                require(s.crest <= 4, "Isolated one-block crest: " + entry);
            }
        }
        if (!baseline) {
            var mountains = stats.get(TerrainProfile.Theme.MOUNTAIN);
            var desert = stats.get(TerrainProfile.Theme.DESERT);
            require(mountains.maxTop >= 135 && mountains.maxTop - mountains.minTop >= 45,
                    "Retain tall mountains and valleys, not a flattened plateau");
            require(desert.maxTop - desert.minTop >= 20, "Retain desert relief");
            report.append("maxReliefCoordinateStep=").append(verifyCoordinates()).append('\n');
        }
        render(out.resolve("relief-samples.png"), baseline);
        Files.writeString(out.resolve("relief-verification.txt"), report);
        System.out.print(report);
    }

    private static double verifyCoordinates() {
        double max = 0;
        for (long seed : SEEDS) {
            var field = new VortexField(seed, SpiralParameters.DEFAULT);
            for (int radius : new int[]{128, 512, 2048, 8192, 32768, 65536}) {
                for (int degrees = 0; degrees < 360; degrees++) {
                    double a = Math.toRadians(degrees);
                    double x = 8 + radius * Math.cos(a), z = 8 + radius * Math.sin(a);
                    var p = field.relief(x, z);
                    for (int[] offset : new int[][]{{1, 0}, {0, 1}}) {
                        var n = field.relief(x + offset[0], z + offset[1]);
                        double step = Math.hypot(p.u() - n.u(), p.v() - n.v());
                        require(Double.isFinite(step) && step < 2.8, "Radius-independent relief shear: r=" + radius + " step=" + step);
                        max = Math.max(max, step);
                    }
                }
            }
        }
        // atan2's branch cut must not create a seam in unwound coordinates.
        var f = new VortexField(42, SpiralParameters.DEFAULT);
        var north = f.relief(-4096, 8 - .000001);
        var south = f.relief(-4096, 8 + .000001);
        require(Math.hypot(north.u() - south.u(), north.v() - south.v()) < .00001, "Continuous angular seam");
        return max;
    }

    private static final class Stats {
        final int[] histogram = new int[512];
        int count, max, minTop = Integer.MAX_VALUE, maxTop = Integer.MIN_VALUE;
        double crest = Double.NEGATIVE_INFINITY;
        String worst = "";
        void add(long seed, int x, int z, int c, int w, int e, int n, int s) {
            int delta = Math.max(Math.abs(c - e), Math.abs(c - s));
            histogram[delta]++; count++;
            minTop = Math.min(minTop, c); maxTop = Math.max(maxTop, c);
            crest = Math.max(crest, c - (w + e + n + s) / 4.0);
            if (delta > max) { max = delta; worst = "seed=" + seed + ",x=" + x + ",z=" + z; }
        }
        int percentile(double fraction) {
            int cumulative = 0;
            for (int i = 0; i < histogram.length; i++) {
                cumulative += histogram[i];
                if (cumulative >= Math.ceil(count * fraction)) return i;
            }
            throw new AssertionError("Empty histogram");
        }
        @Override public String toString() {
            return "samples=" + count + " p95Delta1=" + percentile(.95) + " p99Delta1=" + percentile(.99)
                    + " maxDelta1=" + max + " maxCrest=" + crest + " topRange=" + minTop + ".." + maxTop + " worst=" + worst;
        }
    }

    /** Fixed coordinates, projection and vertical scale: actual model heights, not AI concepts. */
    private static void render(Path path, boolean baseline) throws Exception {
        int width = 1560, height = 650;
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(new Color(20, 28, 40)); g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        g.drawString((baseline ? "BEFORE / 0.1.2" : "CURRENT / relief preserved from 0.1.3") + "   |   identical coordinates and height scale", 24, 35);
        int[][] probes = {{-486, -1256}, {1201, 1404}, {-983, -1116}};
        String[] labels = {"Arid sample", "Mountain sample", "Badlands sample"};
        long[] seeds = {0, 42, -739221};
        for (int panel = 0; panel < probes.length; panel++) {
            int ox = panel * 520, cx = probes[panel][0], cz = probes[panel][1];
            var terrain = new TerrainProfile(seeds[panel], SpiralParameters.DEFAULT);
            int[][] y = new int[65][65];
            for (int i = 0; i <= 64; i++) for (int j = 0; j <= 64; j++)
                y[i][j] = terrain.sample(cx - 64 + i * 2, cz - 64 + j * 2).surface();
            g.setColor(Color.LIGHT_GRAY); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
            g.drawString(labels[panel] + " / seed " + seeds[panel], ox + 24, 72);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.drawString("128 x 128 blocks at (" + cx + ", " + cz + ")", ox + 24, 95);
            for (int diagonal = 0; diagonal < 127; diagonal++) for (int i = 0; i < 64; i++) {
                int j = diagonal - i; if (j < 0 || j >= 64) continue;
                var c = terrain.sample(cx - 64 + i * 2, cz - 64 + j * 2);
                if (!c.land()) continue;
                int[] px = new int[4], py = new int[4];
                int[][] corners = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int k = 0; k < 4; k++) {
                    int a = corners[k][0], b = corners[k][1];
                    px[k] = ox + 260 + (int) Math.round((a - b) * 3.6);
                    py[k] = 280 + (int) Math.round((a + b) * 1.25 - (y[a][b] - 60) * 1.65);
                }
                Color base = c.wet() ? new Color(53, 135, 179) : panel == 1 ? new Color(161, 183, 149) : new Color(205, 163, 100);
                double shade = Math.max(.35, Math.min(1.25, .9 + (y[i][j] - y[i + 1][j]) * .055
                        - (y[i][j] - y[i][j + 1]) * .03));
                g.setColor(new Color((int) Math.min(255, base.getRed() * shade),
                        (int) Math.min(255, base.getGreen() * shade), (int) Math.min(255, base.getBlue() * shade)));
                g.fillPolygon(px, py, 4);
            }
            // East/west transect through the same center, not an illustrative curve.
            for (int level = 40; level <= 160; level += 40) {
                int sy = 616 - (int) ((level - 40) * .8);
                g.setColor(new Color(54, 66, 80)); g.drawLine(ox + 48, sy, ox + 492, sy);
                g.setColor(Color.LIGHT_GRAY); g.drawString("" + level, ox + 16, sy + 4);
            }
            g.setColor(new Color(112, 211, 196)); g.setStroke(new BasicStroke(1.8f));
            for (int i = -64; i < 64; i++) {
                int ya = terrain.sample(cx + i, cz).surface(), yb = terrain.sample(cx + i + 1, cz).surface();
                g.drawLine(ox + 48 + (i + 64) * 444 / 128, 616 - (int) ((ya - 40) * .8),
                        ox + 48 + (i + 65) * 444 / 128, 616 - (int) ((yb - 40) * .8));
            }
            g.setColor(Color.LIGHT_GRAY); g.drawString("E/W height section (Y40-160)", ox + 48, 640);
        }
        g.dispose(); ImageIO.write(image, "png", path.toFile());
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
