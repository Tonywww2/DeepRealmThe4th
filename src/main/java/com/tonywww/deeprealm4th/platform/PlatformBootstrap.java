package com.tonywww.deeprealm4th.platform;

import com.tonywww.deeprealm4th.platform.event.AstralCurioSlotGuard;
import com.tonywww.deeprealm4th.platform.registry.AstralCreativeTabRegistration;
import com.tonywww.deeprealm4th.platform.registry.AstralItemRegistration;
import com.tonywww.deeprealm4th.platform.registry.AstralMenuRegistration;
import com.tonywww.deeprealm4th.platform.registry.AstralProcessRegistration;
import com.tonywww.deeprealm4th.platform.registry.AstralBlockRegistration;
import com.tonywww.deeprealm4th.platform.registry.AstralBlockEntityRegistration;
import com.tonywww.deeprealm4th.platform.event.MimeticVoidTravelEvents;
import com.tonywww.deeprealm4th.platform.registry.AstralLootRegistration;
import com.tonywww.deeprealm4th.platform.registry.WorldgenRegistration;

//? if forge {
import net.minecraftforge.eventbus.api.IEventBus;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
*///?}

public final class PlatformBootstrap {
    private PlatformBootstrap() {}

    public static void initialize(IEventBus bus) {
        AstralBlockRegistration.register(bus);
        AstralBlockEntityRegistration.register(bus);
        AstralItemRegistration.register(bus);
        AstralCreativeTabRegistration.register(bus);
        AstralCurioSlotGuard.register();
        AstralMenuRegistration.register(bus);
        AstralProcessRegistration.register(bus);
        MimeticVoidTravelEvents.register();
        AstralLootRegistration.register(bus);
        WorldgenRegistration.register(bus);
    }
}
