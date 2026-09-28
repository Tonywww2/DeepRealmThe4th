package com.tonywww.deeprealm4th.worldgen.layout;

/** World-coordinate geometry shared by generation, biome selection and previews. */
public final class SpiralLayout {
    private static final double TAU = Math.PI * 2;
    private static final double PAIR_ANGLE = Math.PI / 2;
    private final SpiralParameters parameters;

    public SpiralLayout(SpiralParameters parameters) {
        this.parameters = parameters;
    }

    public SpiralParameters parameters() { return parameters; }

    public Point sample(double x, double z) {
        double dx = x - parameters.centerX();
        double dz = z - parameters.centerZ();
        double radius = Math.hypot(dx, dz);
        double phase = Math.atan2(dz, dx) + parameters.twist()
                * Math.log(Math.max(radius, parameters.coreRadius()) / parameters.referenceRadius())
                - parameters.rotation();
        phase = phase - Math.floor(phase / TAU) * TAU;
        // Four land/void pairs. Default widths are 67.5 degrees / 22.5 degrees.
        int pair = Math.min(3, (int) (phase / PAIR_ANGLE));
        double withinPair = phase - pair * PAIR_ANGLE;
        double landAngle = PAIR_ANGLE * parameters.landFraction();
        boolean landBand = withinPair < landAngle;
        int arm = pair * 2 + (landBand ? 1 : 2);
        double local = landBand ? withinPair : withinPair - landAngle;
        double width = landBand ? landAngle : PAIR_ANGLE - landAngle;
        // Local normal distance to a logarithmic spiral, not just an angular fraction.
        double edge = radius * Math.min(local, width - local)
                / Math.sqrt(1 + parameters.twist() * parameters.twist());
        boolean land = (arm & 1) == 1 && radius > parameters.coreRadius() && edge > 1.5;
        return new Point(radius, arm, edge, land);
    }

    public double baseHeight(double radius) {
        // The plunge radius is the TOP of the central cliff, not its midpoint.
        // Anchor it to Y=60 independently of the deliberately slow outer rise.
        double b = parameters.plungeRadius(), a = Math.max(parameters.coreRadius() + 4, b - 26);
        double ya = -52, yb = 60;
        if (radius <= a) return ya + .25 * (radialHeight(radius) - radialHeight(a));
        if (radius >= b) return yb + .1 * (radialHeight(radius) - radialHeight(b));
        double t = (radius - a) / (b - a), t2 = t * t, t3 = t2 * t;
        double ma = 52.5 / parameters.radialScale() * Math.exp(-a / parameters.radialScale());
        double mb = 21 / parameters.radialScale() * Math.exp(-b / parameters.radialScale());
        return (2 * t3 - 3 * t2 + 1) * ya + (t3 - 2 * t2 + t) * (b - a) * ma
                + (-2 * t3 + 3 * t2) * yb + (t3 - t2) * (b - a) * mb;
    }

    public double radialHeight(double radius) { return 230 - 210 * Math.exp(-radius / parameters.radialScale()); }

    public double[] armCenter(int arm, double radius) {
        double landAngle = PAIR_ANGLE * parameters.landFraction();
        double phase = ((arm - 1) / 2) * PAIR_ANGLE
                + ((arm & 1) == 1 ? landAngle / 2 : landAngle + (PAIR_ANGLE - landAngle) / 2);
        double angle = phase - parameters.twist()
                * Math.log(radius / parameters.referenceRadius()) + parameters.rotation();
        return new double[]{parameters.centerX() + Math.cos(angle) * radius,
                parameters.centerZ() + Math.sin(angle) * radius};
    }

    public static double smooth(double low, double high, double value) {
        double t = Math.max(0, Math.min(1, (value - low) / (high - low)));
        return t * t * (3 - 2 * t);
    }

    public record Point(double radius, int arm, double edgeDistance, boolean land) {}
}
