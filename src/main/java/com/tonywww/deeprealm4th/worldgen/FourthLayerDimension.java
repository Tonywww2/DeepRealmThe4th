package com.tonywww.deeprealm4th.worldgen;

import com.tonywww.deeprealm4th.platform.PlatformIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class FourthLayerDimension {
    public static final ResourceKey<Level> KEY =
            ResourceKey.create(Registries.DIMENSION, PlatformIds.id("fourth_layer"));

    private FourthLayerDimension() {}
}
