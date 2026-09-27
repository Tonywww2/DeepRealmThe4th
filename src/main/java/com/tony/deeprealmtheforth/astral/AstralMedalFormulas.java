package com.tony.deeprealmtheforth.astral;

/** Score-to-attribute formulas shared by Curios and tooltip previews. */
public final class AstralMedalFormulas {
    private AstralMedalFormulas() {}

    public static double movementFraction(double agility) {
        return Math.min(0.30, Math.max(0, agility) * 0.01);
    }

    public static double maxHealthBonus(double constitution) {
        return Math.min(10, Math.max(0, constitution) * 0.5);
    }
}
