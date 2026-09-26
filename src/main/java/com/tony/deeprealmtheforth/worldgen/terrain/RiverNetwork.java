package com.tony.deeprealmtheforth.worldgen.terrain;

import com.tony.deeprealmtheforth.worldgen.layout.SpiralLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Continuous distance to meandering drainage paths, including rounded sources.
 * Neighboring catchments contribute valley envelopes, not discontinuous cell borders. */
public final class RiverNetwork {
    private static final double SIZE = 420;
    private record Key(long seed, int x, int z) {}
    private record Segment(double x, double y, double dx, double dy, double length2, double w, double dw) {
        double distance(double px, double py) {
            double t = Math.max(0, Math.min(1, ((px - x) * dx + (py - y) * dy) / length2));
            return Math.hypot(px - x - t * dx, py - y - t * dy) - w - t * dw;
        }
    }
    private record Basin(long id, double x, double z, double mouth, double lakeX,
                         double lakeRadius, double lakeAxis, Segment[] segments) {
        double distance(double u, double v) {
            double px = (u - x) * ((id & 1) == 0 ? 1 : -1), py = v - z;
            double d = Math.hypot((px - lakeX) / lakeAxis, py - mouth + 4) - lakeRadius
                    - 2 * SeededNoise.fractal(id ^ 3711, px, py, 32, 2);
            for (Segment segment : segments) d = Math.min(d, segment.distance(px, py));
            return d;
        }
    }
    private static final ThreadLocal<Map<Key, Basin>> CACHE = ThreadLocal.withInitial(() ->
            new LinkedHashMap<>(128, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Key, Basin> e) { return size() > 128; }
            });
    private RiverNetwork() {}

    public static Sample sample(long seed, double u, double v, SpiralLayout layout) {
        int gx = (int) Math.floor(u / SIZE), gz = (int) Math.floor(v / SIZE);
        double nearest = Double.POSITIVE_INFINITY, valley = 1024;
        int level = 64; long id = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            Key key = new Key(seed, gx + dx, gz + dz);
            Basin b = CACHE.get().computeIfAbsent(key, RiverNetwork::create);
            // Paths fit within radius 180. Outside 440 the valley is above
            // every supported terrain height, so omitting it cannot carve a seam.
            if (Math.hypot(u - b.x, v - b.z) > 440) continue;
            double d = b.distance(u, v);
            int y = (int) Math.floor(layout.baseHeight(Math.hypot(b.x, b.z))) - 9;
            double bank = Math.max(0, d);
            valley = Math.min(valley, y + .16 * bank + .009 * bank * bank);
            if (d < nearest) { nearest = d; level = y; id = b.id; }
        }
        return new Sample(nearest, level, id, valley);
    }

    private static Basin create(Key k) {
        long key = SeededNoise.hash(k.seed ^ 67181, k.x, k.z);
        double cx = (k.x + .5) * SIZE + random(key, 1) * 30 - 15;
        double cz = (k.z + .5) * SIZE + random(key, 2) * 30 - 15;
        double mouth = -95 - random(key, 3) * 25, head = 96 + random(key, 4) * 46;
        List<Segment> segments = new ArrayList<>();
        path(segments, key, mouth, head, 0, 0);
        for (int side : new int[]{-1, 1}) {
            if (random(key, side + 18) < .25) continue;
            double join = -30 + random(key, side + 23) * 70;
            path(segments, key, join, Math.min(head, join + 72 + random(key, side + 28) * 36),
                    side, 70 + random(key, side + 33) * 32);
        }
        return new Basin(key, cx, cz, mouth, stem(key, mouth), 18 + 12 * random(key, 41),
                1.05 + .35 * random(key, 42), segments.toArray(Segment[]::new));
    }

    private static void path(List<Segment> segments, long key, double from, double to, int side, double reach) {
        int count = (int) Math.ceil((to - from) / 7);
        double oldX = 0, oldY = 0, oldW = 0;
        for (int i = 0; i <= count; i++) {
            double t = (double) i / count, y = from + t * (to - from);
            double x = stem(key, y) + side * (t * t * (2 - t) * reach
                    + t * (1 - t) * 112 * SeededNoise.fractal(key ^ side, y, 37, 45, 2));
            double w = (side == 0 ? 4.5 + (1 - t) * (5 + random(key, 5) * 3) : 4.3 + (1 - t) * 2.8)
                    * (1 + .16 * SeededNoise.fractal(key ^ 391, y, 17, 65, 2));
            if (i > 0) segments.add(new Segment(oldX, oldY, x - oldX, y - oldY,
                    (x - oldX) * (x - oldX) + (y - oldY) * (y - oldY), oldW, w - oldW));
            oldX = x; oldY = y; oldW = w;
        }
    }
    private static double stem(long key, double y) {
        return 38 * SeededNoise.fractal(key ^ 119, y, 73, 135, 2)
                + 15 * SeededNoise.fractal(key ^ 811, y, 31, 65, 2);
    }
    private static double random(long key, int salt) { return SeededNoise.unit(SeededNoise.hash(key, salt, 791)); }
    public record Sample(double distance, int level, long catchment, double valleyFloor) {}
}
