package com.tony.deeprealmtheforth.astral;

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
