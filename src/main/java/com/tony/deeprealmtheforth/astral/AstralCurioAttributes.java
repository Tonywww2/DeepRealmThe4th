package com.tony.deeprealmtheforth.astral;

import com.tony.deeprealmtheforth.DeepRealmTheForth;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

//? if forge {
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
/*import net.minecraft.core.Holder;
*///?}

/** Applies only medal-generated attributes while the owning Curios item is worn. */
public final class AstralCurioAttributes {
    private static final Map<Player, Map<String, Set<AstralAttributeKey>>> APPLIED = new WeakHashMap<>();

    private AstralCurioAttributes() {}

    public static void tick(SlotContext slot, ItemStack body) {
        if (!(slot.entity() instanceof Player player) || player.level().isClientSide) return;
        if (player.tickCount % 5 != 0) return;
        if (!(body.getItem() instanceof AstralContainerItem item)) return;
        ContainerLayout layout = item.layout(body);
        AstralScoreEngine.Result result = AstralScoreEngine.calculate(player, body, layout,
                AstralStoredItems.read(body, layout.size()));
        sync(player, slot, result.attributeBonuses());
    }

    public static void remove(SlotContext slot) {
        if (!(slot.entity() instanceof Player player) || player.level().isClientSide) return;
        String slotKey = slotKey(slot);
        Map<String, Set<AstralAttributeKey>> playerEntries = APPLIED.get(player);
        if (playerEntries == null) return;
        Set<AstralAttributeKey> old = playerEntries.remove(slotKey);
        if (old != null) {
            for (AstralAttributeKey key : old) updateModifier(player, slot, key, 0);
        }
        if (playerEntries.isEmpty()) APPLIED.remove(player);
    }

    private static void sync(Player player, SlotContext slot, Map<AstralAttributeKey, Double> desired) {
        Map<String, Set<AstralAttributeKey>> playerEntries = APPLIED.computeIfAbsent(player, ignored -> new HashMap<>());
        String slotKey = slotKey(slot);
        Set<AstralAttributeKey> previous = playerEntries.getOrDefault(slotKey, Set.of());
        Set<AstralAttributeKey> all = new HashSet<>(previous);
        all.addAll(desired.keySet());
        for (AstralAttributeKey key : all) {
            updateModifier(player, slot, key, desired.getOrDefault(key, 0.0));
        }
        if (desired.isEmpty()) playerEntries.remove(slotKey);
        else playerEntries.put(slotKey, Set.copyOf(desired.keySet()));
    }

    private static String slotKey(SlotContext slot) {
        return slot.identifier() + "/" + slot.index();
    }

    private static UUID modifierUuid(SlotContext slot, AstralAttributeKey key) {
        return UUID.nameUUIDFromBytes((DeepRealmTheForth.MOD_ID + "/" + slotKey(slot) + "/"
                + key.attributeId() + "/" + key.operation())
                .getBytes(StandardCharsets.UTF_8));
    }

    private static void updateModifier(Player player, SlotContext slot, AstralAttributeKey key, double amount) {
        if (!Double.isFinite(amount)) return;
        //? if forge {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(key.attributeId()));
        if (attribute == null) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        UUID id = modifierUuid(slot, key);
        AttributeModifier old = instance.getModifier(id);
        if (old != null && old.getAmount() == amount) return;
        if (old != null) instance.removeModifier(id);
        if (amount != 0) {
            instance.addTransientModifier(new AttributeModifier(id, "Astral body " + slotKey(slot),
                    amount, switch (key.operation()) {
                        case ADD -> AttributeModifier.Operation.ADDITION;
                        case MULTIPLY_BASE -> AttributeModifier.Operation.MULTIPLY_BASE;
                        case MULTIPLY_TOTAL -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                    }));
        }
        //?} else {
        /*var holder = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(key.attributeId()));
        if (holder.isEmpty()) return;
        AttributeInstance instance = player.getAttribute(holder.get());
        if (instance == null) return;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(DeepRealmTheForth.MOD_ID,
                "astral/" + modifierUuid(slot, key));
        AttributeModifier old = instance.getModifier(id);
        if (old != null && old.amount() == amount) return;
        if (old != null) instance.removeModifier(id);
        if (amount != 0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, switch (key.operation()) {
                case ADD -> AttributeModifier.Operation.ADD_VALUE;
                case MULTIPLY_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                case MULTIPLY_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            }));
        }
        *///?}
    }
}
