package com.tonywww.deeprealm4th.astral.process;

import java.util.List;

/** Ordered material and direction matching for projection combining. */
public final class ProjectionRules {
    private ProjectionRules() {}

    public static void validate(String resultId, List<?> steps) {
        ProcessItemIds.requireValid(resultId);
        if (steps.size() < 2 || steps.size() > 9)
            throw new IllegalArgumentException("Projection steps must be 2..9");
    }

    public static <T> boolean matchesPrefix(List<T> steps, List<T> input) {
        if (input.size() > steps.size()) return false;
        for (int i = 0; i < input.size(); i++) {
            if (!steps.get(i).equals(input.get(i))) return false;
        }
        return true;
    }
}
