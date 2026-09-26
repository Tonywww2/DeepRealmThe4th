package com.tony.deeprealmtheforth.astral;

import java.util.Locale;

/** The six scores tracked by an astral body. Scores have no intrinsic attribute effect. */
public enum ScoreType {
    STRENGTH,
    AGILITY,
    INTELLIGENCE,
    CONSTITUTION,
    PERCEPTION,
    MAGIC;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ScoreType fromId(String id) {
        for (ScoreType type : values()) {
            if (type.id().equals(id)) return type;
        }
        throw new IllegalArgumentException("Unknown astral score: " + id);
    }
}
