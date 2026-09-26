package com.tony.deeprealmtheforth.platform;

import com.tony.deeprealmtheforth.platform.commands.FourthLayerCommands;
import com.tony.deeprealmtheforth.platform.items.AstralCurioSlotGuard;
import com.tony.deeprealmtheforth.platform.items.AstralCreativeTabRegistration;
import com.tony.deeprealmtheforth.platform.items.AstralItemRegistration;
import com.tony.deeprealmtheforth.platform.items.AstralMenuRegistration;
import com.tony.deeprealmtheforth.platform.worldgen.WorldgenRegistration;

//? if forge {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
//?} else {
/*import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.IEventBus;
*///?}

public final class PlatformBootstrap {
    private PlatformBootstrap() {}

    public static void initialize(IEventBus bus) {
        AstralItemRegistration.register(bus);
        AstralCreativeTabRegistration.register(bus);
        AstralCurioSlotGuard.register();
        AstralMenuRegistration.register(bus);
        WorldgenRegistration.register(bus);
        //? if forge {
        MinecraftForge.EVENT_BUS.addListener(PlatformBootstrap::commands);
        //?} else {
        /*NeoForge.EVENT_BUS.addListener(PlatformBootstrap::commands);
        *///?}
    }

    private static void commands(RegisterCommandsEvent event) { FourthLayerCommands.register(event.getDispatcher()); }
}
