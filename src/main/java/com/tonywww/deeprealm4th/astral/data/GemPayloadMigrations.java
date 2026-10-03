package com.tonywww.deeprealm4th.astral.data;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Explicit migration entry point. Unknown legacy values are never guessed. */
public final class GemPayloadMigrations {
    public static final int MISSING = -1;
    public static final int LEGACY = 0;
    private static final Map<Integer, Migrator> MIGRATORS = new ConcurrentHashMap<>();
    private GemPayloadMigrations() {}

    @FunctionalInterface
    public interface Migrator {
        GemPayload migrate(ItemStack source, CompoundTag oldData);
    }

    public record MigrationResult(ItemStack stack, String error) {
        public boolean ok() { return error == null; }
    }

    public static int version(CompoundTag data) {
        if (data == null) return MISSING;
        return data.contains("schema", Tag.TAG_INT) ? data.getInt("schema") : LEGACY;
    }

    public static void register(int oldVersion, Migrator migrator) {
        if (oldVersion < 0 || oldVersion == GemPayload.SCHEMA)
            throw new IllegalArgumentException("Invalid migration source version");
        if (MIGRATORS.putIfAbsent(oldVersion, Objects.requireNonNull(migrator)) != null)
            throw new IllegalArgumentException("Migration already registered for schema " + oldVersion);
    }

    public static MigrationResult migrateCopy(ItemStack source, String oldKey, String newKey) {
        CompoundTag old = AstralItemData.read(source, oldKey);
        int version = version(old);
        if (version == MISSING) return new MigrationResult(null, "missing_gem_data");
        if (version == GemPayload.SCHEMA) {
            GemPayload.DecodeResult decoded = GemPayload.decode(old);
            return decoded.ok() ? new MigrationResult(AstralItemData.writeCopy(source, newKey,
                    decoded.value().encode()), null) : new MigrationResult(null, decoded.error());
        }
        Migrator migrator = MIGRATORS.get(version);
        if (migrator == null) return new MigrationResult(null, "gem_schema_requires_migration:" + version);
        try {
            GemPayload converted = migrator.migrate(source.copy(), old.copy());
            if (converted == null) return new MigrationResult(null, "migration_rejected");
            return new MigrationResult(AstralItemData.writeCopy(source, newKey, converted.encode()), null);
        } catch (RuntimeException exception) {
            return new MigrationResult(null, "migration_error:" + exception.getMessage());
        }
    }
}
