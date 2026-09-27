package com.tony.deeprealmtheforth.astral;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Small factory for KubeJS custom-item registration; Java addons may subclass directly. */
public final class AstralContainers {
    private AstralContainers() {}

    public static AstralContainerItem create(int width, int height, String... rows) {
        ContainerLayout layout = ContainerLayout.parse(width, height, List.of(rows));
        return new AstralContainerItem(new Item.Properties()) {
            @Override
            public ContainerLayout layout(ItemStack stack) {
                return layout;
            }
        };
    }
}
