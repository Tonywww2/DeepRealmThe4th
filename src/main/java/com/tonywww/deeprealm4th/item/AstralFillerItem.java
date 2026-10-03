package com.tonywww.deeprealm4th.item;

import com.tonywww.deeprealm4th.astral.score.AstralScoreColors;
import com.tonywww.deeprealm4th.astral.tooltip.AstralTooltips;
import com.tonywww.deeprealm4th.astral.tooltip.FillerPresentations;
import com.tonywww.deeprealm4th.platform.item.VersionedTooltipItem;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A filler with translated description lines and shared activation/conflict hints. */
public class AstralFillerItem extends VersionedTooltipItem {
    private final List<Component> descriptionLines;

    /**
     * Each key is a full translation key, in display order. Subclasses may override
     * {@link #appendAstralTooltip} to add live values or other context.
     */
    public AstralFillerItem(Properties properties, String... descriptionKeys) {
        this(properties, java.util.Arrays.stream(descriptionKeys)
                .map(key -> (Component) Component.translatable(key).withStyle(ChatFormatting.GRAY))
                .toList());
    }

    /** Translated components may contain score names styled with {@link AstralScoreColors}. */
    public AstralFillerItem(Properties properties, List<Component> descriptionLines) {
        super(properties);
        this.descriptionLines = List.copyOf(descriptionLines);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component dynamic = FillerPresentations.name(stack);
        return dynamic == null ? super.getName(stack) : dynamic;
    }

    @Override
    protected void appendAstralTooltip(ItemStack stack, List<Component> tooltip) {
        AstralTooltips.appendFillerComponents(stack, tooltip, descriptionLines);
        tooltip.addAll(FillerPresentations.summary(stack));
        tooltip.addAll(FillerPresentations.tooltip(stack, null));
    }
}
