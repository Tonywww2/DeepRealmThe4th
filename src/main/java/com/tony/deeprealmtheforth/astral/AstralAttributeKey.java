package com.tony.deeprealmtheforth.astral;

import java.util.Objects;

/** Stable identity of one attribute contribution from an equipped astral body. */
public record AstralAttributeKey(String attributeId, AstralAttributeOperation operation) {
    public AstralAttributeKey {
        Objects.requireNonNull(attributeId, "attributeId");
        Objects.requireNonNull(operation, "operation");
        if (!attributeId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
            throw new IllegalArgumentException("Invalid attribute ID: " + attributeId);
        }
    }
}
