package com.tonywww.deeprealm4th.astral.node;

import com.tonywww.deeprealm4th.astral.AstralFillers;
import com.tonywww.deeprealm4th.astral.FillerContext;
import com.tonywww.deeprealm4th.astral.FillerDefinition;
import com.tonywww.deeprealm4th.astral.ScoreType;
import com.tonywww.deeprealm4th.astral.score.AstralNumber;
import com.tonywww.deeprealm4th.astral.score.BigScoreSheet;
import com.tonywww.deeprealm4th.astral.score.ScoreSheet;
import java.util.EnumMap;
import java.util.Map;

/** Maps fixed built-in rules to the same per-cell engine used by data-driven gems. */
public final class StaticFillerNodes {
    private StaticFillerNodes() {}

    public static FillerNode resolve(String itemId, FillerDefinition definition) {
        String role = !definition.conversionRules().isEmpty() ? "conversion"
                : !definition.bonusRules().isEmpty() ? "amplifier"
                : !definition.medalRules().isEmpty() || !definition.bigMedalRules().isEmpty()
                        ? "medal" : "producer";
        return FillerNode.builder().kindKey(itemId).ruleKey(itemId).role(role)
                .signedScores(true)
                .intrinsic((context, data, output) -> {
                    for (ScoreType type : ScoreType.values())
                        output.add(type, definition.baseScores().get(type));
                })
                .reads((context, data) -> definition.conversionRules().isEmpty() ? ReadPlan.empty()
                        : ReadPlan.builder().finalScores("prior_modifiers", context.cells(),
                                FillerFilter.where(cell -> priorModifier(context, cell))).build())
                .calculate((context, data, inputs, output) -> {
                    ScoreSheet baseline = finite(baseTotal(context));
                    for (FillerDefinition.ScoreRule rule : definition.bonusRules())
                        rule.apply(context, baseline, output::add);
                    if (definition.conversionRules().isEmpty()) return;
                    BigScoreSheet current = baseTotal(context);
                    FillerResult ownBase = context.baseAt(context.cellAt(context.x(), context.y()));
                    for (ScoreType type : ScoreType.values())
                        current = current.plus(type, output.get(type).subtract(ownBase.baseScores().get(type)));
                    for (FillerResult prior : inputs.results("prior_modifiers")) {
                        for (ScoreType type : ScoreType.values())
                            current = current.plus(type, prior.finalScores().get(type)
                                    .subtract(prior.baseScores().get(type)));
                    }
                    ScoreSheet before = finite(current);
                    for (FillerDefinition.ScoreRule rule : definition.conversionRules()) {
                        EnumMap<ScoreType, Double> changes = new EnumMap<>(ScoreType.class);
                        rule.apply(context, before, (type, amount) -> {
                            if (type == null || !Double.isFinite(amount))
                                throw new IllegalArgumentException("Invalid conversion amount");
                            changes.merge(type, amount, Double::sum);
                        });
                        double fraction = 1;
                        for (Map.Entry<ScoreType, Double> change : changes.entrySet()) {
                            if (change.getValue() < 0) {
                                double available = Math.max(0, before.get(change.getKey()));
                                fraction = Math.min(fraction, available / -change.getValue());
                            }
                        }
                        for (Map.Entry<ScoreType, Double> change : changes.entrySet()) {
                            double limited = change.getValue() * fraction;
                            output.add(change.getKey(), limited);
                            before = before.plus(change.getKey(), limited);
                        }
                    }
                }).build();
    }

    private static boolean priorModifier(FillerContext context, CellView cell) {
        if (!cell.effective() || cell.index() == context.index()) return false;
        FillerDefinition other = AstralFillers.find(cell.itemCopy());
        if (other == null || other.nodeResolver() != null) return false;
        return !other.bonusRules().isEmpty() || cell.index() < context.index()
                && !other.conversionRules().isEmpty();
    }

    private static BigScoreSheet baseTotal(FillerContext context) {
        BigScoreSheet total = BigScoreSheet.EMPTY;
        for (CellView cell : context.cells()) {
            if (!cell.effective()) continue;
            FillerResult base = context.baseAt(cell);
            if ("base".equals(base.state())) total = total.plus(base.baseScores());
        }
        return total;
    }

    private static ScoreSheet finite(BigScoreSheet values) {
        EnumMap<ScoreType, Double> result = new EnumMap<>(ScoreType.class);
        for (ScoreType type : ScoreType.values()) {
            AstralNumber amount = values.get(type);
            double converted = amount.toFiniteDouble();
            result.put(type, converted);
        }
        return ScoreSheet.of(result);
    }
}
