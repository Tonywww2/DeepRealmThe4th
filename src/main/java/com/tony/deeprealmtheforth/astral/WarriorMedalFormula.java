package com.tony.deeprealmtheforth.astral;

/** Final-attack multiplier fraction granted by the warrior medal. */
public final class WarriorMedalFormula {
    private WarriorMedalFormula() {}

    /** Strength 15 returns ln(2), which means about +69.3% final attack damage. */
    public static double attackFraction(double strength) {
        if (!Double.isFinite(strength)) {
            throw new IllegalArgumentException("Strength score must be finite");
        }
        // Negative strength is treated as zero before applying the logarithm.
        return Math.log1p(Math.max(0.0, strength) / 15.0);
    }
}
