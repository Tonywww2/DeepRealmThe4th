package com.tonywww.deeprealm4th.platform.attributes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

//? if forge {
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
/*import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
*///?}

/** Version bridge for querying a player's already-applied attribute value. */
public final class AstralAttributeValues {
    private AstralAttributeValues() {}

    public static double get(Player player, String attributeId) {
        ResourceLocation key = ResourceLocation.tryParse(attributeId);
        if (key == null) throw new IllegalArgumentException("Invalid attribute ID: " + attributeId);
        //? if forge {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(key);
        if (attribute == null || !player.getAttributes().hasAttribute(attribute)) return Double.NaN;
        return player.getAttributeValue(attribute);
        //?} else {
        /*Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(key).orElse(null);
        if (attribute == null || !player.getAttributes().hasAttribute(attribute)) return Double.NaN;
        return player.getAttributeValue(attribute);
        *///?}
    }
}
