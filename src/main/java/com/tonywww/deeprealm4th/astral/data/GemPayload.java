package com.tonywww.deeprealm4th.astral.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Schema for gem data; the allowed source/primary catalogue belongs to the addon. */
public final class GemPayload {
    public static final int SCHEMA = 1;
    public enum Stat { SIZE, PURITY, POLISH }
    public enum Operation { FLAT, PERCENT }
    public record Affix(Stat stat, Operation operation, double value) {
        public Affix {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Affix value is not finite");
        }
    }
    public record Natural(double size, double purity, double polish) {
        public Natural {
            if (!Double.isFinite(size) || size < 1 || size > 500
                    || !Double.isFinite(purity) || purity < 1 || purity > 200
                    || !Double.isFinite(polish) || polish < 1 || polish > 100)
                throw new IllegalArgumentException("Natural gem dimensions are out of range");
        }
        public double get(Stat stat) {
            return switch (stat) { case SIZE -> size; case PURITY -> purity; case POLISH -> polish; };
        }
    }
    public record DecodeResult(GemPayload value, String error) {
        public boolean ok() { return error == null; }
        public static DecodeResult invalid(String error) { return new DecodeResult(null, error); }
    }

    private final String sourceItem, primary;
    private final Natural natural;
    private final List<Affix> affixes, reinforcements;
    private final CompoundTag extensions;

    public GemPayload(String sourceItem, String primary, Natural natural, List<Affix> affixes,
            List<Affix> reinforcements, CompoundTag extensions) {
        if (sourceItem == null || !sourceItem.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"))
            throw new IllegalArgumentException("Invalid source_item");
        this.sourceItem = sourceItem;
        this.primary = normalizePrimary(primary);
        this.natural = Objects.requireNonNull(natural, "natural");
        this.affixes = List.copyOf(Objects.requireNonNull(affixes, "affixes"));
        if (this.affixes.size() > 5)
            throw new IllegalArgumentException("A gem needs 0..5 natural affixes");
        for (Affix affix : this.affixes) validateNaturalAffix(affix);
        this.reinforcements = List.copyOf(Objects.requireNonNull(reinforcements, "reinforcements"));
        this.extensions = extensions == null ? new CompoundTag() : extensions.copy();
    }

    public static String normalizePrimary(String input) {
        if (input == null) throw new IllegalArgumentException("Missing primary");
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "α", "alpha" -> "alpha";
            case "β", "beta" -> "beta";
            case "γ", "gamma" -> "gamma";
            case "δ", "delta" -> "delta";
            default -> throw new IllegalArgumentException("Unknown primary: " + input);
        };
    }
    public String sourceItem() { return sourceItem; }
    public String primary() { return primary; }
    public String displayPrimary() {
        return switch (primary) { case "alpha" -> "α"; case "beta" -> "β"; case "gamma" -> "γ"; default -> "δ"; };
    }
    public String identity() { return sourceItem + "#" + primary; }
    public Natural natural() { return natural; }
    public List<Affix> affixes() { return affixes; }
    public List<Affix> reinforcements() { return reinforcements; }
    public CompoundTag extensions() { return extensions.copy(); }

    public double effective(Stat stat) {
        double flat = 0, percent = 0;
        for (Affix affix : affixes) {
            if (affix.stat() != stat) continue;
            if (affix.operation() == Operation.FLAT) flat += affix.value();
            else percent += affix.value();
        }
        for (Affix affix : reinforcements) {
            if (affix.stat() != stat) continue;
            if (affix.operation() == Operation.FLAT) flat += affix.value();
            else percent += affix.value();
        }
        double result = Math.max(1, (natural.get(stat) + flat) * (1 + percent));
        if (!Double.isFinite(result)) throw new ArithmeticException("Effective gem dimension overflow");
        return result;
    }

    public double efficiency(Stat main, Stat secondary) {
        Stat other = null;
        for (Stat stat : Stat.values()) if (stat != main && stat != secondary) other = stat;
        if (main == secondary || other == null) throw new IllegalArgumentException("Dimensions must differ");
        return 0.70 * normalized(main) + 0.15 * normalized(secondary) + 0.15 * normalized(other);
    }
    public int positiveNaturalAffixCount() {
        int count = 0;
        for (Affix affix : affixes) if (affix.value() > 0) count++;
        return count;
    }
    public double naturalQualityForUse(Stat main, Stat secondary) {
        Stat other = null;
        for (Stat stat : Stat.values()) if (stat != main && stat != secondary) other = stat;
        if (main == secondary || other == null) throw new IllegalArgumentException("Dimensions must differ");
        double triQuality = 0.70 * naturalNormalized(main)
                + 0.15 * naturalNormalized(secondary) + 0.15 * naturalNormalized(other);
        return 0.80 * triQuality + 0.20 * positiveNaturalAffixCount() / 5.0;
    }
    public static double overallDropQuality(double fillerQuality, double badgeQuality) {
        if (!Double.isFinite(fillerQuality) || !Double.isFinite(badgeQuality))
            throw new IllegalArgumentException("Quality must be finite");
        return Math.max(fillerQuality, badgeQuality);
    }
    private double naturalNormalized(Stat stat) {
        return natural.get(stat) / switch (stat) {
            case SIZE -> 500.0; case PURITY -> 200.0; case POLISH -> 100.0;
        };
    }
    private double normalized(Stat stat) {
        return effective(stat) / switch (stat) { case SIZE -> 500.0; case PURITY -> 200.0; case POLISH -> 100.0; };
    }

    public CompoundTag encode() {
        CompoundTag root = new CompoundTag();
        root.putInt("schema", SCHEMA);
        root.putString("source_item", sourceItem);
        root.putString("primary", primary);
        CompoundTag naturalTag = new CompoundTag();
        naturalTag.putDouble("size", natural.size());
        naturalTag.putDouble("purity", natural.purity());
        naturalTag.putDouble("polish", natural.polish());
        root.put("natural", naturalTag);
        root.put("affixes", encodeAffixes(affixes));
        root.put("reinforcements", encodeAffixes(reinforcements));
        root.put("extensions", extensions.copy());
        return root;
    }

    public static DecodeResult decode(CompoundTag data, Predicate<String> allowedIdentity) {
        if (data == null) return DecodeResult.invalid("missing_gem_data");
        if (!data.contains("schema", Tag.TAG_INT)) return DecodeResult.invalid("legacy_gem_needs_migration");
        if (data.getInt("schema") != SCHEMA) return DecodeResult.invalid("unsupported_gem_schema");
        if (!data.contains("source_item", Tag.TAG_STRING) || !data.contains("primary", Tag.TAG_STRING)
                || !data.contains("natural", Tag.TAG_COMPOUND)) return DecodeResult.invalid("missing_gem_field");
        try {
            CompoundTag nbt = data.getCompound("natural");
            if (!nbt.contains("size", Tag.TAG_ANY_NUMERIC) || !nbt.contains("purity", Tag.TAG_ANY_NUMERIC)
                    || !nbt.contains("polish", Tag.TAG_ANY_NUMERIC)) return DecodeResult.invalid("missing_gem_dimension");
            Natural natural = new Natural(nbt.getDouble("size"), nbt.getDouble("purity"), nbt.getDouble("polish"));
            if (!data.contains("affixes", Tag.TAG_LIST) || !data.contains("reinforcements", Tag.TAG_LIST))
                return DecodeResult.invalid("missing_gem_field");
            ListTag naturalAffixes = (ListTag) data.get("affixes");
            ListTag upgrades = (ListTag) data.get("reinforcements");
            if (!naturalAffixes.isEmpty() && naturalAffixes.getElementType() != Tag.TAG_COMPOUND
                    || !upgrades.isEmpty() && upgrades.getElementType() != Tag.TAG_COMPOUND)
                return DecodeResult.invalid("invalid_gem_affix_count");
            List<Affix> affixes = decodeAffixes(naturalAffixes);
            if (affixes.size() > 5)
                return DecodeResult.invalid("invalid_gem_affix_count");
            List<Affix> reinforcements = decodeAffixes(upgrades);
            GemPayload payload = new GemPayload(data.getString("source_item"), data.getString("primary"), natural,
                    affixes, reinforcements, data.getCompound("extensions"));
            if (allowedIdentity != null && !allowedIdentity.test(payload.identity()))
                return DecodeResult.invalid("unknown_gem_primary");
            return new DecodeResult(payload, null);
        } catch (RuntimeException exception) {
            return DecodeResult.invalid("invalid_gem_data: " + exception.getMessage());
        }
    }

    public static DecodeResult decode(CompoundTag data) {
        return decode(data, (Predicate<String>) null);
    }

    public static DecodeResult decode(CompoundTag data, Set<String> allowedIdentities) {
        return decode(data, allowedIdentities == null ? null : allowedIdentities::contains);
    }

    private static ListTag encodeAffixes(List<Affix> affixes) {
        ListTag list = new ListTag();
        for (Affix affix : affixes) {
            CompoundTag entry = new CompoundTag();
            entry.putString("stat", affix.stat().name().toLowerCase(Locale.ROOT));
            entry.putString("operation", affix.operation().name().toLowerCase(Locale.ROOT));
            entry.putDouble("value", affix.value());
            list.add(entry);
        }
        return list;
    }

    private static void validateNaturalAffix(Affix affix) {
        double lower, upper;
        if (affix.operation() == Operation.PERCENT) {
            lower = -0.125;
            upper = 0.25;
        } else {
            lower = switch (affix.stat()) { case SIZE -> -25; case PURITY -> -10; case POLISH -> -5; };
            upper = switch (affix.stat()) { case SIZE -> 50; case PURITY -> 20; case POLISH -> 10; };
        }
        if (affix.value() < lower || affix.value() > upper)
            throw new IllegalArgumentException("Natural affix is out of range");
    }
    private static List<Affix> decodeAffixes(ListTag list) {
        List<Affix> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.contains("value", Tag.TAG_ANY_NUMERIC)) throw new IllegalArgumentException("Missing affix value");
            result.add(new Affix(Stat.valueOf(entry.getString("stat").toUpperCase(Locale.ROOT)),
                    Operation.valueOf(entry.getString("operation").toUpperCase(Locale.ROOT)),
                    entry.getDouble("value")));
        }
        return List.copyOf(result);
    }
}
