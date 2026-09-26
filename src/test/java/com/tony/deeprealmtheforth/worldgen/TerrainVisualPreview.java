package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Same renderer can run against the archived previous JAR for a true sampler comparison. */
public final class TerrainVisualPreview {
    private TerrainVisualPreview() {}

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Files.createDirectories(out.toAbsolutePath().getParent());
        render(out, new TerrainProfile(42, SpiralParameters.DEFAULT), args.length > 1 ? args[1] : "Terrain relief");
    }

    public static void render(Path path, TerrainProfile terrain, String label) throws Exception {
        BufferedImage image = new BufferedImage(1200, 730, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(18, 24, 34)); g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
        g.drawString(label, 28, 34);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.drawString("Regional relief / 2048 x 2048 blocks", 28, 68);
        g.drawString("Cliff rim / 256 x 256 blocks", 620, 68);
        map(image, terrain, 28, 84, 552, 1024);
        map(image, terrain, 620, 84, 552, 128);
        g.setColor(new Color(201, 210, 223));
        g.drawString("Actual Java terrain sampler, seed 42. Height + slope shading; not an in-game screenshot.", 28, 672);
        g.drawString("Void: dark   Water: blue   Lava: orange   Land: low brown -> high pale green", 28, 701);
        g.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    private static void map(BufferedImage image, TerrainProfile terrain, int ox, int oy, int size, int radius) {
        double step = radius * 2.0 / size;
        TerrainProfile.Column[][] columns = new TerrainProfile.Column[size + 2][size + 2];
        for (int px = 0; px < size + 2; px++) for (int pz = 0; pz < size + 2; pz++) {
            int x = (int) Math.floor(8 + (px - 1 - size * .5) * step);
            int z = (int) Math.floor(8 + (pz - 1 - size * .5) * step);
            columns[px][pz] = terrain.sample(x, z);
        }
        for (int px = 1; px <= size; px++) for (int pz = 1; pz <= size; pz++) {
            var c = columns[px][pz];
            Color color = new Color(9, 14, 23);
            if (c.land()) {
                if (c.wet()) color = c.fluid() == TerrainProfile.Fluid.WATER
                        ? new Color(41, 113, 160) : new Color(246, 125, 41);
                else {
                    double height = Math.max(0, Math.min(1, (c.top() + 55) / 185.0));
                    double sx = (columns[px + 1][pz].surface() - columns[px - 1][pz].surface()) / (2 * step);
                    double sz = (columns[px][pz + 1].surface() - columns[px][pz - 1].surface()) / (2 * step);
                    double light = Math.max(0, (.5 * sx + .4 * sz + 1) / Math.sqrt(1.41 * (1 + sx * sx + sz * sz)));
                    double shade = .35 + .65 * light;
                    color = new Color((int) ((126 + 65 * height) * shade), (int) ((100 + 104 * height) * shade),
                            (int) ((71 + 100 * height) * shade));
                }
            }
            image.setRGB(ox + px - 1, oy + pz - 1, color.getRGB());
        }
    }
}
