package com.tonywww.deeprealm4th.platform.event;

import com.tonywww.deeprealm4th.travel.MimeticVoidTravel;
import net.minecraft.server.level.ServerPlayer;

//? if forge {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
//?} else {
/*import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
*///?}

/** Check void entry before the player's tick applies damage. */
public final class MimeticVoidTravelEvents {
    private MimeticVoidTravelEvents() {}

    public static void register() {
        //? if forge {
        MinecraftForge.EVENT_BUS.addListener(MimeticVoidTravelEvents::tick);
        //?} else {
        /*NeoForge.EVENT_BUS.addListener(MimeticVoidTravelEvents::tick);
        *///?}
    }

    //? if forge {
    private static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer player) MimeticVoidTravel.tick(player);
    }
    //?} else {
    /*private static void tick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) MimeticVoidTravel.tick(player);
    }
    *///?}

}
