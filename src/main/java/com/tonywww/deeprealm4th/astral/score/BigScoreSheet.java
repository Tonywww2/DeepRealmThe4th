package com.tonywww.deeprealm4th.astral.score;

import com.tonywww.deeprealm4th.astral.ScoreType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable six-score table for per-cell node calculations. */
public final class BigScoreSheet {
    public static final BigScoreSheet EMPTY = new BigScoreSheet(new EnumMap<>(ScoreType.class));
    private final Map<ScoreType, AstralNumber> values;

    private BigScoreSheet(Map<ScoreType, AstralNumber> values) { this.values = Map.copyOf(values); }

    public static BigScoreSheet of(Map<ScoreType, AstralNumber> values) {
        EnumMap<ScoreType, AstralNumber> copy = new EnumMap<>(ScoreType.class);
        values.forEach((type, amount) -> copy.put(Objects.requireNonNull(type), Objects.requireNonNull(amount)));
        return new BigScoreSheet(copy);
    }

    public static BigScoreSheet fromLegacy(ScoreSheet old) {
        EnumMap<ScoreType, AstralNumber> values = new EnumMap<>(ScoreType.class);
        for (ScoreType type : ScoreType.values()) values.put(type, AstralNumber.of(old.get(type)));
        return of(values);
    }

    public AstralNumber get(ScoreType type) { return values.getOrDefault(type, AstralNumber.ZERO); }
    public AstralNumber total() {
        AstralNumber result = AstralNumber.ZERO;
        for (ScoreType type : ScoreType.values()) result = result.add(get(type));
        return result;
    }
    public BigScoreSheet plus(ScoreType type, AstralNumber amount) {
        EnumMap<ScoreType, AstralNumber> next = new EnumMap<>(ScoreType.class);
        next.putAll(values);
        next.put(type, get(type).add(amount));
        return of(next);
    }
    public BigScoreSheet plus(BigScoreSheet other) {
        BigScoreSheet next = this;
        for (ScoreType type : ScoreType.values()) next = next.plus(type, other.get(type));
        return next;
    }
    public Map<ScoreType, AstralNumber> asMap() { return values; }
    @Override public boolean equals(Object other) { return other instanceof BigScoreSheet sheet && values.equals(sheet.values); }
    @Override public int hashCode() { return values.hashCode(); }
}
