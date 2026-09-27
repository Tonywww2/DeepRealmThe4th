package com.tony.deeprealmtheforth.astral;

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
        ContainerLayout layout = item.layout(stack);
        tooltip.add(Component.translatable(PREFIX + "base_container.open").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(PREFIX + "base_container.layout",
                layout.width(), layout.height(), layout.openCount()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(PREFIX + "base_container.equipped").withStyle(ChatFormatting.GOLD));
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
            tooltip.add(Component.translatable(PREFIX + "filler.equipped")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static String number(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
}
