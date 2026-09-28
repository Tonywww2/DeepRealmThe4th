package com.tonywww.deeprealm4th.astral.tooltip;

import com.tonywww.deeprealm4th.item.AstralContainerItem;
import com.tonywww.deeprealm4th.astral.AstralFillers;
import com.tonywww.deeprealm4th.astral.FillerDefinition;

import com.tonywww.deeprealm4th.astral.container.AstralClientPreview;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Shared tooltip wording for built-in fillers and Java addon item subclasses. */
public final class AstralTooltips {
    private static final String PREFIX = "tooltip.deeprealm_4th.";

    private AstralTooltips() {}

    public static void appendContainer(ItemStack stack, AstralContainerItem item, List<Component> tooltip) {
        tooltip.add(Component.translatable(PREFIX + "base_container.open").withStyle(ChatFormatting.GRAY));
        AstralClientPreview.appendContainer(stack, item, tooltip);
    }

    public static void appendFiller(ItemStack stack, List<Component> tooltip, List<String> descriptionKeys) {
        appendFillerComponents(stack, tooltip, descriptionKeys.stream()
                .map(key -> (Component) Component.translatable(key).withStyle(ChatFormatting.GRAY))
                .toList());
    }

    public static void appendFillerComponents(ItemStack stack, List<Component> tooltip,
                                              List<Component> descriptionLines) {
        tooltip.addAll(descriptionLines);
        FillerDefinition definition = AstralFillers.find(stack);
        if (definition != null) {
            tooltip.add(Component.translatable(PREFIX + "activation."
                    + definition.activation().name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.DARK_AQUA));
            for (String itemId : definition.suppressedBy().stream().sorted().toList()) {
                String[] parts = itemId.split(":", 2);
                tooltip.add(Component.translatable(PREFIX + "common.suppressed_by",
                        Component.translatable("item." + parts[0] + "." + parts[1]))
                        .withStyle(ChatFormatting.RED));
            }
        }
    }

    public static String number(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
}
