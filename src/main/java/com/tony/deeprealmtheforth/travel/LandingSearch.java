package com.tony.deeprealmtheforth.travel;

/** Bounded, nearest-ring-first column search, independent of world or loader APIs. */
public final class LandingSearch {
    private LandingSearch() {}

    @FunctionalInterface
    public interface Probe<T> {
        /** Return a safe landing, or null to reject this column without changing it. */
        T at(int x, int z);
    }

    public static <T> T find(int x, int z, int radius, Probe<T> probe) {
        if (radius < 0) throw new IllegalArgumentException("Negative search radius");
        // A coarser stride misses isolated safe columns and three of four parities.
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    T landing = probe.at(x + dx, z + dz);
                    if (landing != null) return landing;
                }
            }
        }
        return null;
    }
}
