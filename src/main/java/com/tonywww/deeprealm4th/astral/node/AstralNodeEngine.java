package com.tonywww.deeprealm4th.astral.node;

import com.tonywww.deeprealm4th.astral.FillerContext;
import com.tonywww.deeprealm4th.astral.FillerDefinition;
import com.tonywww.deeprealm4th.astral.ScoreType;
import com.tonywww.deeprealm4th.astral.container.ContainerLayout;
import com.tonywww.deeprealm4th.astral.score.AstralNumber;
import com.tonywww.deeprealm4th.astral.score.BigScoreSheet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Single-pass dependency resolver for per-stack nodes; all results belong to this round. */
public final class AstralNodeEngine {
    public record Candidate(int index, FillerDefinition definition, FillerNode node) {}
    public record Result(BigScoreSheet scores, Map<Integer, FillerResult> cells,
            List<CellView> views, List<String> diagnostics, PlayerStateSnapshot state) {}
    private AstralNodeEngine() {}

    public static Result calculate(Player player, ItemStack container, ContainerLayout layout,
            List<ItemStack> snapshot, List<Candidate> candidates, Set<Integer> otherEffective,
            Map<Integer, String> inactive, PlayerStateSnapshot state) {
        Map<Integer, Candidate> nodes = new LinkedHashMap<>();
        for (Candidate candidate : candidates) nodes.put(candidate.index(), candidate);
        Set<Integer> effective = new HashSet<>(otherEffective);
        effective.addAll(nodes.keySet());
        List<CellView> views = new ArrayList<>(snapshot.size());
        for (int i = 0; i < snapshot.size(); i++) {
            ItemStack stack = snapshot.get(i);
            FillerNode node = nodes.containsKey(i) ? nodes.get(i).node() : null;
            views.add(new CellView(i, i % layout.width(), i / layout.width(), true, layout.isOpen(i),
                    effective.contains(i), inactive.get(i), stack.isEmpty() ? ""
                            : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    node == null ? "" : node.kindKey(), node == null ? "" : node.variantKey(),
                    node == null ? "" : node.ruleKey(), node == null ? "" : node.role(),
                    stack, node == null ? null : node.data()));
        }
        List<CellView> initialViews = List.copyOf(views);
        Map<Integer, FillerResult> bases = new LinkedHashMap<>();
        Map<Integer, FillerResult> finals = new LinkedHashMap<>();
        Map<Integer, ReadPlan> plans = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (Candidate candidate : candidates) {
            FillerNode node = candidate.node();
            FillerContext context = context(player, container, layout, snapshot, candidate.index(),
                    effective, initialViews, state, bases, finals, Set.of(), new ReadPlan.Inputs(Map.of(), Map.of()));
            try {
                LocalScoreWriter writer = new LocalScoreWriter();
                node.intrinsic().apply(context, node.data(), writer);
                BigScoreSheet base = writer.snapshot();
                if (!node.signedScores()) requireNonNegative(base);
                bases.put(candidate.index(), new FillerResult(candidate.index(), node.kindKey(),
                        node.variantKey(), node.ruleKey(), node.role(), base, base, false, "base", List.of()));
            } catch (RuntimeException exception) {
                errors.add("Cell " + candidate.index() + " intrinsic: " + exception.getMessage());
                bases.put(candidate.index(), FillerResult.unavailable(candidate.index(), "intrinsic_error"));
            }
        }
        for (Candidate candidate : candidates) {
            int index = candidate.index();
            if (!"intrinsic_error".equals(bases.get(index).state())) continue;
            effective.remove(index);
            CellView old = views.get(index);
            views.set(index, new CellView(index, old.x(), old.y(), true, old.open(), false,
                    "intrinsic_error", old.actualItemId(), old.kindKey(), old.variantKey(),
                    old.ruleKey(), old.role(), old.itemCopy(), old.instanceData()));
        }
        List<CellView> cellViews = List.copyOf(views);
        for (Candidate candidate : candidates) {
            if ("intrinsic_error".equals(bases.get(candidate.index()).state())) continue;
            FillerContext context = context(player, container, layout, snapshot, candidate.index(),
                    effective, cellViews, state, bases, finals, Set.of(), new ReadPlan.Inputs(Map.of(), Map.of()));
            try {
                ReadPlan plan = candidate.node().reads().declare(context, candidate.node().data());
                plans.put(candidate.index(), plan == null ? ReadPlan.empty() : plan);
            } catch (RuntimeException exception) {
                errors.add("Cell " + candidate.index() + " reads: " + exception.getMessage());
                plans.put(candidate.index(), ReadPlan.empty());
            }
        }
        Map<Integer, Set<Integer>> dependencies = new HashMap<>();
        for (Candidate candidate : candidates) {
            Set<Integer> needs = new HashSet<>();
            for (ReadPlan.Port port : plans.getOrDefault(candidate.index(), ReadPlan.empty()).ports()) {
                if (port.mode() == ReadPlan.Mode.FINAL)
                    for (int source : port.cells()) if (nodes.containsKey(source)) needs.add(source);
            }
            dependencies.put(candidate.index(), needs);
        }
        PriorityQueue<Integer> ready = new PriorityQueue<>();
        for (int index : nodes.keySet()) if (dependencies.get(index).isEmpty()) ready.add(index);
        while (!ready.isEmpty()) {
            int index = ready.remove();
            if (finals.containsKey(index)) continue;
            compute(nodes.get(index), plans.getOrDefault(index, ReadPlan.empty()), player, container,
                    layout, snapshot, effective, cellViews, state, bases, finals, errors, false);
            for (int dependent : nodes.keySet()) {
                if (dependencies.get(dependent).remove(index) && dependencies.get(dependent).isEmpty())
                    ready.add(dependent);
            }
        }
        nodes.keySet().stream().filter(index -> !finals.containsKey(index)).sorted().forEach(index ->
                compute(nodes.get(index), plans.getOrDefault(index, ReadPlan.empty()), player, container,
                        layout, snapshot, effective, cellViews, state, bases, finals, errors, true));
        BigScoreSheet total = BigScoreSheet.EMPTY;
        for (FillerResult result : finals.values()) total = total.plus(result.finalScores());
        return new Result(total, Map.copyOf(finals), cellViews, List.copyOf(errors), state);
    }

    private static void compute(Candidate candidate, ReadPlan plan, Player player, ItemStack container,
            ContainerLayout layout, List<ItemStack> snapshot, Set<Integer> effective, List<CellView> views,
            PlayerStateSnapshot state, Map<Integer, FillerResult> bases, Map<Integer, FillerResult> finals,
            List<String> errors, boolean cycle) {
        int index = candidate.index();
        FillerResult base = bases.get(index);
        if (!base.computed() && "intrinsic_error".equals(base.state())) {
            finals.put(index, base);
            return;
        }
        Map<String, List<CellView>> portCells = new LinkedHashMap<>();
        Map<String, List<FillerResult>> portResults = new LinkedHashMap<>();
        Set<Integer> allowedFinal = new HashSet<>();
        for (ReadPlan.Port port : plan.ports()) {
            List<CellView> cells = port.cells().stream().filter(i -> i >= 0 && i < views.size())
                    .map(views::get).toList();
            portCells.put(port.name(), cells);
            if (port.mode() == ReadPlan.Mode.FINAL) allowedFinal.addAll(port.cells());
            if (port.mode() != ReadPlan.Mode.METADATA) {
                Map<Integer, FillerResult> source = port.mode() == ReadPlan.Mode.BASE ? bases : finals;
                portResults.put(port.name(), port.cells().stream().map(source::get)
                        .filter(result -> result != null && !"intrinsic_error".equals(result.state())).toList());
            }
        }
        ReadPlan.Inputs inputs = new ReadPlan.Inputs(portCells, portResults);
        Map<String, List<Integer>> sourceCells = new LinkedHashMap<>();
        for (ReadPlan.Port port : plan.ports()) sourceCells.put(port.name(), port.cells());
        FillerContext context = context(player, container, layout, snapshot, index, effective,
                views, state, bases, finals, allowedFinal, inputs);
        List<String> diagnostics = new ArrayList<>();
        if (cycle) {
            diagnostics.add("final_dependency_cycle_or_unavailable");
            errors.add("Cell " + index + ": final_dependency_cycle_or_unavailable");
        }
        try {
            LocalScoreWriter writer = new LocalScoreWriter(base.baseScores());
            candidate.node().calculate().apply(context, candidate.node().data(), inputs, writer);
            BigScoreSheet finalScores = writer.snapshot();
            if (!candidate.node().signedScores()) requireNonNegative(finalScores);
            finals.put(index, new FillerResult(index, candidate.node().kindKey(), candidate.node().variantKey(),
                    candidate.node().ruleKey(), candidate.node().role(), base.baseScores(), finalScores,
                    true, cycle ? "cycle_fallback" : "computed", diagnostics, sourceCells));
        } catch (RuntimeException exception) {
            String reason = "Cell " + index + " calculate: " + exception.getMessage();
            errors.add(reason);
            diagnostics.add(reason);
            finals.put(index, new FillerResult(index, candidate.node().kindKey(), candidate.node().variantKey(),
                    candidate.node().ruleKey(), candidate.node().role(), base.baseScores(), base.baseScores(),
                    false, "calculate_error", diagnostics, sourceCells));
        }
    }

    private static FillerContext context(Player player, ItemStack container, ContainerLayout layout,
            List<ItemStack> snapshot, int index, Set<Integer> effective, List<CellView> views,
            PlayerStateSnapshot state, Map<Integer, FillerResult> bases, Map<Integer, FillerResult> finals,
            Set<Integer> allowedFinal, ReadPlan.Inputs inputs) {
        return new FillerContext(player, container, snapshot.get(index), layout, snapshot, index,
                effective, views, state, bases, finals, allowedFinal, inputs);
    }

    private static void requireNonNegative(BigScoreSheet scores) {
        for (ScoreType type : ScoreType.values())
            if (scores.get(type).sign() < 0) throw new IllegalArgumentException("Negative node score: " + type.id());
    }
}
