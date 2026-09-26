package com.tony.deeprealmtheforth.worldgen.terrain;

import com.tony.deeprealmtheforth.worldgen.layout.SpiralLayout;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;

/** Seeded shear in an unwound coordinate system; no chunk-local decisions or angular seam. */
public final class VortexField {
    private final long seed;
    private final SpiralParameters p;
    private final boolean detailedEdges;
    public VortexField(long seed, SpiralParameters p) { this(seed,p,false); }
    public VortexField(long seed, SpiralParameters p,boolean detailedEdges) { this.seed = seed; this.p = p;this.detailedEdges=detailedEdges; }

    /**
     * Relief needs bounded spatial shear: multiplying noisy ANGLES by radius
     * compresses hills/rivers into one-block spikes far from the center. Keep
     * the logarithmic winding, then perturb POSITIONS by a fixed block offset.
     * The independent boundary field below deliberately remains unchanged.
     */
    public Coordinates relief(double x, double z) {
        double dx = x - p.centerX(), dz = z - p.centerZ(), radius = Math.hypot(dx, dz);
        double phase = Math.atan2(dz, dx)
                + p.twist() * Math.log(Math.max(radius, p.coreRadius()) / p.referenceRadius()) - p.rotation();
        double u = radius * Math.cos(phase), v = radius * Math.sin(phase);
        return new Coordinates(u + 18 * SeededNoise.sample(seed ^ 3881, u, v, 180),
                v + 18 * SeededNoise.sample(seed ^ 7581, u, v, 180));
    }

    public Point sample(double x, double z) {
        double dx = x - p.centerX(), dz = z - p.centerZ(), radius = Math.hypot(dx, dz);
        double fade = SpiralLayout.smooth(p.coreRadius(), 150, radius);
        double bend = .20 * SeededNoise.sample(seed ^ 1859, x, z, 390)
                + .13 * SeededNoise.fractal(seed ^ 741, x, z, 130, 3)
                + .055 * SeededNoise.fractal(seed ^ 983, x, z, 45, 2);
        double phase = Math.atan2(dz, dx) + p.twist() * Math.log(Math.max(radius, p.coreRadius()) / p.referenceRadius())
                - p.rotation() + fade * bend;
        double u = radius * Math.cos(phase), v = radius * Math.sin(phase);
        double ripple = fade * (22 * SeededNoise.fractal(seed ^ 419, u, v, 75, 3)
                + 10 * SeededNoise.fractal(seed ^ 719, u, v, 28, 2));
        phase += ripple / Math.max(48, radius);
        double wrapped = phase - Math.floor(phase / (Math.PI * 2)) * Math.PI * 2;
        int pair = Math.min(3, (int) (wrapped / (Math.PI / 2)));
        double within = wrapped - pair * Math.PI / 2;
        double width = Math.PI / 2 * (p.landFraction()
                + fade * .055 * SeededNoise.sample(seed ^ 1777, u, v, 220));
        boolean band = within < width;
        double edge = radius * Math.min(band ? within : within - width,
                band ? width - within : Math.PI / 2 - within) / Math.sqrt(1 + p.twist() * p.twist());
        if(detailedEdges && band && edge<40 && radius>p.plungeRadius()+144) {
            // Fixed-block erosion only in the outer strip. Interior land cannot tear.
            double detail=9+12*SeededNoise.fractal(seed^817731,x,z,43,3)
                    +5*SeededNoise.fractal(seed^329197,x,z,15,2);
            edge-=Math.max(0,detail)*(1-SpiralLayout.smooth(22,40,edge))
                    *SpiralLayout.smooth(p.plungeRadius()+144,p.plungeRadius()+240,radius);
        }
        // Only the warped OUTER boundaries cut land. No independent interior tear mask.
        boolean land = band && radius > p.coreRadius() && edge > 1.5;
        double wx = u + 36 * SeededNoise.sample(seed ^ 3881, u, v, 100);
        double wz = v + 36 * SeededNoise.sample(seed ^ 7581, u, v, 100);
        return new Point(radius, pair * 2 + (band ? 1 : 2), edge, land, wx, wz);
    }

    public record Point(double radius, int arm, double edgeDistance, boolean land, double u, double v) {}
    public record Coordinates(double u, double v) {}
}
