package com.tony.deeprealmtheforth.worldgen;

import com.tony.deeprealmtheforth.worldgen.hydrology.ClimateSnapshot;
import com.tony.deeprealmtheforth.worldgen.hydrology.DrainageGraph;
import com.tony.deeprealmtheforth.worldgen.hydrology.DrainageGraph.*;
import com.tony.deeprealmtheforth.worldgen.layout.SpiralParameters;
import com.tony.deeprealmtheforth.worldgen.terrain.BaseTerrain;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Executable prototype specification. Requires an actual post-startup registry export. */
public final class HydrologyPrototypeVerification {
    private static int checks;
    private static void require(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Path input = Path.of(args[0]), out = Path.of(args[1]); Files.createDirectories(out);
        ClimateSnapshot climate = ClimateSnapshot.read(input);
        Files.copy(input, out.resolve("climate.tsv"), StandardCopyOption.REPLACE_EXISTING);
        var jungle = climate.climates().get("minecraft:jungle");
        var desert = climate.climates().get("minecraft:desert");
        require(jungle.runoff() > .4 && desert.runoff() == 0, "Actual hot-wet vs hot-dry registry climate");
        require(new ClimateSnapshot.Climate(-.5, .8, true, "none").runoff() > .5, "Cold does not mean zero long-term runoff");
        require(new ClimateSnapshot.Climate(2, .9, false, "none").runoff() == 0, "Precipitation switch is authoritative");
        StringBuilder report = new StringBuilder("HYDROLOGY_PROTOTYPE_LOCAL_TESTS_OK\nversion=" + DrainageGraph.VERSION
                + "\nproductionIntegration=false\nparentDomainStitching=NOT_SOLVED\nwaterIsAnalysisPreview=true\n");
        List<DrainageGraph> examples = new ArrayList<>();
        List<BaseTerrain> bases = new ArrayList<>();
        for (long seed : new long[]{42, 0, -739221}) {
            BaseTerrain base = new BaseTerrain(seed, SpiralParameters.DEFAULT);
            for (int scene = 0; scene < 3; scene++) {
                Domain domain = domain(base, scene);
                long begin = System.nanoTime();
                DrainageGraph graph = DrainageGraph.build(seed, base, climate, domain);
                double ms = (System.nanoTime() - begin) / 1e6;
                verifyGraph(graph, base);
                var dry = DrainageGraph.build(seed, base, climate.scaleRain(.5), domain);
                for (int i = 0; i < graph.nodes().size(); i++) {
                    require(dry.nodes().get(i).downstream() == graph.nodes().get(i).downstream(), "Climate cannot rewire topology");
                    require(dry.nodes().get(i).discharge() <= graph.nodes().get(i).discharge() + 1e-7, "Less rain cannot create more flow");
                }
                if (scene == 2) require(graph.nodes().stream().noneMatch(Node::wet), "Isolated arid arm has no invented perennial sources");
                else require(graph.nodes().stream().mapToInt(Node::order).max().orElse(0) >= 3, "Wet terrain has trunk + multiple tributary levels");
                report.append(stats(seed, scene, graph, ms));
                if (seed == 42) { examples.add(graph); bases.add(base); }
            }
        }
        // Independent reconstruction, concurrent/reverse child requests, and eviction/rebuild.
        var reference = examples.get(0); BaseTerrain base = bases.get(0); Domain d = reference.domain();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            var left = workers.submit(() -> DrainageGraph.build(42, new BaseTerrain(42, SpiralParameters.DEFAULT), climate, d));
            var right = workers.submit(() -> DrainageGraph.build(42, new BaseTerrain(42, SpiralParameters.DEFAULT), climate, d));
            var rightFirst = right.get(); var leftSecond = left.get();
            double boundary = d.x() + d.span() / 2.0;
            require(!reference.verticalContracts(boundary).isEmpty(), "Non-vacuous cross-child river contracts");
            require(rightFirst.verticalContracts(boundary).equals(leftSecond.verticalContracts(boundary)), "Independent children share exact parent contracts");
            require(reference.fingerprint() == rightFirst.fingerprint(), "Concurrent reconstruction deterministic");
            require(reference.fingerprint() == DrainageGraph.build(42, base, climate, d).fingerprint(), "No cache state required after eviction");
            report.append("childContracts=").append(reference.verticalContracts(boundary).size()).append("; reverse/parallel/rebuild=PASS\n");
        } finally { workers.shutdownNow(); }
        // Deliberately test TWO different parents and report discrepancies instead of hiding them.
        Domain adjacent = new Domain(d.x() + d.span(), d.z(), d.span(), d.spacing());
        var neighbor = DrainageGraph.build(42, base, climate, adjacent);
        int mismatches = 0, side = d.side();
        for (int z = 0; z < side; z++) {
            Node a = reference.nodes().get(z * side + side - 1), b = neighbor.nodes().get(z * side);
            if (a.active() && b.active() && (Math.abs(a.discharge() - b.discharge()) > 1e-7 || a.water() != b.water())) mismatches++;
        }
        require(mismatches > 0, "Known independent-parent seam must remain a visible integration blocker");
        report.append("independentAdjacentParentBoundaryMismatches=").append(mismatches)
                .append("\nINTEGRATION_GATE=FAIL: finite parents do not yet share upstream catchments\n");
        HydrologyPrototypeRenderer.render(out, climate, examples, bases, neighbor);
        report.append("assertions=").append(checks).append('\n');
        Files.writeString(out.resolve("verification.txt"), report.toString());
        System.out.println(report);
    }
    static Domain domain(BaseTerrain base, int scene) {
        double[] center = base.layout().armCenter(scene == 2 ? 1 : 5, scene == 1 ? 9000 : 5000);
        return new Domain(Math.floorDiv((int)center[0] - 1024, 32) * 32,
                Math.floorDiv((int)center[1] - 1024, 32) * 32, 2048, 32);
    }
    static void verifyGraph(DrainageGraph g, BaseTerrain base) {
        int size = g.nodes().size(); double[] expected = new double[size], area = new double[size];
        int[] rank = new int[size]; Arrays.fill(rank, -1); int r = 0;
        for (int i : g.topological()) rank[i] = r++;
        double supply = 0, losses = 0, terminals = 0;
        for (int i = 0; i < size; i++) {
            Node n = g.nodes().get(i);
            if (!n.active()) continue;
            require(rank[i] >= 0, "Every active cell is routed");
            expected[i] += n.localRunoff(); area[i] += g.domain().spacing() * (double)g.domain().spacing();
            supply += n.localRunoff(); losses += n.loss();
            if (n.downstream() < 0) {
                require(n.terminal() != Terminal.NONE, "Explicit terminal, not random disconnected river"); terminals += n.discharge();
            } else {
                Node p = g.nodes().get(n.downstream());
                require(rank[n.downstream()] < rank[i], "Strict topological DAG order");
                require(p.water() <= n.water() && p.potential() <= n.potential(), "No uphill water/potential");
                expected[n.downstream()] += n.discharge(); area[n.downstream()] += n.area();
                for (int t = 0; t <= 4; t++) {
                    double u = t / 4.0;
                    var c = base.sample((int)Math.floor(n.x() + u*(p.x()-n.x())), (int)Math.floor(n.z() + u*(p.z()-n.z())));
                    require(c.land() && c.arm() == base.sample((int)Math.floor(n.x()), (int)Math.floor(n.z())).arm(), "No river crossing a void gap");
                }
            }
        }
        for (int i = 0; i < size; i++) if (g.nodes().get(i).active()) {
            Node n = g.nodes().get(i);
            require(Math.abs(expected[i] - n.discharge() - n.loss()) < 1e-6, "Local confluence water balance");
            require(Math.abs(area[i] - n.area()) < 1e-6, "Contributing area balance");
        }
        require(Math.abs(supply - losses - terminals) < 1e-5, "Whole-domain water budget");
    }
    static String stats(long seed, int scene, DrainageGraph g, double ms) {
        long wet = g.nodes().stream().filter(Node::wet).count();
        long roots = g.nodes().stream().filter(n -> n.terminal() == Terminal.BASIN_CANDIDATE).count();
        long unresolved = g.nodes().stream().filter(n -> n.terminal() == Terminal.UNRESOLVED_BOUNDARY).count();
        long heads = g.nodes().stream().filter(n -> n.wet() && n.incoming() == 0).count();
        long joins = g.nodes().stream().filter(n -> n.wet() && n.incoming() >= 2).count();
        int order = g.nodes().stream().mapToInt(Node::order).max().orElse(0);
        double fill = g.nodes().stream().filter(Node::active).mapToDouble(n -> Math.max(0,n.water()-n.height())).max().orElse(0);
        return String.format(Locale.ROOT,"seed=%d scene=%d origin=%d,%d wetNodes=%d sources=%d junctions=%d maxOrder=%d basinCandidates=%d unresolvedTerminals=%d maxAnalysisPonding=%.1f buildMs=%.1f fingerprint=%016x\n",
                seed,scene,g.domain().x(),g.domain().z(),wet,heads,joins,order,roots,unresolved,fill,ms,g.fingerprint());
    }
}
