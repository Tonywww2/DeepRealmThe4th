package com.tonywww.deeprealm4th.astral.node;

import com.tonywww.deeprealm4th.astral.ScoreType;
import com.tonywww.deeprealm4th.astral.score.AstralNumber;
import com.tonywww.deeprealm4th.astral.score.BigScoreSheet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Compact, lossless per-cell result for item synchronization and script inspection. */
public record FillerResultSummary(int cell, String kindKey, String variantKey, String ruleKey,
        String role, BigScoreSheet baseScores, BigScoreSheet finalScores,
        boolean computed, String state, List<String> diagnostics,
        Map<String, List<Integer>> sources) {
    public FillerResultSummary {
        diagnostics = List.copyOf(diagnostics);
        Map<String, List<Integer>> copied = new LinkedHashMap<>();
        sources.forEach((name, cells) -> copied.put(name, List.copyOf(cells)));
        sources = Map.copyOf(copied);
    }
    public static FillerResultSummary from(FillerResult result) {
        return new FillerResultSummary(result.cellRef(), result.kindKey(), result.variantKey(),
                result.ruleKey(), result.role(), result.baseScores(), result.finalScores(),
                result.computed(), result.state(), result.diagnostics(), result.sources());
    }
    public AstralNumber baseTotal() { return baseScores.total(); }
    public AstralNumber finalTotal() { return finalScores.total(); }
    public AstralNumber gainTotal() { return finalTotal().subtract(baseTotal()).max(AstralNumber.ZERO); }
    public AstralNumber gainRatio() {
        return finalTotal().sign() > 0 ? gainTotal().divide(finalTotal()) : AstralNumber.ZERO;
    }
    public CompoundTag encode() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Cell", cell);
        tag.putString("Kind", kindKey);
        tag.putString("Variant", variantKey);
        tag.putString("Rule", ruleKey);
        tag.putString("Role", role);
        tag.put("Base", encodeScores(baseScores));
        tag.put("Final", encodeScores(finalScores));
        tag.putBoolean("Computed", computed);
        tag.putString("State", state);
        ListTag details = new ListTag();
        diagnostics.forEach(value -> details.add(StringTag.valueOf(value)));
        tag.put("Diagnostics", details);
        ListTag ports = new ListTag();
        sources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag port = new CompoundTag();
            port.putString("Name", entry.getKey());
            port.putIntArray("Cells", entry.getValue());
            ports.add(port);
        });
        tag.put("Sources", ports);
        return tag;
    }
    public static FillerResultSummary decode(CompoundTag tag) {
        List<String> details = new ArrayList<>();
        ListTag list = tag.getList("Diagnostics", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) details.add(list.getString(i));
        Map<String, List<Integer>> sources = new LinkedHashMap<>();
        ListTag ports = tag.getList("Sources", Tag.TAG_COMPOUND);
        for (int i = 0; i < ports.size(); i++) {
            CompoundTag port = ports.getCompound(i);
            List<Integer> indices = new ArrayList<>();
            for (int index : port.getIntArray("Cells")) indices.add(index);
            sources.put(port.getString("Name"), indices);
        }
        return new FillerResultSummary(tag.getInt("Cell"), tag.getString("Kind"),
                tag.getString("Variant"), tag.getString("Rule"), tag.getString("Role"),
                decodeScores(tag.getCompound("Base")), decodeScores(tag.getCompound("Final")),
                tag.getBoolean("Computed"), tag.getString("State"), details, sources);
    }
    private static CompoundTag encodeScores(BigScoreSheet scores) {
        CompoundTag tag = new CompoundTag();
        for (ScoreType type : ScoreType.values()) tag.putString(type.id(), scores.get(type).serialize());
        return tag;
    }
    private static BigScoreSheet decodeScores(CompoundTag tag) {
        java.util.Map<ScoreType, AstralNumber> values = new java.util.EnumMap<>(ScoreType.class);
        for (ScoreType type : ScoreType.values())
            values.put(type, tag.contains(type.id(), Tag.TAG_STRING)
                    ? AstralNumber.deserialize(tag.getString(type.id())) : AstralNumber.ZERO);
        return BigScoreSheet.of(values);
    }
}
