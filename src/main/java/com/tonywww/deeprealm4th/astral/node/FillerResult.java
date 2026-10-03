package com.tonywww.deeprealm4th.astral.node;

import com.tonywww.deeprealm4th.astral.score.AstralNumber;
import com.tonywww.deeprealm4th.astral.score.BigScoreSheet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** One physical cell's independently computed base and final scores. */
public record FillerResult(int cellRef, String kindKey, String variantKey, String ruleKey, String role,
        BigScoreSheet baseScores, BigScoreSheet finalScores, boolean computed, String state,
        List<String> diagnostics, Map<String, List<Integer>> sources) {
    public FillerResult {
        diagnostics = List.copyOf(diagnostics);
        Map<String, List<Integer>> copy = new LinkedHashMap<>();
        sources.forEach((name, cells) -> copy.put(name, List.copyOf(cells)));
        sources = Map.copyOf(copy);
    }
    public FillerResult(int cellRef, String kindKey, String variantKey, String ruleKey, String role,
            BigScoreSheet baseScores, BigScoreSheet finalScores, boolean computed, String state,
            List<String> diagnostics) {
        this(cellRef, kindKey, variantKey, ruleKey, role, baseScores, finalScores,
                computed, state, diagnostics, Map.of());
    }
    public AstralNumber baseTotal() { return baseScores.total(); }
    public AstralNumber finalTotal() { return finalScores.total(); }
    public AstralNumber gainTotal() {
        return finalTotal().subtract(baseTotal()).max(AstralNumber.ZERO);
    }
    public AstralNumber gainRatio() {
        return finalTotal().sign() > 0 ? gainTotal().divide(finalTotal()) : AstralNumber.ZERO;
    }
    public static FillerResult unavailable(int index, String state) {
        return new FillerResult(index, "", "", "", "", BigScoreSheet.EMPTY, BigScoreSheet.EMPTY,
                false, state, List.of(state));
    }
}
