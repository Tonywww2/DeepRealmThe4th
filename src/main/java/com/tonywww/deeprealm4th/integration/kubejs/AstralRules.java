package com.tonywww.deeprealm4th.integration.kubejs;

import com.tonywww.deeprealm4th.astral.score.AstralAttributeIds;
import com.tonywww.deeprealm4th.astral.FillerDefinition;
import com.tonywww.deeprealm4th.astral.ScoreRules;
import com.tonywww.deeprealm4th.astral.ScoreType;
import com.tonywww.deeprealm4th.astral.score.WarriorMedalFormula;

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
        return ScoreRules.countAtLeast(itemId, minimum, target, amount);
    }

    /** Adds the score once when the named filler is in an active orthogonal neighbor. */
    public static FillerDefinition.ScoreRule whenAdjacent(String itemId,
            ScoreType target, double amount) {
        return ScoreRules.adjacent(itemId, target, amount);
    }

    public static FillerDefinition.ScoreRule perScore(ScoreType source,
            ScoreType target, double multiplier) {
        return ScoreRules.perScore(source, target, multiplier);
    }

    public static FillerDefinition.ScoreRule percentOf(ScoreType source,
            ScoreType target, double rate, double cap) {
        return ScoreRules.percentOf(source, target, rate, cap);
    }

    public static FillerDefinition.ScoreRule percentWhenAdjacent(String itemId,
            ScoreType source, ScoreType target, double rate, double cap) {
        return ScoreRules.percentWhenAdjacent(itemId, source, target, rate, cap);
    }

    public static FillerDefinition.ScoreRule percentWhenCountAtLeast(String itemId, int minimum,
            ScoreType source, ScoreType target, double rate, double cap) {
        return ScoreRules.percentWhenCountAtLeast(itemId, minimum, source, target, rate, cap);
    }

    public static FillerDefinition.ScoreRule percentWhenAdjacentTagAtLeast(String tagId, int minimum,
            ScoreType source, ScoreType target, double rate) {
        return ScoreRules.percentWhenAdjacentTagAtLeast(tagId, minimum, source, target, rate);
    }

    public static FillerDefinition.ScoreRule whenExperienceLevelAbove(int level,
            ScoreType target, double amount) {
        return ScoreRules.whenExperienceLevelAbove(level, target, amount);
    }

    public static FillerDefinition.ScoreRule whenHealthAbove(double health,
            ScoreType target, double amount) {
        return ScoreRules.whenHealthAbove(health, target, amount);
    }

    public static FillerDefinition.ScoreRule whenHealthAtMost(double health,
            ScoreType target, double amount) {
        return ScoreRules.whenHealthAtMost(health, target, amount);
    }

    public static FillerDefinition.ScoreRule whenFoodAtLeast(int food,
            ScoreType target, double amount) {
        return ScoreRules.whenFoodAtLeast(food, target, amount);
    }

    public static FillerDefinition.ScoreRule whenNightVision(ScoreType target, double amount) {
        return ScoreRules.whenNightVision(target, amount);
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

    public static FillerDefinition.MedalRule attributePerAllScores(String attributeId, double perPoint) {
        return ScoreRules.attributePerAllScores(attributeId, perPoint);
    }

    /** Multiplies final attribute value; 0.01 means +1% per score point. */
    public static FillerDefinition.MedalRule attributeMultiplyTotalPerScore(ScoreType source,
            String attributeId, double fractionPerPoint) {
        return ScoreRules.attributeMultiplyTotalPerScore(source, attributeId, fractionPerPoint);
    }

    /** The built-in warrior medal's logarithmic final-attack multiplier. */
    public static FillerDefinition.MedalRule warriorMedal() {
        return (context, scores, output) -> output.multiplyTotal(
                AstralAttributeIds.ATTACK_DAMAGE,
                WarriorMedalFormula.attackFraction(scores.get(ScoreType.STRENGTH)));
    }

    private static void finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Rule value must be finite");
    }
}
