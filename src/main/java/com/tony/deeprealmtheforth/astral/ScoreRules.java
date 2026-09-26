package com.tony.deeprealmtheforth.astral;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Reusable rules for Java addons; also exposed to KubeJS through AstralRules. */
public final class ScoreRules {
    private ScoreRules() {}

    /** A Java addon can inspect any read-only player or container state in the predicate. */
    public static FillerDefinition.ScoreRule when(Predicate<FillerContext> condition,
            ScoreType target, double amount) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(target, "target");
        finite(amount);
        return (context, scores, output) -> {
            if (condition.test(context)) output.add(target, amount);
        };
    }

    public static FillerDefinition.ScoreRule perScore(ScoreType source,
            ScoreType target, double multiplier) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        finite(multiplier);
        return (context, scores, output) -> output.add(target, scores.get(source) * multiplier);
    }

    /** Consumes a fraction of the current source score; later grid cells receive the remainder. */
    public static FillerDefinition.ScoreRule convert(ScoreType source,
            ScoreType target, double fraction, double targetMultiplier) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        if (source == target) throw new IllegalArgumentException("Conversion needs different scores");
        finite(fraction);
        if (fraction < 0 || fraction > 1) {
            throw new IllegalArgumentException("Conversion fraction must be between 0 and 1");
        }
        finite(targetMultiplier);
        if (targetMultiplier < 0) throw new IllegalArgumentException("Conversion multiplier cannot be negative");
        return (context, scores, output) -> {
            double amount = Math.max(0, scores.get(source)) * fraction;
            output.add(source, -amount);
            output.add(target, amount * targetMultiplier);
        };
    }

    public static FillerDefinition.MedalRule attributePerScore(ScoreType source,
            String attributeId, double multiplier) {
        Objects.requireNonNull(source, "source");
        if (attributeId == null || !attributeId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
            throw new IllegalArgumentException("Expected a namespaced attribute ID, got: " + attributeId);
        }
        finite(multiplier);
        return (context, scores, output) -> output.add(attributeId, scores.get(source) * multiplier);
    }

    /** A multiplier fraction per score point; 0.01 means +1% per point. */
    public static FillerDefinition.MedalRule attributeMultiplyTotalPerScore(ScoreType source,
            String attributeId, double fractionPerPoint) {
        Objects.requireNonNull(source, "source");
        new AstralAttributeKey(attributeId, AstralAttributeOperation.MULTIPLY_TOTAL);
        finite(fractionPerPoint);
        return (context, scores, output) ->
                output.multiplyTotal(attributeId, scores.get(source) * fractionPerPoint);
    }

    /**
     * Adds {@code amount} once if at least one open cell immediately above, below,
     * left or right contains {@code itemId}. Diagonal cells do not count.
     */
    public static FillerDefinition.ScoreRule adjacent(String itemId, ScoreType target, double amount) {
        if (itemId == null || !itemId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
            throw new IllegalArgumentException("Expected a namespaced item ID, got: " + itemId);
        }
        Objects.requireNonNull(target, "target");
        finite(amount);
        return (context, phaseScores, output) -> {
            if (matches(context.itemAt(context.x() - 1, context.y()), itemId)
                    || matches(context.itemAt(context.x() + 1, context.y()), itemId)
                    || matches(context.itemAt(context.x(), context.y() - 1), itemId)
                    || matches(context.itemAt(context.x(), context.y() + 1), itemId)) {
                output.add(target, amount);
            }
        };
    }

    private static boolean matches(ItemStack stack, String itemId) {
        return !stack.isEmpty()
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId);
    }

    private static void finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Rule value must be finite");
    }
}
