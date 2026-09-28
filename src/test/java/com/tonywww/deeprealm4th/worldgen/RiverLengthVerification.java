package com.tonywww.deeprealm4th.worldgen;

import com.tonywww.deeprealm4th.worldgen.hydrology.ClimateSnapshot;
import com.tonywww.deeprealm4th.worldgen.hydrology.GlobalDrainage;
import com.tonywww.deeprealm4th.worldgen.hydrology.WatershedRivers;
import com.tonywww.deeprealm4th.worldgen.layout.SpiralParameters;
import com.tonywww.deeprealm4th.worldgen.terrain.BaseTerrain;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Fixed-seed, fixed-region diagnostic for the lengths of rivers that actually pass geometry checks. */
public final class RiverLengthVerification {
    public static void main(String[] args) throws Exception {
        var climate = ClimateSnapshot.read(Path.of(args[0]));
        var base = new BaseTerrain(42, SpiralParameters.DEFAULT);
        var rivers = new WatershedRivers(42, base, climate);
        var drainage = rivers.drainage();
        Set<Long> seen = new HashSet<>();
        List<Double> mainstems = new ArrayList<>();
        Map<String, Integer> statuses = new TreeMap<>();
        double totalLength = 0;
        int reaches = 0;
        for (int[] origin : new int[][]{{3232, -3648}, {544, -9888}, {-5312, 1568}}) {
            for (int x = origin[0] + 128; x < origin[0] + 2048; x += 384) {
                for (int z = origin[1] + 128; z < origin[1] + 2048; z += 384) {
                    long site = GlobalDrainage.key(Math.floorDiv(x, GlobalDrainage.STEP), Math.floorDiv(z, GlobalDrainage.STEP));
                    long root = drainage.root(site);
                    if (!seen.add(root)) continue;
                    var model = rivers.model(root);
                    statuses.merge(model.status(), 1, Integer::sum);
                    if (!model.accepted()) continue;
                    Map<Long, WatershedRivers.Reach> byId = new HashMap<>();
                    for (var reach : model.reaches()) {
                        byId.put(reach.id(), reach);
                        reaches++;
                        totalLength += length(reach);
                    }
                    Map<Long, Double> toMouth = new HashMap<>();
                    double longest = 0;
                    for (var reach : model.reaches()) longest = Math.max(longest, toMouth(reach, byId, toMouth));
                    mainstems.add(longest);
                    System.out.printf("basin=%d mainstem=%.1f reaches=%d%n", root, longest, model.reaches().size());
                }
            }
        }
        Collections.sort(mainstems);
        int count = mainstems.size();
        if (count == 0) throw new AssertionError("No accepted river networks");
        double sum = mainstems.stream().mapToDouble(Double::doubleValue).sum();
        System.out.printf("RIVER_LENGTH basins=%d accepted=%d reaches=%d statuses=%s "
                        + "mainstemMean=%.1f mainstemMedian=%.1f mainstemP75=%.1f totalChannel=%.1f%n",
                seen.size(), count, reaches, statuses, sum / count, mainstems.get(count / 2),
                mainstems.get(Math.min(count - 1, (int) Math.floor(count * .75))), totalLength);
    }

    private static double toMouth(WatershedRivers.Reach reach, Map<Long, WatershedRivers.Reach> byId,
                                  Map<Long, Double> memo) {
        Double saved = memo.get(reach.id());
        if (saved != null) return saved;
        var downstream = byId.get(reach.downstream());
        double value = length(reach) + (downstream == null ? 0 : toMouth(downstream, byId, memo));
        memo.put(reach.id(), value);
        return value;
    }

    private static double length(WatershedRivers.Reach reach) {
        double result = 0;
        for (int i = 1; i < reach.knots().size(); i++) {
            var a = reach.knots().get(i - 1);
            var b = reach.knots().get(i);
            result += Math.hypot(b.x() - a.x(), b.z() - a.z());
        }
        return result;
    }
}
