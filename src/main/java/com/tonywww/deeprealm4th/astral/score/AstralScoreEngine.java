package com.tonywww.deeprealm4th.astral.score;

import com.tonywww.deeprealm4th.astral.AstralFillers;
import com.tonywww.deeprealm4th.astral.AstralPlayerEffectGroups;
import com.tonywww.deeprealm4th.astral.AstralRuntime;
import com.tonywww.deeprealm4th.astral.EffectScope;
import com.tonywww.deeprealm4th.astral.FillerActivation;
import com.tonywww.deeprealm4th.astral.FillerContext;
import com.tonywww.deeprealm4th.astral.FillerDefinition;
import com.tonywww.deeprealm4th.astral.ScoreType;
import com.tonywww.deeprealm4th.astral.container.ContainerLayout;
import com.tonywww.deeprealm4th.astral.node.AstralNodeEngine;
import com.tonywww.deeprealm4th.astral.node.FillerNode;
import com.tonywww.deeprealm4th.astral.node.FillerResult;
import com.tonywww.deeprealm4th.astral.node.ReadPlan;
import com.tonywww.deeprealm4th.astral.node.PlayerStateSnapshot;
import com.tonywww.deeprealm4th.astral.node.StaticFillerNodes;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** One per-cell node pass followed by medal conversion. */
public final class AstralScoreEngine {
    private AstralScoreEngine() {}

    public static Result calculate(Player player, ItemStack container, ContainerLayout layout,
            List<ItemStack> contents) {
        return calculateEquipped(player, container, layout, contents, null, -1);
    }

    public static Result calculateEquipped(Player player, ItemStack container, ContainerLayout layout,
            List<ItemStack> contents, String slotName, int slotIndex) {
        if (contents.size() != layout.size())
            throw new IllegalArgumentException("Container contents do not match layout dimensions");
        AstralRuntime.beginCalculation();
        try {
            List<ItemStack> snapshot = contents.stream().map(ItemStack::copy).toList();
            PlayerStateSnapshot state = PlayerStateSnapshot.capture(player);
            List<AstralNodeEngine.Candidate> nodes = new ArrayList<>();
            Map<Integer, InactiveReason> inactive = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            Set<String> present = new HashSet<>();
            for (int i = 0; i < snapshot.size(); i++) {
                if (layout.isOpen(i) && !snapshot.get(i).isEmpty())
                    present.add(BuiltInRegistries.ITEM.getKey(snapshot.get(i).getItem()).toString());
            }
            Set<String> countedOnce = new HashSet<>();
            Set<String> countedGroups = new HashSet<>();
            for (int i = 0; i < snapshot.size(); i++) {
                if (!layout.isOpen(i)) continue;
                ItemStack stack = snapshot.get(i);
                FillerDefinition definition = AstralFillers.find(stack);
                if (definition == null) {
                    if (!stack.isEmpty() && AstralFillers.isDisabled(
                            BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()))
                        inactive.put(i, InactiveReason.DISABLED);
                    continue;
                }
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (definition.suppressedBy().stream().anyMatch(present::contains)) {
                    inactive.put(i, InactiveReason.SUPPRESSED);
                    continue;
                }
                if (definition.activation() != FillerActivation.STACKABLE && countedOnce.contains(itemId)) {
                    inactive.put(i, InactiveReason.DUPLICATE);
                    continue;
                }
                if (!definition.effectGroup().isEmpty() && (countedGroups.contains(definition.effectGroup())
                        || definition.effectScope() == EffectScope.PLAYER
                        && AstralPlayerEffectGroups.precedes(player, container, definition.effectGroup(),
                                slotName, slotIndex))) {
                    inactive.put(i, InactiveReason.EFFECT_GROUP);
                    continue;
                }
                try {
                    FillerNode node = definition.nodeResolver() == null
                            ? StaticFillerNodes.resolve(itemId, definition)
                            : definition.nodeResolver().resolve(new FillerContext(player,
                                    container, stack, layout, snapshot, i, state));
                    if (node == null || !node.valid()) {
                        inactive.put(i, InactiveReason.INVALID);
                        errors.add("Cell " + i + " node: " + (node == null ? "missing_node" : node.invalidReason()));
                        continue;
                    }
                    nodes.add(new AstralNodeEngine.Candidate(i, definition, node));
                } catch (RuntimeException exception) {
                    inactive.put(i, InactiveReason.INVALID);
                    errors.add("Cell " + i + " node: " + exception.getMessage());
                    continue;
                }
                if (definition.activation() != FillerActivation.STACKABLE) countedOnce.add(itemId);
                if (!definition.effectGroup().isEmpty()) countedGroups.add(definition.effectGroup());
            }
            Map<Integer, String> inactiveText = new LinkedHashMap<>();
            inactive.forEach((index, reason) -> inactiveText.put(index, reason.name().toLowerCase()));
            AstralNodeEngine.Result round = AstralNodeEngine.calculate(player, container, layout,
                    snapshot, nodes, Set.of(), inactiveText, state);
            errors.addAll(round.diagnostics());
            for (var entry : round.cells().entrySet()) {
                if ("intrinsic_error".equals(entry.getValue().state()))
                    inactive.put(entry.getKey(), InactiveReason.INVALID);
            }
            BigScoreSheet bigScores = round.scores();
            ScoreSheet display = finiteDisplay(bigScores);
            Set<Integer> allEffective = new HashSet<>();
            for (var entry : round.cells().entrySet())
                if (entry.getValue().computed()) allEffective.add(entry.getKey());
            Map<AstralAttributeKey, Double> attributes = new LinkedHashMap<>();
            for (AstralNodeEngine.Candidate node : nodes) {
                FillerResult result = round.cells().get(node.index());
                if (result == null || !result.computed()) continue;
                FillerContext context = new FillerContext(player, container, snapshot.get(node.index()), layout,
                        snapshot, node.index(), allEffective, round.views(), round.state(),
                        round.cells(), round.cells(), allEffective, new ReadPlan.Inputs(Map.of(), Map.of()));
                for (FillerDefinition.MedalRule rule : node.definition().medalRules())
                    applyAttributeRule(node.index(), writer -> rule.apply(context, display, writer),
                            attributes, errors);
                for (FillerDefinition.BigMedalRule rule : node.definition().bigMedalRules())
                    applyAttributeRule(node.index(), writer -> rule.apply(context, bigScores, writer),
                            attributes, errors);
            }
            return new Result(display, Map.copyOf(attributes), List.copyOf(errors),
                    Map.copyOf(inactive), bigScores, round.cells());
        } finally {
            AstralRuntime.endCalculation();
        }
    }

    private static ScoreSheet finiteDisplay(BigScoreSheet scores) {
        Map<ScoreType, Double> display = new EnumMap<>(ScoreType.class);
        double limit = Double.MAX_VALUE / 16;
        for (ScoreType type : ScoreType.values()) {
            double value = scores.get(type).toFiniteDouble();
            display.put(type, Math.max(-limit, Math.min(limit, value)));
        }
        return ScoreSheet.of(display);
    }

    public enum InactiveReason { SUPPRESSED, DUPLICATE, INVALID, EFFECT_GROUP, DISABLED }

    public record Result(ScoreSheet scores, Map<AstralAttributeKey, Double> attributeBonuses,
            List<String> errors, Map<Integer, InactiveReason> inactiveReasons,
            BigScoreSheet bigScores, Map<Integer, FillerResult> nodeResults) {}

    private static void applyAttributeRule(int index, Consumer<FillerDefinition.AttributeWriter> rule,
            Map<AstralAttributeKey, Double> attributes, List<String> errors) {
        Map<AstralAttributeKey, Double> candidate = new LinkedHashMap<>();
        try {
            rule.accept(new FillerDefinition.AttributeWriter() {
                @Override public void add(String id, double amount) {
                    collect(id, AstralAttributeOperation.ADD, amount);
                }
                @Override public void multiplyBase(String id, double fraction) {
                    collect(id, AstralAttributeOperation.MULTIPLY_BASE, fraction);
                }
                @Override public void multiplyTotal(String id, double fraction) {
                    collect(id, AstralAttributeOperation.MULTIPLY_TOTAL, fraction);
                }
                private void collect(String id, AstralAttributeOperation operation, double amount) {
                    if (!Double.isFinite(amount))
                        throw new IllegalArgumentException("Attribute amount must be finite");
                    mergeFinite(candidate, new AstralAttributeKey(id, operation), amount);
                }
            });
            Map<AstralAttributeKey, Double> next = new LinkedHashMap<>(attributes);
            candidate.forEach((id, amount) -> mergeFinite(next, id, amount));
            attributes.clear();
            attributes.putAll(next);
        } catch (RuntimeException exception) {
            errors.add("Cell " + index + " medal: " + exception.getMessage());
        }
    }

    private static <K> void mergeFinite(Map<K, Double> map, K key, double amount) {
        double sum = map.getOrDefault(key, 0.0) + amount;
        if (!Double.isFinite(sum)) throw new IllegalArgumentException("Rule total is not finite");
        map.put(key, sum);
    }
}
