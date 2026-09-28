package com.tonywww.deeprealm4th.platform.curios;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.tonywww.deeprealm4th.DeepRealmTheForth;
import com.tonywww.deeprealm4th.astral.score.AstralCurioAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

//? if forge {
import java.util.UUID;
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
/*import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
*///?}

/** Bridges Curios modifier signatures and item metadata across Minecraft versions. */
public final class CurioAttributePlatform {
    private CurioAttributePlatform() {}

    //? if forge {
    public static Multimap<Attribute, AttributeModifier> modifiers(SlotContext slot,
            UUID slotId, ItemStack body) {
        Multimap<Attribute, AttributeModifier> result = HashMultimap.create();
        AstralCurioAttributes.bonuses(slot, body).forEach((key, amount) -> {
            if (!Double.isFinite(amount) || amount == 0) return;
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(key.attributeId()));
            if (attribute == null) return;
            UUID id = AstralCurioAttributes.modifierUuid(slotId.toString(), key);
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
        AstralCurioAttributes.bonuses(slot, body).forEach((key, amount) -> {
            if (!Double.isFinite(amount) || amount == 0) return;
            Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE
                    .getHolder(ResourceLocation.parse(key.attributeId())).orElse(null);
            if (attribute == null) return;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(DeepRealmTheForth.MOD_ID,
                    "astral/" + AstralCurioAttributes.modifierUuid(slotId.toString(), key));
            result.put(attribute, new AttributeModifier(id, amount, switch (key.operation()) {
                case ADD -> AttributeModifier.Operation.ADD_VALUE;
                case MULTIPLY_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                case MULTIPLY_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            }));
        });
        return result;
    }
    *///?}

    public static CompoundTag readSnapshotTag(ItemStack body, String key) {
        //? if forge {
        return body.getTagElement(key);
        //?} else {
        /*CustomData custom = body.get(DataComponents.CUSTOM_DATA);
        return custom == null ? null : custom.copyTag().getCompound(key);
        *///?}
    }

    public static void clearSnapshotTag(ItemStack body, String key) {
        //? if forge {
        CompoundTag tag = body.getTag();
        if (tag != null) tag.remove(key);
        //?} else {
        /*CustomData custom = body.get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            tag.remove(key);
            body.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        *///?}
    }

    public static void writeSnapshotTag(ItemStack body, String key, CompoundTag root) {
        // A component/tag change makes Curios recalculate modifiers for both stacks.
        //? if forge {
        body.getOrCreateTag().put(key, root);
        //?} else {
        /*CustomData custom = body.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = custom == null ? new CompoundTag() : custom.copyTag();
        tag.put(key, root);
        body.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        *///?}
    }
}
