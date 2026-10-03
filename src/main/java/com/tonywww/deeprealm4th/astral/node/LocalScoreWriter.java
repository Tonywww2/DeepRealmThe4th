package com.tonywww.deeprealm4th.astral.node;

import com.tonywww.deeprealm4th.astral.ScoreType;
import com.tonywww.deeprealm4th.astral.score.AstralNumber;
import com.tonywww.deeprealm4th.astral.score.BigScoreSheet;
import java.util.EnumMap;
import java.util.Objects;

/** A private working copy; source node scores are never consumed by another node. */
public final class LocalScoreWriter {
    private final EnumMap<ScoreType, AstralNumber> values = new EnumMap<>(ScoreType.class);

    public LocalScoreWriter() {}
    public LocalScoreWriter(BigScoreSheet initial) { values.putAll(initial.asMap()); }
    public AstralNumber get(ScoreType type) { return values.getOrDefault(type, AstralNumber.ZERO); }
    public void add(ScoreType type, AstralNumber amount) { set(type, get(type).add(amount)); }
    public void add(ScoreType type, double amount) { add(type, AstralNumber.of(amount)); }
    public void set(ScoreType type, AstralNumber amount) {
        values.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(amount, "amount"));
    }
    public void replace(BigScoreSheet scores) {
        values.clear();
        values.putAll(scores.asMap());
    }
    public BigScoreSheet snapshot() { return BigScoreSheet.of(values); }
}
