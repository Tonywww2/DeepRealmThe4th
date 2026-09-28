package com.tonywww.deeprealm4th.item;

import com.tonywww.deeprealm4th.platform.recipe.ProjectionRecipe;

import com.tonywww.deeprealm4th.client.render.ProjectionFrameRenderer;

import com.tonywww.deeprealm4th.platform.data.ProjectionFrameData;
import com.tonywww.deeprealm4th.platform.item.VersionedProjectionFrameItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Carries up to nine actual absorbed stacks, including their item data. */
public final class ProjectionFrameItem extends VersionedProjectionFrameItem {
    public ProjectionFrameItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    protected void appendAstralTooltip(ItemStack stack, List<Component> tooltip) {
        appendSteps(stack, tooltip);
    }

    private static void appendSteps(ItemStack stack, List<Component> tooltip) {
        var steps = ProjectionFrameData.read(stack);
        tooltip.add(Component.translatable("tooltip.deeprealm_4th.projection_frame.progress", steps.size())
                .withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < steps.size(); i++) {
            var entry = steps.get(i);
            String direction = entry.direction() == com.tonywww.deeprealm4th.platform.recipe.ProjectionRecipe.Direction.MATERIAL_ON_FRAME
                    ? "material_on_frame" : "frame_on_material";
            tooltip.add(Component.translatable("tooltip.deeprealm_4th.projection_frame.step", i + 1,
                    entry.stack().getHoverName(), Component.translatable("tooltip.deeprealm_4th.direction." + direction))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        if (!steps.isEmpty()) tooltip.add(Component.translatable("tooltip.deeprealm_4th.projection_frame.dismantle")
                .withStyle(ChatFormatting.GRAY));
    }
}
