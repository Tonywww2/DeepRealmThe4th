package com.tony.deeprealmtheforth.worldgen.terrain;

/** Stateless noise: chunk generation order and worker scheduling cannot change it. */
public final class SeededNoise {
    private SeededNoise() {}

    public static long hash(long seed, long x, long z) {
        long n = seed ^ (x * 0x632BE59BD9B4E019L) ^ (z * 0x9E3779B97F4A7C15L);
        n = (n ^ (n >>> 30)) * 0xBF58476D1CE4E5B9L;
        n = (n ^ (n >>> 27)) * 0x94D049BB133111EBL;
        return n ^ (n >>> 31);
    }

    public static double unit(long value) { return (value >>> 11) * 0x1.0p-53; }

    public static double sample(long seed, double x, double z, double scale) {
        double sx = x / scale, sz = z / scale;
        long ix = (long) Math.floor(sx), iz = (long) Math.floor(sz);
        double fx = fade(sx - ix), fz = fade(sz - iz);
        double a = lerp(unit(hash(seed, ix, iz)), unit(hash(seed, ix + 1, iz)), fx);
        double b = lerp(unit(hash(seed, ix, iz + 1)), unit(hash(seed, ix + 1, iz + 1)), fx);
        return lerp(a, b, fz) * 2 - 1;
    }

    public static Cell cell(long seed, double x, double z, double size) {
        int gx = (int) Math.floor(x / size), gz = (int) Math.floor(z / size);
        double first = Double.POSITIVE_INFINITY, second = first;
        double ax = 0, az = 0, bx = 0, bz = 0;
        long id = 0, other = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long h = hash(seed, gx + dx, gz + dz);
                double px = (gx + dx + 0.25 + unit(h) * 0.5) * size;
                double pz = (gz + dz + 0.25 + unit(hash(h, 19, 71)) * 0.5) * size;
                double d = (x - px) * (x - px) + (z - pz) * (z - pz);
                if (d < first) {
                    second = first; bx = ax; bz = az; other = id;
                    first = d; ax = px; az = pz; id = h;
                } else if (d < second) {
                    second = d; bx = px; bz = pz; other = h;
                }
            }
        }
        double edge = (second - first) / (2 * Math.max(1, Math.hypot(ax - bx, az - bz)));
        return new Cell(id, other, ax, az, Math.sqrt(first), edge);
    }

    /** Gradient noise with rotated, decorrelated octaves; avoids value-noise plateaus. */
    public static double fractal(long seed, double x, double z, double scale, int octaves) {
        double sx = x / scale, sz = z / scale, amplitude = 1, sum = 0, weight = 0;
        for (int i = 0; i < octaves; i++) {
            sum += amplitude * gradient(seed + i * 0x9E3779B97F4A7C15L, sx, sz);
            weight += amplitude;
            double nextX = (sx * .8 - sz * .6) * 2.03 + 17.17;
            sz = (sx * .6 + sz * .8) * 2.03 - 31.73;
            sx = nextX; amplitude *= .5;
        }
        return sum / weight;
    }

    private static double gradient(long seed, double x, double z) {
        long ix = (long) Math.floor(x), iz = (long) Math.floor(z);
        double fx = x - ix, fz = z - iz, tx = fade(fx), tz = fade(fz);
        return 1.4142135623730951 * lerp(
                lerp(dot(hash(seed, ix, iz), fx, fz), dot(hash(seed, ix + 1, iz), fx - 1, fz), tx),
                lerp(dot(hash(seed, ix, iz + 1), fx, fz - 1), dot(hash(seed, ix + 1, iz + 1), fx - 1, fz - 1), tx), tz);
    }

    private static double dot(long hash, double x, double z) {
        return switch ((int) hash & 7) {
            case 0 -> x; case 1 -> -x; case 2 -> z; case 3 -> -z;
            case 4 -> (x + z) * .7071067811865476;
            case 5 -> (x - z) * .7071067811865476;
            case 6 -> (-x + z) * .7071067811865476;
            default -> (-x - z) * .7071067811865476;
        };
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    public record Cell(long id, long neighborId, double x, double z, double distance, double edgeDistance) {}
}
