package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.biome.BiomeClimate;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.SeededNoise;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Legacy distribution tests use shipped legacy-theme tags and the production selection function. */
public final class BiomeDistributionVerification {
    private static final long[] SEEDS = {0, 42, -739221};
    private static final Map<String, List<String>> POOLS = readPools();
    private static final List<String> ALL = POOLS.values().stream().flatMap(Collection::stream).distinct().sorted().toList();
    private static final List<String> NETHER = POOLS.get("infernal/nether");

    public static void main(String[] args) throws Exception { run(Path.of(args[0]), args.length > 1 && args[1].equals("baseline")); }

    public static void run(Path out, boolean baseline) throws Exception {
        Files.createDirectories(out);
        long geometry = 0xcbf29ce484222325L;
        Map<String, Integer> counts = new TreeMap<>();
        int columns = 0;
        for (long seed : SEEDS) {
            var t = new TerrainProfile(seed, SpiralParameters.DEFAULT);
            for (int x = -2048; x <= 2048; x += 13) for (int z = -2048; z <= 2048; z += 13) {
                var c = t.sample(x, z);
                for (long value : new long[]{c.arm(), c.top(), c.bottom(), c.fluidLevel(), c.fluid().ordinal(),
                        c.land() ? 1 : 0, c.shore() ? 1 : 0, Double.doubleToLongBits(c.edgeDistance()), c.cell()})
                    geometry = (geometry ^ value) * 0x100000001b3L;
                columns++;
                counts.merge(biome(t, c, x, z, baseline), 1, Integer::sum);
            }
        }
        if (baseline) require(geometry == 0x41b0d2de1d26ec92L, "Archived 0.1.3 geometry");
        else require(geometry == 0x9e13290f425a4d0eL, "Stage 0 must preserve exact 0.1.5 geometry");
        require(counts.keySet().containsAll(ALL), "Every shipped legacy-theme biome is reachable: " + counts);
        double near = coherence(new int[]{512, 1024, 2048}, baseline);
        double far = coherence(new int[]{8192, 16384, 32768}, baseline);
        if (!baseline) {
            require(near > .85 && far > .85, "Coherent patches at near/far radii: " + near + "/" + far);
            require(Math.abs(near - far) < .06, "No distance-amplified fragmentation");
            verifySelection();
        }
        String report = (baseline ? "BIOME_DISTRIBUTION_BASELINE" : "BIOME_DISTRIBUTION_OK")
                + "\nseeds=0,42,-739221\ncolumns=" + columns + "\ngeometryFingerprint=" + Long.toUnsignedString(geometry, 16)
                + "\nbiomes=" + counts.size() + "\ncounts=" + counts
                + "\nnetherPoolSameBiomeAt8BlocksNear=" + near + "\nnetherPoolSameBiomeAt8BlocksFar=" + far
                + "\ncoherenceProtocol=unclipped nether pool; 360 directions; three seeds; near radii 512/1024/2048; far 8192/16384/32768\n";
        Files.writeString(out.resolve("biome-verification.txt"), report);
        render(out.resolve("biome-map.png"), baseline);
        System.out.println(report);
    }

    private static double coherence(int[] radii, boolean baseline) {
        int same = 0, count = 0;
        for (long seed : SEEDS) {
            var t = new TerrainProfile(seed, SpiralParameters.DEFAULT);
            for (int r : radii) for (int degree = 0; degree < 360; degree++) {
                double angle = Math.toRadians(degree);
                int x = (int) Math.floor(8 + r * Math.cos(angle)), z = (int) Math.floor(8 + r * Math.sin(angle));
                String c = choose(t, "infernal/nether", NETHER, x, z, baseline);
                for (int[] d : new int[][]{{8, 0}, {0, 8}}) {
                    if (c.equals(choose(t, "infernal/nether", NETHER, x + d[0], z + d[1], baseline))) same++;
                    count++;
                }
            }
        }
        return same / (double) count;
    }

    private static void verifySelection() {
        var climate = new BiomeClimate(42);
        var other = new BiomeClimate(43);
        List<String> reversed = new ArrayList<>(NETHER); Collections.reverse(reversed);
        int extra = 0, changed = 0, cases = 0;
        for (int x = -2048; x <= 2048; x += 31) for (int z = -2048; z <= 2048; z += 37) {
            var p = climate.point(x + .5, z + .5);
            String expected = climate.choose(p, NETHER, Function.identity(), 190);
            require(expected.equals(climate.choose(p, reversed, Function.identity(), 190)), "Tag order independence");
            if (!expected.equals(other.choose(other.point(x + .5, z + .5), NETHER, Function.identity(), 190))) changed++;
            if (climate.useExtras(p, "infernal")) extra++;
            cases++;
        }
        require(changed > cases / 3, "Seed variation");
        require(extra > cases / 100 && extra < cases / 3, "Root-tag additions have bounded reachable pockets");
        var expected = java.util.stream.IntStream.range(0, 4096).mapToObj(i ->
                climate.choose(climate.point(i - 2048, i * 17 % 4096 - 2048), NETHER, Function.identity(), 190)).toList();
        java.util.stream.IntStream.range(0, 4096).parallel().forEach(j -> {
            int i = 4095 - j;
            require(expected.get(i).equals(climate.choose(climate.point(i - 2048, i * 17 % 4096 - 2048),
                    NETHER, Function.identity(), 190)), "Concurrent/reverse sampling");
        });
    }

    private static String biome(TerrainProfile t, TerrainProfile.Column c, int x, int z, boolean baseline) {
        return choose(t, c.theme().pool(), POOLS.get(c.theme().pool()), x, z, baseline);
    }

    private static String choose(TerrainProfile t, String pool, List<String> entries, int x, int z, boolean baseline) {
        if (baseline) {
            // Exact 0.1.3 selection; the archived JAR supplies the old terrain/theme and field.
            var p = t.field().sample(x + .5, z + .5);
            long salt = pool.hashCode();
            double variant = SeededNoise.sample(t.seed() ^ salt, p.u(), p.v(), 150)
                    + .22 * SeededNoise.sample(t.seed() ^ salt ^ 891, p.u(), p.v(), 150 * .29);
            double rank = Math.max(0, Math.min(.999999, .5 + variant * .7));
            return entries.get((int) (rank * entries.size()));
        }
        return t.biomes().choose(t.biomes().point(x + .5, z + .5), entries, Function.identity(), 190);
    }

    private static Map<String, List<String>> readPools() {
        Map<String, List<String>> pools = new HashMap<>();
        pools.put("void", List.of("minecraft:the_void"));
        // This fixture samples generation 1. V5-only themes are checked by
        // MarineAridVerification and the real-registry BiomeCompatVerification.
        for (var theme : EnumSet.range(TerrainProfile.Theme.VOID, TerrainProfile.Theme.ISLAND)) {
            if (pools.containsKey(theme.pool())) continue;
            String path = "/data/deeprealm_4th/tags/worldgen/biome/region/" + theme.pool() + ".json";
            try (var in = BiomeDistributionVerification.class.getResourceAsStream(path)) {
                if (in == null) throw new IllegalStateException("Missing shipped tag " + path);
                var matcher = Pattern.compile("\"(minecraft:[a-z_]+)\"").matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                List<String> entries = new ArrayList<>();
                while (matcher.find()) entries.add(matcher.group(1));
                require(!entries.isEmpty(), "Nonempty default tag " + path);
                pools.put(theme.pool(), entries.stream().distinct().sorted().toList());
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        return Map.copyOf(pools);
    }

    private static void render(Path path, boolean baseline) throws Exception {
        var image = new BufferedImage(1740, 840, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(new Color(20, 28, 40)); g.fillRect(0, 0, 1740, 840);
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        g.drawString((baseline ? "BEFORE / 0.1.3" : "CURRENT / 0.1.5") + "   |   biome IDs, seed 42, same coordinates", 24, 35);
        var t = new TerrainProfile(42, SpiralParameters.DEFAULT);
        double[] infernal = t.layout().armCenter(3, 900), temperate = t.layout().armCenter(5, 1400);
        int[][] centers = {{8, 8}, {(int) infernal[0], (int) infernal[1]}, {(int) temperate[0], (int) temperate[1]}};
        int[] span = {4096, 768, 768};
        String[] labels = {"Overview", "Infernal patches", "Temperate / tropical patches"};
        for (int panel = 0; panel < 3; panel++) {
            int ox = 24 + panel * 580;
            g.setColor(Color.LIGHT_GRAY); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
            g.drawString(labels[panel] + " / " + span[panel] + " blocks", ox, 66);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.drawString("Center (" + centers[panel][0] + ", " + centers[panel][1] + ")", ox, 87);
            for (int px = 0; px < 540; px++) for (int pz = 0; pz < 540; pz++) {
                // Match noise-biome quart centers, rather than inventing pixel-scale biomes.
                int x = Math.floorDiv(centers[panel][0] + px * span[panel] / 540 - span[panel] / 2, 4) * 4 + 2;
                int z = Math.floorDiv(centers[panel][1] + pz * span[panel] / 540 - span[panel] / 2, 4) * 4 + 2;
                image.setRGB(ox + px, 100 + pz, color(biome(t, t.sample(x, z), x, z, baseline)).getRGB());
            }
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        for (int i = 0; i < ALL.size(); i++) {
            int x = 24 + (i % 6) * 285, y = 665 + (i / 6) * 28;
            g.setColor(color(ALL.get(i))); g.fillRect(x, y, 18, 14);
            g.setColor(Color.LIGHT_GRAY); g.drawString(ALL.get(i).substring("minecraft:".length()), x + 24, y + 12);
        }
        g.drawString("Actual selector + shipped biome tags; algorithm preview, not an in-game screenshot.", 24, 825);
        g.dispose(); ImageIO.write(image, "png", path.toFile());
    }

    private static Color color(String id) {
        int rgb = switch (id.substring("minecraft:".length())) {
            case "the_void" -> 0x090f19;
            case "desert" -> 0xf0d18e; case "badlands" -> 0xbb7047; case "eroded_badlands" -> 0xf69b65; case "wooded_badlands" -> 0x987c31;
            case "river" -> 0x57b8de;
            case "nether_wastes" -> 0xb26364; case "crimson_forest" -> 0x781b4b; case "warped_forest" -> 0x37b0a0;
            case "soul_sand_valley" -> 0xa59ac7; case "basalt_deltas" -> 0x616775;
            case "forest" -> 0x448f5b; case "birch_forest" -> 0xa5c775; case "dark_forest" -> 0x234c44;
            case "jungle" -> 0x147733; case "sparse_jungle" -> 0x83b130; case "bamboo_jungle" -> 0xc0d94c;
            case "plains" -> 0xcbd79b; case "sunflower_plains" -> 0xf4dd43;
            case "stony_peaks" -> 0xd8e0dd; case "windswept_hills" -> 0x8395a0; case "meadow" -> 0xc3a9ce;
            case "beach" -> 0xfbe8b9; case "stony_shore" -> 0xacb7bc;
            case "ocean" -> 0x3c83d0; case "lukewarm_ocean" -> 0x3dc0d8; case "warm_ocean" -> 0x78e5df;
            case "deep_ocean" -> 0x234074; case "deep_lukewarm_ocean" -> 0x276e94;
            default -> throw new IllegalArgumentException(id);
        };
        return new Color(rgb);
    }

    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
