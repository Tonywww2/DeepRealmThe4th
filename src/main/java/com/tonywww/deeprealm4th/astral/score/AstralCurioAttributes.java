package com.tonywww.deeprealm4th.astral.score;

import com.tonywww.deeprealm4th.DeepRealmTheForth;
import com.tonywww.deeprealm4th.astral.ScoreType;
import com.tonywww.deeprealm4th.astral.container.ContainerLayout;
import com.tonywww.deeprealm4th.item.AstralContainerItem;
import com.tonywww.deeprealm4th.platform.curios.CurioAttributePlatform;
import com.tonywww.deeprealm4th.platform.data.AstralStoredItems;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

/** Calculates and persists the bonuses displayed and supplied by an equipped astral body. */
public final class AstralCurioAttributes {
    private static final String SNAPSHOT_KEY = "AstralAttributeBonuses";

    private AstralCurioAttributes() {}

    public static void refresh(SlotContext slot, ItemStack body) {
        if (!(slot.entity() instanceof Player player) || player.level().isClientSide
                || !(body.getItem() instanceof AstralContainerItem item)) return;
        ContainerLayout layout = item.layout(body);
        AstralScoreEngine.Result result = AstralScoreEngine.calculate(player, body, layout,
                AstralStoredItems.read(body, layout.size()));
        Map<AstralAttributeKey, Double> active = new HashMap<>();
        result.attributeBonuses().forEach((key, amount) -> {
            if (Double.isFinite(amount) && amount != 0) active.put(key, amount);
        });
        if (!active.equals(readSnapshot(body)) || !result.scores().equals(scoreSnapshot(body))) {
            writeSnapshot(body, active, result.scores());
        }
    }

    public static Map<AstralAttributeKey, Double> bonuses(SlotContext slot, ItemStack body) {
        if (slot.entity() instanceof Player player && player.level().isClientSide
                && body.getItem() instanceof AstralContainerItem item) {
            if (scoreSnapshot(body) != null) return readSnapshot(body);
            ContainerLayout layout = item.layout(body);
            return AstralScoreEngine.calculate(player, body, layout,
                    AstralStoredItems.read(body, layout.size())).attributeBonuses();
        }
        return readSnapshot(body);
    }

    public static UUID modifierUuid(String slotId, AstralAttributeKey key) {
        return UUID.nameUUIDFromBytes((DeepRealmTheForth.MOD_ID + "/" + slotId + "/"
                + key.attributeId() + "/" + key.operation()).getBytes(StandardCharsets.UTF_8));
    }

    private static Map<AstralAttributeKey, Double> readSnapshot(ItemStack body) {
        CompoundTag root = CurioAttributePlatform.readSnapshotTag(body, SNAPSHOT_KEY);
        if (root == null) return Map.of();
        Map<AstralAttributeKey, Double> values = new HashMap<>();
        ListTag entries = root.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            try {
                AstralAttributeKey key = new AstralAttributeKey(entry.getString("Attribute"),
                        AstralAttributeOperation.valueOf(entry.getString("Operation")));
                double amount = entry.getDouble("Amount");
                if (Double.isFinite(amount) && amount != 0) values.put(key, amount);
            } catch (IllegalArgumentException ignored) {
                // Ignore an obsolete or malformed saved modifier.
            }
        }
        return values;
    }

    public static ScoreSheet scoreSnapshot(ItemStack body) {
        CompoundTag root = CurioAttributePlatform.readSnapshotTag(body, SNAPSHOT_KEY);
        if (root == null || !root.contains("Scores", Tag.TAG_COMPOUND)) return null;
        CompoundTag values = root.getCompound("Scores");
        Map<ScoreType, Double> scores = new EnumMap<>(ScoreType.class);
        for (ScoreType type : ScoreType.values()) {
            double value = values.getDouble(type.id());
            if (!Double.isFinite(value)) return null;
            scores.put(type, value);
        }
        return ScoreSheet.of(scores);
    }

    public static Map<AstralAttributeKey, Double> attributeSnapshot(ItemStack body) {
        return readSnapshot(body);
    }

    public static void clearSnapshot(ItemStack body) {
        CurioAttributePlatform.clearSnapshotTag(body, SNAPSHOT_KEY);
    }

    private static void writeSnapshot(ItemStack body, Map<AstralAttributeKey, Double> values,
            ScoreSheet scores) {
        CompoundTag root = new CompoundTag();
        ListTag entries = new ListTag();
        values.entrySet().stream().sorted((left, right) ->
                (left.getKey().attributeId() + left.getKey().operation()).compareTo(
                        right.getKey().attributeId() + right.getKey().operation())).forEach(entry -> {
            CompoundTag value = new CompoundTag();
            value.putString("Attribute", entry.getKey().attributeId());
            value.putString("Operation", entry.getKey().operation().name());
            value.putDouble("Amount", entry.getValue());
            entries.add(value);
        });
        root.put("Entries", entries);
        CompoundTag scoreValues = new CompoundTag();
        for (ScoreType type : ScoreType.values()) scoreValues.putDouble(type.id(), scores.get(type));
        root.put("Scores", scoreValues);
        CurioAttributePlatform.writeSnapshotTag(body, SNAPSHOT_KEY, root);
    }
}
