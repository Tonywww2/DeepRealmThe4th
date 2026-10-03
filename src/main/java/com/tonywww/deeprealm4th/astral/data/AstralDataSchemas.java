package com.tonywww.deeprealm4th.astral.data;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;

/** Named STARTUP validators for item data and processing recipes. */
public final class AstralDataSchemas {
    private static final Map<String, AstralItemData.DataSchema> SCHEMAS = new ConcurrentHashMap<>();
    private AstralDataSchemas() {}

    public static void register(String id, AstralItemData.DataSchema schema) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"))
            throw new IllegalArgumentException("Expected a namespaced schema ID");
        if (SCHEMAS.putIfAbsent(id, Objects.requireNonNull(schema)) != null)
            throw new IllegalArgumentException("Schema already registered: " + id);
    }

    public static AstralItemData.ValidationResult validate(String id, CompoundTag data) {
        AstralItemData.DataSchema schema = SCHEMAS.get(id);
        return schema == null ? AstralItemData.ValidationResult.error("missing_schema:" + id)
                : AstralItemData.validate(data, schema);
    }
}
