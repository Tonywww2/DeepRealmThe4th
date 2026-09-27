package com.tony.deeprealmtheforth.platform.loot;

import com.tony.deeprealmtheforth.DeepRealmTheForth;

//? if forge {
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
/*import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
*///?}

public final class AstralLootRegistration {
    //? if forge {
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    DeepRealmTheForth.MOD_ID);
    //?} else {
    /*private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    DeepRealmTheForth.MOD_ID);
    *///?}

    static {
        MODIFIERS.register("fourth_layer_gems", () -> FourthLayerGemLootModifier.CODEC);
    }

    private AstralLootRegistration() {}

    public static void register(IEventBus bus) {
        MODIFIERS.register(bus);
    }
}
