package com.tonywww.deeprealm4th.platform;

import com.tonywww.deeprealm4th.DeepRealmTheForth;
import com.tonywww.deeprealm4th.platform.config.AstralConfig;

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

/** Loader-owned mod entrypoint; common code only depends on the stable mod ID. */
@Mod(DeepRealmTheForth.MOD_ID)
public final class ModEntrypoint {
    //? if forge {
    @SuppressWarnings("removal")
    public ModEntrypoint() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AstralConfig.SPEC);
        PlatformBootstrap.initialize(FMLJavaModLoadingContext.get().getModEventBus());
    //?} else {
    /*public ModEntrypoint(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, AstralConfig.SPEC);
        PlatformBootstrap.initialize(modBus);
    *///?}
    }
}
