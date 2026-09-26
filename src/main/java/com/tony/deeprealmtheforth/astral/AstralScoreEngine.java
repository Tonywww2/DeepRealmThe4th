package com.tony.deeprealmtheforth.astral;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server-side score and medal calculation. Bonus rules share a snapshot; conversions run in cell order. */
public final class AstralScoreEngine {
    private AstralScoreEngine() {}

    public static Result calculate(Player player, ItemStack container, ContainerLayout layout,
            List<ItemStack> contents) {
        if (contents.size() != layout.size()) {
            throw new IllegalArgumentException("Container contents do not match layout dimensions");
        }
        List<ItemStack> snapshot = contents.stream().map(ItemStack::copy).toList();

        List<ActiveFiller> fillers = new ArrayList<>();
        Map<Integer, InactiveReason> inactive = new LinkedHashMap<>();
        Set<String> present = new HashSet<>();
        for (int i = 0; i < snapshot.size(); i++) {
            if (layout.isOpen(i) && !snapshot.get(i).isEmpty()) {
                present.add(BuiltInRegistries.ITEM.getKey(snapshot.get(i).getItem()).toString());
            }
        }
        Set<String> countedOnce = new HashSet<>();
        ScoreSheet scores = ScoreSheet.EMPTY;
        for (int i = 0; i < snapshot.size(); i++) {
            if (!layout.isOpen(i)) continue;
            ItemStack stack = snapshot.get(i);
            FillerDefinition definition = AstralFillers.find(stack);
            if (definition == null) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (definition.suppressedBy().stream().anyMatch(present::contains)) {
                inactive.put(i, InactiveReason.SUPPRESSED);
                continue;
            }
            if (definition.activation() != FillerActivation.STACKABLE && !countedOnce.add(itemId)) {
                inactive.put(i, InactiveReason.DUPLICATE);
                continue;
            }
            fillers.add(new ActiveFiller(definition,
                    new FillerContext(player, container, stack, layout, snapshot, i), i));
            scores = scores.plus(definition.baseScores());
        }

        List<String> errors = new ArrayList<>();
        scores = applyBonusPhase(scores, fillers, errors);
        scores = applyConversionPhase(scores, fillers, errors);

        Map<AstralAttributeKey, Double> attributes = new LinkedHashMap<>();
        for (ActiveFiller filler : fillers) {
            for (FillerDefinition.MedalRule rule : filler.definition().medalRules()) {
                Map<AstralAttributeKey, Double> candidate = new LinkedHashMap<>();
                try {
                    rule.apply(filler.context(), scores, new FillerDefinition.AttributeWriter() {
                        @Override
                        public void add(String attributeId, double amount) {
                            collect(attributeId, AstralAttributeOperation.ADD, amount);
                        }

                        @Override
                        public void multiplyBase(String attributeId, double fraction) {
                            collect(attributeId, AstralAttributeOperation.MULTIPLY_BASE, fraction);
                        }

                        @Override
                        public void multiplyTotal(String attributeId, double fraction) {
                            collect(attributeId, AstralAttributeOperation.MULTIPLY_TOTAL, fraction);
                        }

                        private void collect(String id, AstralAttributeOperation operation, double amount) {
                            if (!Double.isFinite(amount)) {
                                throw new IllegalArgumentException("Attribute amount must be finite");
                            }
                            mergeFinite(candidate, new AstralAttributeKey(id, operation), amount);
                        }
                    });
                    Map<AstralAttributeKey, Double> next = new LinkedHashMap<>(attributes);
                    candidate.forEach((id, amount) -> mergeFinite(next, id, amount));
                    attributes.clear();
                    attributes.putAll(next);
                } catch (RuntimeException exception) {
                    errors.add("Cell " + filler.index() + " medal: " + exception.getMessage());
                }
            }
        }
        return new Result(scores, Map.copyOf(attributes), List.copyOf(errors), Map.copyOf(inactive));
    }

    private static ScoreSheet applyBonusPhase(ScoreSheet input, List<ActiveFiller> fillers,
            List<String> errors) {
        EnumMap<ScoreType, Double> total = new EnumMap<>(ScoreType.class);
        for (ActiveFiller filler : fillers) {
            for (FillerDefinition.ScoreRule rule : filler.definition().bonusRules()) {
                EnumMap<ScoreType, Double> candidate = new EnumMap<>(ScoreType.class);
                try {
                    rule.apply(filler.context(), input, (type, amount) -> {
                        if (type == null || !Double.isFinite(amount)) {
                            throw new IllegalArgumentException("Score rule returned an invalid value");
                        }
                        mergeFinite(candidate, type, amount);
                    });
                    EnumMap<ScoreType, Double> next = new EnumMap<>(total);
                    candidate.forEach((type, amount) -> mergeFinite(next, type, amount));
                    input.plus(ScoreSheet.of(next));
                    total.clear();
                    total.putAll(next);
                } catch (RuntimeException exception) {
                    errors.add("Cell " + filler.index() + " bonus: " + exception.getMessage());
                }
            }
        }
        return input.plus(ScoreSheet.of(total));
    }

    private static ScoreSheet applyConversionPhase(ScoreSheet input, List<ActiveFiller> fillers,
            List<String> errors) {
        ScoreSheet current = input;
        for (ActiveFiller filler : fillers) {
            for (FillerDefinition.ScoreRule rule : filler.definition().conversionRules()) {
                EnumMap<ScoreType, Double> candidate = new EnumMap<>(ScoreType.class);
                try {
                    ScoreSheet before = current;
                    rule.apply(filler.context(), before, (type, amount) -> {
                        if (type == null || !Double.isFinite(amount)) {
                            throw new IllegalArgumentException("Conversion returned an invalid value");
                        }
                        mergeFinite(candidate, type, amount);
                    });
                    double fraction = 1;
                    for (Map.Entry<ScoreType, Double> change : candidate.entrySet()) {
                        if (change.getValue() < 0) {
                            double available = Math.max(0, before.get(change.getKey()));
                            fraction = Math.min(fraction, available / -change.getValue());
                        }
                    }
                    EnumMap<ScoreType, Double> limited = new EnumMap<>(ScoreType.class);
                    for (Map.Entry<ScoreType, Double> change : candidate.entrySet()) {
                        limited.put(change.getKey(), change.getValue() * fraction);
                    }
                    current = before.plus(ScoreSheet.of(limited));
                } catch (RuntimeException exception) {
                    errors.add("Cell " + filler.index() + " conversion: " + exception.getMessage());
                }
            }
        }
        return current;
    }

    private record ActiveFiller(FillerDefinition definition, FillerContext context, int index) {}

    public enum InactiveReason { SUPPRESSED, DUPLICATE }

    public record Result(ScoreSheet scores, Map<AstralAttributeKey, Double> attributeBonuses,
            List<String> errors, Map<Integer, InactiveReason> inactiveReasons) {}

    private static <K> void mergeFinite(Map<K, Double> map, K key, double amount) {
        double sum = map.getOrDefault(key, 0.0) + amount;
        if (!Double.isFinite(sum)) throw new IllegalArgumentException("Rule total is not finite");
        map.put(key, sum);
    }
}
