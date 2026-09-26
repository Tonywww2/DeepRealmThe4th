package com.tony.deeprealmtheforth.compat.kubejs;

import com.tony.deeprealmtheforth.astral.FillerDefinition;
import com.tony.deeprealmtheforth.astral.ScoreRules;
import com.tony.deeprealmtheforth.astral.ScoreType;
import com.tony.deeprealmtheforth.astral.WarriorMedalFormula;

/** Data-only rule factories shared by Forge 1.20.1 and NeoForge 1.21.1 scripts. */
public final class AstralRules {
    private AstralRules() {}

    public static FillerDefinition.ScoreRule whenHealthBelow(double health,
            ScoreType target, double amount) {
        finite(health);
        if (health <= 0) throw new IllegalArgumentException("Health threshold must be positive");
        return ScoreRules.when(context -> context.player().getHealth() < health, target, amount);
    }

    public static FillerDefinition.ScoreRule whenFillerCount(String itemId, int minimum,
            ScoreType target, double amount) {
        if (itemId == null || !itemId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
            throw new IllegalArgumentException("Expected a namespaced item ID, got: " + itemId);
        }
        if (minimum < 1) throw new IllegalArgumentException("Minimum filler count must be positive");
        return ScoreRules.when(context -> context.count(itemId) >= minimum, target, amount);
    }

    /** Adds the score once when the named filler is in an open orthogonal neighbor. */
    public static FillerDefinition.ScoreRule whenAdjacent(String itemId,
            ScoreType target, double amount) {
        return ScoreRules.adjacent(itemId, target, amount);
    }

    public static FillerDefinition.ScoreRule perScore(ScoreType source,
            ScoreType target, double multiplier) {
        return ScoreRules.perScore(source, target, multiplier);
    }

    /** Deducts a fraction of the source phase score and adds it to the target at a rate. */
    public static FillerDefinition.ScoreRule convert(ScoreType source,
            ScoreType target, double fraction) {
        return convert(source, target, fraction, 1);
    }

    public static FillerDefinition.ScoreRule convert(ScoreType source,
            ScoreType target, double fraction, double targetMultiplier) {
        return ScoreRules.convert(source, target, fraction, targetMultiplier);
    }

    public static FillerDefinition.MedalRule attributePerScore(ScoreType source,
            String attributeId, double multiplier) {
        return ScoreRules.attributePerScore(source, attributeId, multiplier);
    }

    /** Multiplies final attribute value; 0.01 means +1% per score point. */
    public static FillerDefinition.MedalRule attributeMultiplyTotalPerScore(ScoreType source,
            String attributeId, double fractionPerPoint) {
        return ScoreRules.attributeMultiplyTotalPerScore(source, attributeId, fractionPerPoint);
    }

    /** The built-in warrior medal's logarithmic final-attack multiplier. */
    public static FillerDefinition.MedalRule warriorMedal() {
        return (context, scores, output) -> output.multiplyTotal(
                "minecraft:generic.attack_damage",
                WarriorMedalFormula.attackFraction(scores.get(ScoreType.STRENGTH)));
    }

    private static void finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Rule value must be finite");
    }
}
