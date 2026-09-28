package com.tonywww.deeprealm4th.platform.item;

import com.tonywww.deeprealm4th.client.render.ProjectionFrameRenderer;

//? if forge {
import java.util.function.Consumer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
//?}

/** Hooks the projection frame's dynamic renderer into the versioned item API. */
public abstract class VersionedProjectionFrameItem extends VersionedTooltipItem {
    protected VersionedProjectionFrameItem(Properties properties) {
        super(properties);
    }

    //? if forge {
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ProjectionFrameRenderer.INSTANCE;
            }
        });
    }
    //?}
}
