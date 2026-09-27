package com.tony.deeprealmtheforth.astral;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.tony.deeprealmtheforth.DeepRealmTheForth;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

//? if forge {
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
/*import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
*///?}

/** Supplies Curios modifiers and refreshes the stack snapshot when live scores change. */
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

    private static Map<AstralAttributeKey, Double> bonuses(SlotContext slot, ItemStack body) {
        if (slot.entity() instanceof Player player && player.level().isClientSide
                && body.getItem() instanceof AstralContainerItem item) {
            if (scoreSnapshot(body) != null) return readSnapshot(body);
            ContainerLayout layout = item.layout(body);
            return AstralScoreEngine.calculate(player, body, layout,
                    AstralStoredItems.read(body, layout.size())).attributeBonuses();
        }
        return readSnapshot(body);
    }

    //? if forge {
    public static Multimap<Attribute, AttributeModifier> modifiers(SlotContext slot,
            UUID slotId, ItemStack body) {
        Multimap<Attribute, AttributeModifier> result = HashMultimap.create();
        bonuses(slot, body).forEach((key, amount) -> {
            if (!Double.isFinite(amount) || amount == 0) return;
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(key.attributeId()));
            if (attribute == null) return;
            UUID id = modifierUuid(slotId.toString(), key);
            result.put(attribute, new AttributeModifier(id, "Astral Body", amount,
                    switch (key.operation()) {
                        case ADD -> AttributeModifier.Operation.ADDITION;
                        case MULTIPLY_BASE -> AttributeModifier.Operation.MULTIPLY_BASE;
                        case MULTIPLY_TOTAL -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                    }));
        });
        return result;
    }
    //?} else {
    /*public static Multimap<Holder<Attribute>, AttributeModifier> modifiers(SlotContext slot,
            ResourceLocation slotId, ItemStack body) {
        Multimap<Holder<Attribute>, AttributeModifier> result = HashMultimap.create();
        bonuses(slot, body).forEach((key, amount) -> {
            if (!Double.isFinite(amount) || amount == 0) return;
            Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE
                    .getHolder(ResourceLocation.parse(key.attributeId())).orElse(null);
            if (attribute == null) return;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(DeepRealmTheForth.MOD_ID,
                    "astral/" + modifierUuid(slotId.toString(), key));
            result.put(attribute, new AttributeModifier(id, amount, switch (key.operation()) {
                case ADD -> AttributeModifier.Operation.ADD_VALUE;
                case MULTIPLY_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                case MULTIPLY_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            }));
        });
        return result;
    }
    *///?}

    private static UUID modifierUuid(String slotId, AstralAttributeKey key) {
        return UUID.nameUUIDFromBytes((DeepRealmTheForth.MOD_ID + "/" + slotId + "/"
                + key.attributeId() + "/" + key.operation()).getBytes(StandardCharsets.UTF_8));
    }

    private static Map<AstralAttributeKey, Double> readSnapshot(ItemStack body) {
        //? if forge {
        CompoundTag root = body.getTagElement(SNAPSHOT_KEY);
        //?} else {
        /*CustomData custom = body.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = custom == null ? null : custom.copyTag().getCompound(SNAPSHOT_KEY);
        *///?}
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

    static ScoreSheet scoreSnapshot(ItemStack body) {
        //? if forge {
        CompoundTag root = body.getTagElement(SNAPSHOT_KEY);
        //?} else {
        /*CustomData custom = body.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = custom == null ? null : custom.copyTag().getCompound(SNAPSHOT_KEY);
        *///?}
        if (root == null || !root.contains("Scores", Tag.TAG_COMPOUND)) return null;
        CompoundTag values = root.getCompound("Scores");
        Map<ScoreType, Double> scores = new java.util.EnumMap<>(ScoreType.class);
        for (ScoreType type : ScoreType.values()) {
            double value = values.getDouble(type.id());
            if (!Double.isFinite(value)) return null;
            scores.put(type, value);
        }
        return ScoreSheet.of(scores);
    }

    static Map<AstralAttributeKey, Double> attributeSnapshot(ItemStack body) {
        return readSnapshot(body);
    }

    public static void clearSnapshot(ItemStack body) {
        //? if forge {
        CompoundTag tag = body.getTag();
        if (tag != null) tag.remove(SNAPSHOT_KEY);
        //?} else {
        /*CustomData custom = body.get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            tag.remove(SNAPSHOT_KEY);
            body.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        *///?}
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
        // A component/tag change makes Curios call getAttributeModifiers again for both stacks.
        //? if forge {
        body.getOrCreateTag().put(SNAPSHOT_KEY, root);
        //?} else {
        /*CustomData custom = body.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = custom == null ? new CompoundTag() : custom.copyTag();
        tag.put(SNAPSHOT_KEY, root);
        body.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        *///?}
    }
}
