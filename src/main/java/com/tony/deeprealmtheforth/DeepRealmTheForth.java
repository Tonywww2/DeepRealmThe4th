package com.tony.deeprealmtheforth;

import com.tony.deeprealmtheforth.astral.AstralConfig;
import com.tony.deeprealmtheforth.platform.PlatformBootstrap;

//? if forge {
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
*///?}

@Mod(DeepRealmTheForth.MOD_ID)
public final class DeepRealmTheForth {
    public static final String MOD_ID = "deeprealm_4th";

    //? if forge {
    @SuppressWarnings("removal")
    public DeepRealmTheForth() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AstralConfig.SPEC);
        PlatformBootstrap.initialize(FMLJavaModLoadingContext.get().getModEventBus());
    //?} else {
    /*public DeepRealmTheForth(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, AstralConfig.SPEC);
        PlatformBootstrap.initialize(modBus);
    *///?}
    }
}
