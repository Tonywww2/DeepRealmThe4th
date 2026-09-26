package com.tony.deeprealmtheforth.worldgen.hydrology;

import com.tony.deeprealmtheforth.worldgen.terrain.BaseTerrain;
import com.tony.deeprealmtheforth.worldgen.terrain.SeededNoise;
import java.util.*;

/**
 * Experimental finite-domain hydrology, deliberately NOT used by the chunk generator.
 * A planar jittered triangulation -> explicit terminal candidates -> Priority-Flood
 * -> reverse-topological area/runoff accumulation. Tile edges are NOT sea outlets.
 * External catchments are unknown: child contracts are exact, independent parent
 * domains are not yet stitched and must never be presented as an infinite solver.
 */
public final class DrainageGraph {
    public static final String VERSION = "finite-drainage-v1";
    public static final double WET_THRESHOLD = 6000;
    public enum Terminal { NONE, BASIN_CANDIDATE, UNRESOLVED_BOUNDARY }
    public record Domain(int x, int z, int span, int spacing) {
        public Domain {
            if (spacing < 16 || span < 512 || span > 8192 || span % spacing != 0
                    || x % spacing != 0 || z % spacing != 0) throw new IllegalArgumentException("Invalid finite domain");
        }
        public int side() { return span / spacing + 1; }
    }
    public record Node(long id, double x, double z, double height, boolean active,
                       ClimateSnapshot.Sample climate, int downstream, double potential,
                       double water, double area, double localRunoff, double loss, double discharge,
                       int order, int incoming, Terminal terminal) {
        public boolean wet() { return active && discharge >= WET_THRESHOLD; }
        public double halfWidth() { return width(discharge); }
    }
    public record Contract(long segment, double x, double z, double water, double discharge,
                           int direction, int order) {}
    private final Domain domain;
    private final List<Node> nodes;
    private final int[] topological;
    private final long fingerprint;
    private final boolean truncated;
    private DrainageGraph(Domain domain, List<Node> nodes, int[] topological, boolean truncated) {
        this.domain = domain; this.nodes = List.copyOf(nodes); this.topological = topological.clone(); this.truncated = truncated;
        long hash = 0xcbf29ce484222325L;
        for (Node n : nodes) for (long value : new long[]{n.id(), n.downstream(), Double.doubleToLongBits(n.potential()),
                Double.doubleToLongBits(n.water()), Double.doubleToLongBits(n.discharge()), n.order(), n.terminal().ordinal()})
            hash = (hash ^ value) * 0x100000001b3L;
        fingerprint = hash;
    }
    public Domain domain() { return domain; }
    public List<Node> nodes() { return nodes; }
    public int[] topological() { return topological.clone(); }
    public long fingerprint() { return fingerprint; }
    public boolean truncated() { return truncated; }
    public static double width(double q) { return Math.min(30, 1.6 + 1.7 * Math.pow(Math.max(0, q) / WET_THRESHOLD, .36)); }

    private record QueueEntry(int index, double potential, long tie) {}
    private static final int[][] ORTHOGONAL = {{-1,0},{1,0},{0,-1},{0,1}};
    public static DrainageGraph build(long seed, BaseTerrain base, ClimateSnapshot climate, Domain domain) {
        return build(seed, base, climate, domain, false);
    }
    public static DrainageGraph buildNatural(long seed, BaseTerrain base, ClimateSnapshot climate, Domain domain) {
        return build(seed, base, climate, domain, true);
    }
    private static DrainageGraph build(long seed, BaseTerrain base, ClimateSnapshot climate, Domain domain, boolean irregular) {
        int side = domain.side(), size = side * side, step = domain.spacing();
        double[] xs = new double[size], zs = new double[size], height = new double[size], potential = new double[size];
        long[] ids = new long[size]; boolean[] active = new boolean[size], seen = new boolean[size];
        int[] parent = new int[size], order = new int[size], incoming = new int[size], maxOrder = new int[size], equal = new int[size];
        double[] area = new double[size], q = new double[size], loss = new double[size], local = new double[size];
        ClimateSnapshot.Sample[] climates = new ClimateSnapshot.Sample[size];
        Terminal[] terminal = new Terminal[size]; Arrays.fill(terminal, Terminal.NONE); Arrays.fill(parent, -1);
        Arrays.fill(potential, Double.POSITIVE_INFINITY);
        boolean truncated = false;
        for (int j = 0; j < side; j++) for (int i = 0; i < side; i++) {
            int k = j * side + i;
            ids[k] = SeededNoise.hash(seed ^ 0x78263, domain.x() / step + i, domain.z() / step + j);
            // Jitter is zero on the analysis boundary; bounded interior jitter preserves planar cells.
            boolean boundary = i == 0 || j == 0 || i == side - 1 || j == side - 1;
            double jitter = irregular ? .90 : .36;
            xs[k] = domain.x() + i * step + (boundary ? 0 : (SeededNoise.unit(ids[k]) - .5) * jitter * step);
            zs[k] = domain.z() + j * step + (boundary ? 0 : (SeededNoise.unit(SeededNoise.hash(ids[k], 1, 7)) - .5) * jitter * step);
            int x = (int) Math.floor(xs[k]), z = (int) Math.floor(zs[k]);
            var c = base.sample(x, z);
            height[k] = c.top();
            // No water over void, infernal terrain, central cliffs, or unsupported arm edges.
            active[k] = c.land() && (c.arm() == 1 || c.arm() == 5) && c.edgeDistance() > 48
                    && Math.hypot(x - 8, z - 8) > 220;
            if (active[k]) {
                climates[k] = climate.sample(base, x, z);
                // Tributary inputs exclude inactive/void neighbors even when the climate blend overlaps them.
                area[k] = step * (double) step;
                local[k] = climates[k].runoff() * area[k];
                q[k] = local[k];
                if (boundary) truncated = true;
            }
        }
        // Endpoint tests are insufficient near a tightly curved arm boundary.
        // Validate the entire planar edge against the actual land mask before routing.
        int[][] adjacency = new int[size][];
        int[][] mesh = irregular ? DrainageMesh.triangulate(xs, zs) : null;
        for (int k = 0; k < size; k++) {
            int[] candidates = irregular ? mesh[k] : neighbors(k, side), valid = new int[candidates.length]; int used = 0;
            if (active[k]) for (int n : candidates) if (active[n]) {
                int steps = (int)Math.ceil(Math.hypot(xs[n]-xs[k], zs[n]-zs[k]));
                int arm = base.field().sample(Math.floor(xs[k])+.5, Math.floor(zs[k])+.5).arm();
                boolean supported = true;
                for (int t = 0; t <= steps; t++) {
                    double f = t / (double)steps;
                    var point = base.field().sample(Math.floor(xs[k]+f*(xs[n]-xs[k]))+.5,
                            Math.floor(zs[k]+f*(zs[n]-zs[k]))+.5);
                    if (!point.land() || point.arm() != arm || point.edgeDistance() < 8) { supported = false; break; }
                }
                if (supported) valid[used++] = n;
            }
            adjacency[k] = Arrays.copyOf(valid, used);
        }
        // Select actual low depressions, not tile centers. Candidate means morphology only:
        // lake water balance and the unknown external watershed still require later approval.
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(Comparator.comparingDouble(QueueEntry::potential)
                .thenComparingLong(QueueEntry::tie).thenComparingInt(QueueEntry::index));
        boolean[] componentSeen = new boolean[size];
        for (int start = 0; start < size; start++) if (active[start] && !componentSeen[start]) {
            List<Integer> component = new ArrayList<>(); ArrayDeque<Integer> pending = new ArrayDeque<>();
            pending.add(start); componentSeen[start] = true;
            while (!pending.isEmpty()) {
                int k = pending.removeFirst(); component.add(k);
                for (int n : adjacency[k]) if (!componentSeen[n]) { componentSeen[n] = true; pending.add(n); }
            }
            component.sort(Comparator.<Integer>comparingDouble(k -> height[k]).thenComparingLong(k -> ids[k]));
            List<Integer> roots = new ArrayList<>();
            for (int k : component) {
                int i = k % side, j = k / side;
                if (i < 4 || j < 4 || i >= side - 4 || j >= side - 4) continue;
                boolean low = true;
                for (int n : adjacency[k]) if (height[n] < height[k]
                        || height[n] == height[k] && ids[n] < ids[k]) { low = false; break; }
                if (!low) continue;
                double ringMinimum = Double.POSITIVE_INFINITY;
                for (int a = 0; a < 16; a++) {
                    double angle = a * Math.PI / 8;
                    var ring = base.sample((int)(xs[k] + 128 * Math.cos(angle)), (int)(zs[k] + 128 * Math.sin(angle)));
                    if (!ring.land()) { ringMinimum = -64; break; }
                    ringMinimum = Math.min(ringMinimum, ring.top());
                }
                if (ringMinimum < height[k] + 4) continue;
                boolean separate = true;
                for (int root : roots) if (Math.hypot(xs[k] - xs[root], zs[k] - zs[root]) < 384) { separate = false; break; }
                if (separate) roots.add(k);
            }
            if (roots.isEmpty()) {
                // A diagnostic escape only; never report this as a physical ocean/lake outlet.
                int k = component.get(0); roots.add(k); terminal[k] = Terminal.UNRESOLVED_BOUNDARY;
            }
            for (int k : roots) {
                if (terminal[k] == Terminal.NONE) terminal[k] = Terminal.BASIN_CANDIDATE;
                seen[k] = true; potential[k] = height[k]; queue.add(new QueueEntry(k, potential[k], ids[k]));
            }
        }
        int[] visited = new int[size]; int count = 0;
        while (!queue.isEmpty()) {
            QueueEntry e = queue.remove(); int k = e.index(); visited[count++] = k;
            for (int n : adjacency[k]) if (!seen[n]) {
                seen[n] = true; parent[n] = k;
                potential[n] = Math.max(height[n], potential[k]);
                queue.add(new QueueEntry(n, potential[n], ids[n]));
            }
        }
        // Prefer the steepest available descent; Priority-Flood parents resolve flats.
        int[] rank = new int[size]; Arrays.fill(rank, -1);
        for (int v = 0; v < count; v++) rank[visited[v]] = v;
        for (int v = 0; v < count; v++) {
            int k = visited[v]; if (terminal[k] != Terminal.NONE) continue;
            double steepest = 0;
            for (int n : adjacency[k]) if (rank[n] < rank[k]) {
                double slope = (potential[k]-potential[n]) / Math.hypot(xs[n]-xs[k], zs[n]-zs[k]);
                if (slope > steepest) { steepest = slope; parent[k] = n; }
            }
        }
        for (int v = count - 1; v >= 0; v--) {
            int k = visited[v], p = parent[k];
            double length = p < 0 ? 0 : Math.hypot(xs[p] - xs[k], zs[p] - zs[k]);
            loss[k] = Math.min(q[k], climates[k].evaporation() * length * width(q[k]) * .04);
            q[k] -= loss[k];
            if (q[k] >= WET_THRESHOLD) {
                order[k] = maxOrder[k] == 0 ? 1 : maxOrder[k] + (equal[k] >= 2 ? 1 : 0);
                if (p >= 0) {
                    incoming[p]++;
                    if (order[k] > maxOrder[p]) { maxOrder[p] = order[k]; equal[p] = 1; }
                    else if (order[k] == maxOrder[p]) equal[p]++;
                }
            }
            if (p >= 0) { q[p] += q[k]; area[p] += area[k]; }
        }
        List<Node> result = new ArrayList<>(size);
        for (int k = 0; k < size; k++) result.add(new Node(ids[k], xs[k], zs[k], height[k], active[k], climates[k], parent[k],
                potential[k], active[k] ? potential[k] - 1 : height[k], area[k], local[k], loss[k], q[k], order[k], incoming[k], terminal[k]));
        return new DrainageGraph(domain, result, Arrays.copyOf(visited, count), truncated);
    }

    private static int[] neighbors(int k, int side) {
        int i = k % side, j = k / side; int[] result = new int[8]; int n = 0;
        for (int[] d : ORTHOGONAL) if (i+d[0] >= 0 && i+d[0] < side && j+d[1] >= 0 && j+d[1] < side)
            result[n++] = (j+d[1])*side+i+d[0];
        // Alternating cell diagonals: planar, no crossing diagonal drainage edges.
        if (((i + j) & 1) == 0) for (int dx : new int[]{-1,1}) for (int dz : new int[]{-1,1})
            if (i+dx >= 0 && i+dx < side && j+dz >= 0 && j+dz < side) result[n++] = (j+dz)*side+i+dx;
        return Arrays.copyOf(result, n);
    }

    /** A child clip consumes the parent solution, never recomputes runoff at its own edges. */
    public List<Contract> verticalContracts(double x) {
        List<Contract> result = new ArrayList<>();
        for (Node n : nodes) if (n.wet() && n.downstream() >= 0) {
            Node p = nodes.get(n.downstream());
            if ((n.x() < x) == (p.x() < x)) continue;
            double t = (x - n.x()) / (p.x() - n.x());
            result.add(new Contract(n.id(), x, n.z() + t * (p.z() - n.z()),
                    n.water() + t * (p.water() - n.water()), n.discharge(), p.x() > n.x() ? 1 : -1, n.order()));
        }
        result.sort(Comparator.comparingLong(Contract::segment));
        return List.copyOf(result);
    }
}
