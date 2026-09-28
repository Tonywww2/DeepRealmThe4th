package com.tonywww.deeprealm4th.astral.container;

import com.tonywww.deeprealm4th.item.AstralContainerItem;
import net.minecraft.world.item.ItemStack;

/** Membership rule for the dedicated astral body Curios slot. */
public final class AstralSlotRules {
    public static final String SLOT_ID = "base_container";

    private AstralSlotRules() {}

    public static boolean rejects(String slotId, ItemStack stack) {
        return SLOT_ID.equals(slotId) && !(stack.getItem() instanceof AstralContainerItem);
    }
}
