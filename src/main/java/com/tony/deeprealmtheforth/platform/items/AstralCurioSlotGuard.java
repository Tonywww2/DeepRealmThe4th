package com.tony.deeprealmtheforth.platform.items;

import com.tony.deeprealmtheforth.astral.AstralContainerItem;

//? if forge {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
//?} else {
/*import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
*///?}

/** Keeps the dedicated Curios slot exclusive even for items tagged as a generic curio. */
public final class AstralCurioSlotGuard {
    public static final String SLOT_ID = "base_container";

    private AstralCurioSlotGuard() {}

    public static void register() {
        //? if forge {
        MinecraftForge.EVENT_BUS.addListener(AstralCurioSlotGuard::onEquip);
        //?} else {
        /*NeoForge.EVENT_BUS.addListener(AstralCurioSlotGuard::onEquip);
        *///?}
    }

    //? if forge {
    private static void onEquip(CurioEquipEvent event) {
        if (SLOT_ID.equals(event.getSlotContext().identifier())
                && !(event.getStack().getItem() instanceof AstralContainerItem)) {
            event.setResult(Event.Result.DENY);
        }
    }
    //?} else {
    /*private static void onEquip(CurioCanEquipEvent event) {
        if (SLOT_ID.equals(event.getSlotContext().identifier())
                && !(event.getStack().getItem() instanceof AstralContainerItem)) {
            event.setEquipResult(TriState.FALSE);
        }
    }
    *///?}
}
