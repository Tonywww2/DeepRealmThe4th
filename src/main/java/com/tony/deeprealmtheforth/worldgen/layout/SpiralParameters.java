package com.tony.deeprealmtheforth.worldgen.layout;

/** Immutable, serialized geometry. Changing these values affects newly generated chunks. */
public record SpiralParameters(double centerX, double centerZ, double coreRadius,
                               double plungeRadius, double twist, double rotation,
                               double referenceRadius, double radialScale, double landFraction) {
    // Diameters: seven chunks for the central cliff, three chunks for the open core.
    public static final SpiralParameters DEFAULT = new SpiralParameters(8, 8, 24, 56, 1.44, 0, 256, 430, 0.75);
    public static final int MIN_Y = -64;
    public static final int HEIGHT = 512;

    public SpiralParameters {
        if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || !Double.isFinite(rotation) || !Double.isFinite(twist) || twist <= 0 || twist > 4
                || !Double.isFinite(coreRadius) || coreRadius < 4
                || !Double.isFinite(plungeRadius) || plungeRadius <= coreRadius + 16
                || !Double.isFinite(referenceRadius) || referenceRadius < 16
                || !Double.isFinite(radialScale) || radialScale < 64
                || !Double.isFinite(landFraction) || landFraction < 0.1 || landFraction > 0.9) {
            throw new IllegalArgumentException("Invalid spiral geometry parameters");
        }
    }
}
