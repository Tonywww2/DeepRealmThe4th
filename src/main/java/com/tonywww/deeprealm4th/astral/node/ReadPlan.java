package com.tonywww.deeprealm4th.astral.node;

import com.tonywww.deeprealm4th.astral.score.AstralNumber;
import com.tonywww.deeprealm4th.astral.score.BigScoreSheet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Named, declared inputs; only FINAL ports create score dependency edges. */
public final class ReadPlan {
    public enum Mode { METADATA, BASE, FINAL }
    public record Port(String name, Mode mode, List<Integer> cells) {
        public Port { cells = List.copyOf(cells); }
    }

    private final List<Port> ports;
    private ReadPlan(List<Port> ports) { this.ports = List.copyOf(ports); }
    public static Builder builder() { return new Builder(); }
    public static ReadPlan empty() { return new Builder().build(); }
    public List<Port> ports() { return ports; }

    public static final class Builder {
        private final Map<String, Port> ports = new LinkedHashMap<>();
        public Builder metadata(String name, List<CellView> cells, FillerFilter filter) {
            return add(name, Mode.METADATA, cells, filter);
        }
        public Builder baseScores(String name, List<CellView> cells, FillerFilter filter) {
            return add(name, Mode.BASE, cells, filter);
        }
        public Builder finalScores(String name, List<CellView> cells, FillerFilter filter) {
            return add(name, Mode.FINAL, cells, filter);
        }
        private Builder add(String name, Mode mode, List<CellView> cells, FillerFilter filter) {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Read port needs a name");
            Objects.requireNonNull(filter, "filter");
            List<Integer> indices = cells.stream().filter(CellView::inBounds).filter(filter)
                    .map(CellView::index).distinct().toList();
            if (ports.putIfAbsent(name, new Port(name, mode, indices)) != null)
                throw new IllegalArgumentException("Duplicate read port: " + name);
            return this;
        }
        public ReadPlan build() { return new ReadPlan(new ArrayList<>(ports.values())); }
    }

    /** Every caller receives independent local writers for source score copies. */
    public static final class Inputs {
        public record ResultPair(FillerResult first, FillerResult second) {}
        private final Map<String, List<CellView>> cells;
        private final Map<String, List<FillerResult>> results;
        public Inputs(Map<String, List<CellView>> cells, Map<String, List<FillerResult>> results) {
            this.cells = Map.copyOf(cells);
            this.results = Map.copyOf(results);
        }
        public List<CellView> cells(String port) { return cells.getOrDefault(port, List.of()); }
        public List<FillerResult> results(String port) { return results.getOrDefault(port, List.of()); }
        public FillerResult highest(String port) {
            return results(port).stream().max(Comparator.comparing(FillerResult::finalTotal)).orElse(null);
        }
        public FillerResult lowest(String port) {
            return results(port).stream().min(Comparator.comparing(FillerResult::finalTotal)).orElse(null);
        }
        public List<ResultPair> distinctPairs(String firstPort, String secondPort) {
            List<ResultPair> pairs = new ArrayList<>();
            for (FillerResult first : results(firstPort)) {
                for (FillerResult second : results(secondPort)) {
                    if (first.cellRef() != second.cellRef()) pairs.add(new ResultPair(first, second));
                }
            }
            return List.copyOf(pairs);
        }
        public LocalScoreWriter localCopy(FillerResult result) {
            return new LocalScoreWriter(result.finalScores());
        }
        public AstralNumber total(String port) {
            AstralNumber total = AstralNumber.ZERO;
            for (FillerResult result : results(port)) total = total.add(result.finalTotal());
            return total;
        }
        public BigScoreSheet sumScores(String port) {
            BigScoreSheet total = BigScoreSheet.EMPTY;
            for (FillerResult result : results(port)) total = total.plus(result.finalScores());
            return total;
        }
    }
}
