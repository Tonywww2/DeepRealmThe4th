package com.tonywww.deeprealm4th.astral.process;

/** Validates resource IDs used by explicit tag-based forging ingredients. */
public final class ProcessItemIds {
    private ProcessItemIds() {}

    public static String requireValid(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
            throw new IllegalArgumentException("Expected namespaced item id: " + id);
        }
        return id;
    }
}
