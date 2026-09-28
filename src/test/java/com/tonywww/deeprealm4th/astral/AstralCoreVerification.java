package com.tonywww.deeprealm4th.astral;

import com.tonywww.deeprealm4th.astral.container.ContainerLayout;
import com.tonywww.deeprealm4th.astral.score.AstralAttributeIds;
import com.tonywww.deeprealm4th.astral.score.AstralMedalFormulas;
import com.tonywww.deeprealm4th.astral.score.ScoreSheet;
import com.tonywww.deeprealm4th.astral.score.WarriorMedalFormula;

import java.util.List;
import java.util.Map;

/** Standalone checks for the persisted grid contract and score value boundaries. */
public final class AstralCoreVerification {
    private AstralCoreVerification() {}

    public static void main(String[] args) {
        ContainerLayout base = ContainerLayout.base();
        require(base.width() == 12 && base.height() == 9, "base dimensions");
        require(base.openCount() == 30, "6 by 5 open area");
        require(base.isOpen(3, 2) && base.isOpen(8, 6), "open area corners");
        require(!base.isOpen(2, 2) && !base.isOpen(9, 6), "closed area corners");
        require(base.rows().equals(ContainerLayout.BASE_ROWS), "round-trip layout rows");

        ContainerLayout addon = ContainerLayout.parse(2, 3, List.of("X.", ".X", "XX"));
        require(addon.size() == 6 && addon.openCount() == 4, "addon dimensions");
        ContainerLayout cross = ContainerLayout.parse(5, 5,
                List.of("..X..", "..X..", "XXXXX", "..X..", "..X.."));
        require(cross.openCount() == 9 && cross.isOpen(2, 2) && !cross.isOpen(0, 0),
                "cross-shaped addon layout");
        expectFailure(() -> ContainerLayout.parse(12, 9, List.of("XXXXXXXXXXXX")), "wrong row count");
        expectFailure(() -> ContainerLayout.parse(2, 1, List.of("X?")), "invalid symbol");

        ScoreSheet scores = ScoreSheet.of(Map.of(ScoreType.STRENGTH, 3.5));
        require(scores.get(ScoreType.STRENGTH) == 3.5, "fractional score retained");
        require(scores.get(ScoreType.MAGIC) == 0, "unassigned score stays zero");
        expectFailure(() -> ScoreSheet.EMPTY.plus(ScoreType.STRENGTH, Double.NaN), "non-finite score");
        require(WarriorMedalFormula.attackFraction(0) == 0, "zero strength has no bonus");
        require(Math.abs(WarriorMedalFormula.attackFraction(15) - Math.log(2)) < 1e-12,
                "15 strength gives about 69.3 percent");
        require(WarriorMedalFormula.attackFraction(-15) == 0, "negative strength is treated as zero");
        require(WarriorMedalFormula.attackFraction(-20) == 0, "negative strength stays at zero");
        require(WarriorMedalFormula.attackFraction(-45) == 0, "large negative strength stays at zero");
        require(Math.abs(AstralMedalFormulas.movementFraction(15) - 0.15) < 1e-12,
                "15 agility gives 15 percent movement speed");
        require(AstralMedalFormulas.movementFraction(40) == 0.30, "movement speed is capped");
        require(AstralMedalFormulas.movementFraction(-5) == 0, "negative agility gives no speed");
        require(AstralMedalFormulas.maxHealthBonus(10) == 5, "10 constitution gives 5 health");
        require(AstralMedalFormulas.maxHealthBonus(30) == 10, "health bonus is capped");
        require(AstralMedalFormulas.maxHealthBonus(-5) == 0, "negative constitution gives no health");

        double[] bonus = {0};
        var percentage = ScoreRules.percentOf(ScoreType.STRENGTH, ScoreType.PERCEPTION, 0.25, 4);
        percentage.apply(null, ScoreSheet.of(Map.of(ScoreType.STRENGTH, 10)),
                (type, amount) -> bonus[0] += amount);
        require(bonus[0] == 2.5, "percentage uses base-phase source");
        bonus[0] = 0;
        percentage.apply(null, ScoreSheet.of(Map.of(ScoreType.STRENGTH, 30)),
                (type, amount) -> bonus[0] += amount);
        require(bonus[0] == 4, "percentage cap applies per rule");
        bonus[0] = 0;
        percentage.apply(null, ScoreSheet.of(Map.of(ScoreType.STRENGTH, -10)),
                (type, amount) -> bonus[0] += amount);
        require(bonus[0] == 0, "negative source is clamped to zero");
        expectFailure(() -> ScoreRules.percentOf(ScoreType.STRENGTH,
                ScoreType.STRENGTH, -0.1, 4), "negative percentage");
        expectFailure(() -> ScoreRules.percentWhenAdjacentTagAtLeast(
                "deeprealm_4th:astral/medals", 0, ScoreType.MAGIC, ScoreType.MAGIC, 0.1),
                "zero adjacent-tag threshold");
        expectFailure(() -> ScoreRules.percentWhenAdjacentTagAtLeast(
                "deeprealm_4th:astral/medals", 5, ScoreType.MAGIC, ScoreType.MAGIC, 0.1),
                "more than four adjacent cells");
        double[] health = {0};
        ScoreRules.attributePerAllScores(AstralAttributeIds.MAX_HEALTH, 0.1).apply(null,
                ScoreSheet.of(Map.of(ScoreType.STRENGTH, 1.0, ScoreType.AGILITY, 2.0,
                        ScoreType.INTELLIGENCE, 3.0, ScoreType.CONSTITUTION, 4.0,
                        ScoreType.PERCEPTION, 5.0, ScoreType.MAGIC, -6.0)),
                (attribute, amount) -> health[0] += amount);
        require(Math.abs(health[0] - 1.5) < 1e-12,
                "medal sums nonnegative values from all six scores");
        System.out.println("Astral core verification passed");
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }

    private static void expectFailure(Runnable action, String description) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(description + " should have failed");
    }
}
