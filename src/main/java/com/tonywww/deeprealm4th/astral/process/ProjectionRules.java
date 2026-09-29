package com.tonywww.deeprealm4th.astral.process;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Ordered material and direction matching for projection combining. */
public final class ProjectionRules {
    private ProjectionRules() {}

    public static void validate(ItemStack result, List<?> steps) {
        if (result == null || result.isEmpty() || result.getCount() > result.getMaxStackSize())
            throw new IllegalArgumentException("Invalid projection result stack");
        if (steps.size() < 2 || steps.size() > 9)
            throw new IllegalArgumentException("Projection steps must be 2..9");
    }
}
