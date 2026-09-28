package com.tonywww.deeprealm4th.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.deeprealm4th.platform.data.ProjectionFrameData;
import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the 16x frame and miniature *actual* absorbed materials from the stack. */
public final class ProjectionFrameRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ProjectionFrameRenderer INSTANCE = new ProjectionFrameRenderer();

    private ProjectionFrameRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        // ItemRenderer shifts the custom model by -0.5 before calling this renderer.
        // renderStatic applies that shift again, so cancel the first one for the
        // shell and every miniature material drawn inside it.
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        minecraft.getItemRenderer().renderStatic(new ItemStack(AstralItemCatalog.PROJECTION_FRAME_SHELL.get()),
                context, light, overlay, pose, buffers, minecraft.level, 0);
        var steps = ProjectionFrameData.read(stack);
        for (int i = 0; i < steps.size(); i++) {
            int column = i % 3, row = i / 3;
            pose.pushPose();
            pose.translate(-0.25 + column * 0.25, 0.25 - row * 0.25, 0.1 + i * 0.001);
            pose.scale(0.21F, 0.21F, 0.21F);
            minecraft.getItemRenderer().renderStatic(steps.get(i).stack(), ItemDisplayContext.GUI,
                    light, overlay, pose, buffers, minecraft.level, i + 1);
            pose.popPose();
        }
        pose.popPose();
    }
}
