package com.tony.deeprealmtheforth.astral;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Rules contributed by one item occupying one open container cell. */
public final class FillerDefinition {
    private final ScoreSheet baseScores;
    private final List<ScoreRule> bonusRules;
    private final List<ScoreRule> conversionRules;
    private final List<MedalRule> medalRules;
    private final FillerActivation activation;
    private final Set<String> suppressedBy;

    private FillerDefinition(Builder builder) {
        this.baseScores = ScoreSheet.of(builder.baseScores);
        this.bonusRules = List.copyOf(builder.bonusRules);
        this.conversionRules = List.copyOf(builder.conversionRules);
        this.medalRules = List.copyOf(builder.medalRules);
        this.activation = builder.activation;
        this.suppressedBy = Set.copyOf(builder.suppressedBy);
    }

    public static Builder builder() {
        return new Builder();
    }

    public ScoreSheet baseScores() {
        return baseScores;
    }

    public List<ScoreRule> bonusRules() {
        return bonusRules;
    }

    public List<ScoreRule> conversionRules() {
        return conversionRules;
    }

    public List<MedalRule> medalRules() {
        return medalRules;
    }

    public FillerActivation activation() {
        return activation;
    }

    /** If any of these IDs is in an open cell, this filler is stored but inactive. */
    public Set<String> suppressedBy() {
        return suppressedBy;
    }

    @FunctionalInterface
    public interface ScoreRule {
        void apply(FillerContext context, ScoreSheet phaseScores, ScoreWriter output);
    }

    @FunctionalInterface
    public interface MedalRule {
        void apply(FillerContext context, ScoreSheet finalScores, AttributeWriter output);
    }

    @FunctionalInterface
    public interface ScoreWriter {
        void add(ScoreType type, double amount);
    }

    @FunctionalInterface
    public interface AttributeWriter {
        /** Flat attribute addition, retained for existing Java and KubeJS addons. */
        void add(String attributeId, double amount);

        /** Fraction of base attribute value; 0.01 means +1%. */
        default void multiplyBase(String attributeId, double fraction) {
            throw new UnsupportedOperationException("This attribute writer cannot multiply base values");
        }

        /** Fraction of final attribute value; 0.01 means +1%. */
        default void multiplyTotal(String attributeId, double fraction) {
            throw new UnsupportedOperationException("This attribute writer cannot multiply total values");
        }
    }

    public static final class Builder {
        private final Map<ScoreType, Double> baseScores = new java.util.EnumMap<>(ScoreType.class);
        private final List<ScoreRule> bonusRules = new ArrayList<>();
        private final List<ScoreRule> conversionRules = new ArrayList<>();
        private final List<MedalRule> medalRules = new ArrayList<>();
        private FillerActivation activation = FillerActivation.STACKABLE;
        private final Set<String> suppressedBy = new java.util.LinkedHashSet<>();

        private Builder() {}

        public Builder base(ScoreType type, double amount) {
            Objects.requireNonNull(type, "type");
            requireFinite(amount);
            baseScores.merge(type, amount, Double::sum);
            return this;
        }

        public Builder bonus(ScoreRule rule) {
            bonusRules.add(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        public Builder conversion(ScoreRule rule) {
            conversionRules.add(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        public Builder medal(MedalRule rule) {
            medalRules.add(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        public Builder activation(FillerActivation mode) {
            activation = Objects.requireNonNull(mode, "mode");
            return this;
        }

        /** A directed conflict: this filler becomes inactive while itemId is present. */
        public Builder suppressedBy(String itemId) {
            if (itemId == null || !itemId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
                throw new IllegalArgumentException("Expected a namespaced item ID, got: " + itemId);
            }
            suppressedBy.add(itemId);
            return this;
        }

        public FillerDefinition build() {
            return new FillerDefinition(this);
        }

        private static void requireFinite(double amount) {
            if (!Double.isFinite(amount)) throw new IllegalArgumentException("Score amount must be finite");
        }
    }
}
