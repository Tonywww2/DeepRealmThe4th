package com.tonywww.deeprealm4th.item;

import com.tonywww.deeprealm4th.platform.item.VersionedTooltipItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Material hints, independent of tooltip API signatures. */
public final class AstralMaterialItem extends VersionedTooltipItem {
    private final String hint;

    public AstralMaterialItem(Properties properties, String hint) {
        super(properties);
        this.hint = hint;
    }

    @Override
    protected void appendAstralTooltip(ItemStack stack, List<Component> tooltip) {
        if (hint != null) tooltip.add(Component.translatable("tooltip.deeprealm_4th.material." + hint)
                .withStyle(ChatFormatting.GRAY));
    }
}
