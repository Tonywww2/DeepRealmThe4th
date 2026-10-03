package com.tonywww.deeprealm4th.astral.score;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

/** Immutable decimal score that remains finite far beyond the double range. */
public final class AstralNumber implements Comparable<AstralNumber> {
    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final double LN_10 = Math.log(10);
    public static final AstralNumber ZERO = new AstralNumber(BigDecimal.ZERO);
    public static final AstralNumber ONE = new AstralNumber(BigDecimal.ONE);
    private final BigDecimal value;

    private AstralNumber(BigDecimal value) { this.value = value.round(PRECISION); }

    public static AstralNumber of(Number number) {
        Objects.requireNonNull(number, "number");
        if (number instanceof BigDecimal decimal) return new AstralNumber(decimal);
        double value = number.doubleValue();
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Astral number must be finite");
        return new AstralNumber(BigDecimal.valueOf(value));
    }

    public static AstralNumber of(String decimalOrScientific) {
        return new AstralNumber(new BigDecimal(decimalOrScientific, PRECISION));
    }

    /** Unambiguous entry points for Rhino/JavaScript overload resolution. */
    public static AstralNumber parse(String decimalOrScientific) { return of(decimalOrScientific); }
    public static AstralNumber fromDouble(double value) { return of(value); }

    public static AstralNumber deserialize(String value) { return of(value); }
    public String serialize() { return value.toString(); }
    public AstralNumber add(AstralNumber other) { return new AstralNumber(value.add(other.value, PRECISION)); }
    public AstralNumber subtract(AstralNumber other) { return new AstralNumber(value.subtract(other.value, PRECISION)); }
    public AstralNumber multiply(AstralNumber other) { return new AstralNumber(value.multiply(other.value, PRECISION)); }
    public AstralNumber divide(AstralNumber other) {
        if (other.isZero()) throw new ArithmeticException("Astral number division by zero");
        return new AstralNumber(value.divide(other.value, PRECISION));
    }
    public AstralNumber min(AstralNumber other) { return compareTo(other) <= 0 ? this : other; }
    public AstralNumber max(AstralNumber other) { return compareTo(other) >= 0 ? this : other; }
    public AstralNumber abs() { return new AstralNumber(value.abs()); }
    public int sign() { return value.signum(); }
    public boolean isZero() { return sign() == 0; }

    public AstralNumber pow(double exponent) {
        if (!Double.isFinite(exponent)) throw new IllegalArgumentException("Power must be finite");
        if (isZero()) {
            if (exponent <= 0) throw new ArithmeticException("Zero cannot have a nonpositive power");
            return ZERO;
        }
        boolean integral = exponent == Math.rint(exponent) && Math.abs(exponent) <= 10_000;
        if (sign() < 0 && !integral) throw new ArithmeticException("Fractional power of a negative score");
        if (integral) {
            int whole = (int) exponent;
            BigDecimal raised = value.pow(Math.abs(whole), PRECISION);
            return new AstralNumber(whole < 0 ? BigDecimal.ONE.divide(raised, PRECISION) : raised);
        }
        double decimalPower = ln() * exponent / LN_10;
        if (!Double.isFinite(decimalPower) || decimalPower < -1_000_000 || decimalPower > 1_000_000)
            throw new ArithmeticException("Astral power exceeds supported decimal scale");
        int scale = (int) Math.floor(decimalPower);
        double mantissa = Math.pow(10, decimalPower - scale);
        return new AstralNumber(BigDecimal.valueOf(mantissa).scaleByPowerOfTen(scale));
    }

    /** A finite logarithm even when the underlying score exceeds Double.MAX_VALUE. */
    public double ln() {
        if (sign() <= 0) throw new ArithmeticException("Logarithm requires a positive score");
        BigDecimal absolute = value.abs().stripTrailingZeros();
        int exponent = absolute.precision() - absolute.scale() - 1;
        BigDecimal mantissa = absolute.movePointLeft(exponent);
        return Math.log(mantissa.doubleValue()) + exponent * LN_10;
    }

    public double log1p() {
        if (compareTo(ONE.multiply(of(-1))) <= 0)
            throw new ArithmeticException("log1p requires a score greater than -1");
        if (value.abs().compareTo(BigDecimal.valueOf(0.1)) < 0)
            return Math.log1p(value.doubleValue());
        return add(ONE).ln();
    }

    public double toFiniteDouble() {
        double converted = value.doubleValue();
        return Double.isInfinite(converted) ? Math.copySign(Double.MAX_VALUE, converted) : converted;
    }

    public String toScientificString() { return value.stripTrailingZeros().toString(); }
    public String toFullString() { return value.toPlainString(); }

    public String format(Format style) {
        return switch (style) {
            case FULL -> toFullString();
            case SCIENTIFIC -> toScientificString();
            case GROUPED -> {
                DecimalFormat format = new DecimalFormat("#,##0.################", DecimalFormatSymbols.getInstance(Locale.ROOT));
                format.setRoundingMode(RoundingMode.HALF_EVEN);
                yield format.format(value);
            }
            case COMPACT -> {
                int exponent = value.signum() == 0 ? 0 : value.abs().precision() - value.abs().scale() - 1;
                if (exponent < 3) yield toFullString();
                if (exponent >= 15) yield toScientificString();
                int thousands = exponent / 3;
                yield value.movePointLeft(thousands * 3).setScale(2, RoundingMode.HALF_EVEN)
                        .toPlainString() + new String[]{"", "K", "M", "B", "T"}[thousands];
            }
        };
    }

    @Override public int compareTo(AstralNumber other) { return value.compareTo(other.value); }
    @Override public boolean equals(Object other) {
        return other instanceof AstralNumber number && compareTo(number) == 0;
    }
    @Override public int hashCode() { return value.stripTrailingZeros().hashCode(); }
    @Override public String toString() { return toScientificString(); }

    public enum Format { FULL, GROUPED, SCIENTIFIC, COMPACT }
}
