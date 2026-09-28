package com.tonywww.deeprealm4th.platform.loot;

import com.tonywww.deeprealm4th.loot.FourthLayerGemLoot;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

//? if forge {
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
//?} else {
/*import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
*///?}

/** Loader-specific loot modifier and codec for the shared Fourth Layer gem pool. */
public final class FourthLayerGemLootModifier implements IGlobalLootModifier {
    //? if forge {
    public static final Codec<FourthLayerGemLootModifier> CODEC = Codec.unit(new FourthLayerGemLootModifier());
    //?} else {
    /*public static final MapCodec<FourthLayerGemLootModifier> CODEC = MapCodec.unit(new FourthLayerGemLootModifier());
    *///?}

    private FourthLayerGemLootModifier() {}

    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        FourthLayerGemLoot.add(generatedLoot, context);
        return generatedLoot;
    }

    //? if forge {
    @Override
    public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
    //?} else {
    /*@Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
    *///?}
}
