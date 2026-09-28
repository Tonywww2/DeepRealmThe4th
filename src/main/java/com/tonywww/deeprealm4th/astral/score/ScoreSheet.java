package com.tonywww.deeprealm4th.astral.score;

import com.tonywww.deeprealm4th.astral.ScoreType;

import java.util.Arrays;
import java.util.Map;

/** An immutable snapshot of the six scores. It does not apply Minecraft attributes. */
public final class ScoreSheet {
    public static final ScoreSheet EMPTY = new ScoreSheet(new double[ScoreType.values().length]);

    private final double[] values;

    private ScoreSheet(double[] values) {
        this.values = values;
    }

    public static ScoreSheet of(Map<ScoreType, ? extends Number> source) {
        double[] values = new double[ScoreType.values().length];
        source.forEach((type, value) -> values[type.ordinal()] = checked(value.doubleValue()));
        return new ScoreSheet(values);
    }

    public double get(ScoreType type) {
        return values[type.ordinal()];
    }

    public ScoreSheet plus(ScoreType type, double amount) {
        double[] next = values.clone();
        next[type.ordinal()] = checked(next[type.ordinal()] + checked(amount));
        return new ScoreSheet(next);
    }

    public ScoreSheet plus(ScoreSheet other) {
        double[] next = values.clone();
        for (int i = 0; i < next.length; i++) {
            next[i] = checked(next[i] + other.values[i]);
        }
        return new ScoreSheet(next);
    }

    public boolean isEmpty() {
        for (double value : values) {
            if (value != 0) return false;
        }
        return true;
    }

    private static double checked(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Astral score must be finite");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ScoreSheet sheet && Arrays.equals(values, sheet.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }
}
