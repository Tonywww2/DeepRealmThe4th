package com.tonywww.deeprealm4th.astral.node;

import com.tonywww.deeprealm4th.astral.FillerContext;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;

/** One stack's resolved logic; no node is shared across occupied cells. */
public final class FillerNode {
    @FunctionalInterface public interface Intrinsic {
        void apply(FillerContext context, CompoundTag data, LocalScoreWriter output);
    }
    @FunctionalInterface public interface Reads {
        ReadPlan declare(FillerContext context, CompoundTag data);
    }
    @FunctionalInterface public interface Calculate {
        void apply(FillerContext context, CompoundTag data, ReadPlan.Inputs inputs, LocalScoreWriter output);
    }

    private final String kindKey, variantKey, ruleKey, role, invalidReason;
    private final CompoundTag data;
    private final Intrinsic intrinsic;
    private final Reads reads;
    private final Calculate calculate;
    private final boolean signedScores;

    private FillerNode(Builder builder) {
        this.kindKey = builder.kindKey;
        this.variantKey = builder.variantKey;
        this.ruleKey = builder.ruleKey;
        this.role = builder.role;
        this.invalidReason = builder.invalidReason;
        this.data = builder.data.copy();
        this.intrinsic = builder.intrinsic;
        this.reads = builder.reads;
        this.calculate = builder.calculate;
        this.signedScores = builder.signedScores;
    }

    public static Builder builder() { return new Builder(); }
    public static FillerNode invalid(String reason) { return builder().invalidReason(reason).build(); }
    public boolean valid() { return invalidReason.isEmpty(); }
    public String invalidReason() { return invalidReason; }
    public String kindKey() { return kindKey; }
    public String variantKey() { return variantKey; }
    public String ruleKey() { return ruleKey; }
    public String role() { return role; }
    public CompoundTag data() { return data.copy(); }
    public Intrinsic intrinsic() { return intrinsic; }
    public Reads reads() { return reads; }
    public Calculate calculate() { return calculate; }
    /** For global modifier nodes that intentionally subtract a score; ordinary gem nodes stay nonnegative. */
    public boolean signedScores() { return signedScores; }

    public static final class Builder {
        private String kindKey = "", variantKey = "", ruleKey = "", role = "", invalidReason = "";
        private CompoundTag data = new CompoundTag();
        private Intrinsic intrinsic = (context, payload, output) -> {};
        private Reads reads = (context, payload) -> ReadPlan.empty();
        private Calculate calculate = (context, payload, inputs, output) -> {};
        private boolean signedScores;
        private Builder() {}
        public Builder kindKey(String value) { kindKey = Objects.requireNonNull(value); return this; }
        public Builder variantKey(String value) { variantKey = Objects.requireNonNull(value); return this; }
        public Builder ruleKey(String value) { ruleKey = Objects.requireNonNull(value); return this; }
        public Builder role(String value) { role = Objects.requireNonNull(value); return this; }
        public Builder data(CompoundTag value) { data = Objects.requireNonNull(value).copy(); return this; }
        public Builder intrinsic(Intrinsic value) { intrinsic = Objects.requireNonNull(value); return this; }
        public Builder reads(Reads value) { reads = Objects.requireNonNull(value); return this; }
        public Builder calculate(Calculate value) { calculate = Objects.requireNonNull(value); return this; }
        public Builder signedScores(boolean value) { signedScores = value; return this; }
        private Builder invalidReason(String value) { invalidReason = Objects.requireNonNull(value); return this; }
        public FillerNode build() {
            if (invalidReason.isEmpty() && (kindKey.isEmpty() || ruleKey.isEmpty()))
                throw new IllegalArgumentException("Valid node needs kindKey and ruleKey");
            return new FillerNode(this);
        }
    }
}
