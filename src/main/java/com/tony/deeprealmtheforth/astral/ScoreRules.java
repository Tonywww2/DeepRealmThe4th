package com.tony.deeprealmtheforth.astral;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
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

    /** Adds a bounded fraction of the base-phase source score without consuming it. */
    public static FillerDefinition.ScoreRule percentOf(ScoreType source,
            ScoreType target, double rate, double cap) {
        return whenPercent(context -> true, source, target, rate, cap);
    }

    public static FillerDefinition.ScoreRule whenPercent(Predicate<FillerContext> condition,
            ScoreType source, ScoreType target, double rate, double cap) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        finite(rate);
        finite(cap);
        if (rate < 0 || cap < 0) throw new IllegalArgumentException("Rate and cap must be nonnegative");
        return (context, scores, output) -> {
            if (condition.test(context)) {
                output.add(target, Math.min(cap, Math.max(0, scores.get(source)) * rate));
            }
        };
    }

    public static FillerDefinition.ScoreRule percentWhenAdjacent(String itemId,
            ScoreType source, ScoreType target, double rate, double cap) {
        validateItemId(itemId);
        return whenPercent(context -> hasAdjacent(context, itemId), source, target, rate, cap);
    }

    public static FillerDefinition.ScoreRule percentWhenCountAtLeast(String itemId, int minimum,
            ScoreType source, ScoreType target, double rate, double cap) {
        validateItemId(itemId);
        if (minimum < 1) throw new IllegalArgumentException("Minimum filler count must be positive");
        return whenPercent(context -> context.count(itemId) >= minimum, source, target, rate, cap);
    }

    /** Uses distinct effective orthogonal neighbors matching a tag, then takes a fraction of base score. */
    public static FillerDefinition.ScoreRule percentWhenAdjacentTagAtLeast(String tagId, int minimum,
            ScoreType source, ScoreType target, double rate) {
        if (tagId == null || !tagId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
            throw new IllegalArgumentException("Expected a namespaced item tag ID, got: " + tagId);
        }
        if (minimum < 1 || minimum > 4) {
            throw new IllegalArgumentException("Adjacent tag count must be within 1..4");
        }
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.tryParse(tagId));
        return whenPercent(context -> context.countAdjacentTag(tag) >= minimum,
                source, target, rate, Double.MAX_VALUE);
    }

    public static FillerDefinition.ScoreRule whenExperienceLevelAbove(int level,
            ScoreType target, double amount) {
        if (level < 0) throw new IllegalArgumentException("Experience level cannot be negative");
        return when(context -> context.player().experienceLevel > level, target, amount);
    }

    public static FillerDefinition.ScoreRule whenHealthAbove(double health,
            ScoreType target, double amount) {
        finite(health);
        if (health < 0) throw new IllegalArgumentException("Health threshold cannot be negative");
        return when(context -> context.player().getHealth() > health, target, amount);
    }

    public static FillerDefinition.ScoreRule whenHealthAtMost(double health,
            ScoreType target, double amount) {
        finite(health);
        if (health < 0) throw new IllegalArgumentException("Health threshold cannot be negative");
        return when(context -> context.player().getHealth() <= health, target, amount);
    }

    public static FillerDefinition.ScoreRule whenFoodAtLeast(int food,
            ScoreType target, double amount) {
        if (food < 0 || food > 20) throw new IllegalArgumentException("Food threshold must be 0..20");
        return when(context -> context.player().getFoodData().getFoodLevel() >= food, target, amount);
    }

    public static FillerDefinition.ScoreRule whenNightVision(ScoreType target, double amount) {
        return when(context -> context.player().hasEffect(MobEffects.NIGHT_VISION), target, amount);
    }

    /** Counts only open cells whose fillers survived conflict and uniqueness checks. */
    public static FillerDefinition.ScoreRule countAtLeast(String itemId, int minimum,
            ScoreType target, double amount) {
        validateItemId(itemId);
        if (minimum < 1) throw new IllegalArgumentException("Minimum filler count must be positive");
        return when(context -> context.count(itemId) >= minimum, target, amount);
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

    /** Sums the nonnegative final values of all six scores into one flat attribute bonus. */
    public static FillerDefinition.MedalRule attributePerAllScores(String attributeId, double perPoint) {
        new AstralAttributeKey(attributeId, AstralAttributeOperation.ADD);
        finite(perPoint);
        if (perPoint < 0) throw new IllegalArgumentException("Per-point bonus cannot be negative");
        return (context, scores, output) -> {
            double total = 0;
            for (ScoreType type : ScoreType.values()) total += Math.max(0, scores.get(type));
            output.add(attributeId, total * perPoint);
        };
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
     * Adds {@code amount} once if at least one active cell immediately above,
     * below, left or right contains {@code itemId}. Diagonal cells do not count.
     */
    public static FillerDefinition.ScoreRule adjacent(String itemId, ScoreType target, double amount) {
        validateItemId(itemId);
        Objects.requireNonNull(target, "target");
        finite(amount);
        return (context, phaseScores, output) -> {
            if (hasAdjacent(context, itemId)) {
                output.add(target, amount);
            }
        };
    }

    private static boolean hasAdjacent(FillerContext context, String itemId) {
        return matches(context.itemAt(context.x() - 1, context.y()), itemId)
                || matches(context.itemAt(context.x() + 1, context.y()), itemId)
                || matches(context.itemAt(context.x(), context.y() - 1), itemId)
                || matches(context.itemAt(context.x(), context.y() + 1), itemId);
    }

    private static boolean matches(ItemStack stack, String itemId) {
        return !stack.isEmpty()
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId);
    }

    private static void validateItemId(String itemId) {
        if (itemId == null || !itemId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
            throw new IllegalArgumentException("Expected a namespaced item ID, got: " + itemId);
        }
    }

    private static void finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Rule value must be finite");
    }
}
