package com.tony.deeprealmtheforth.platform;

import com.tony.deeprealmtheforth.DeepRealmTheForth;
import net.minecraft.resources.ResourceLocation;

public final class PlatformIds {
    private PlatformIds() {}

    public static ResourceLocation id(String path) { return parse(DeepRealmTheForth.MOD_ID + ":" + path); }

    @SuppressWarnings("removal")
    public static ResourceLocation parse(String value) {
        //? if <1.21 {
        return new ResourceLocation(value);
        //?} else {
        /*return ResourceLocation.parse(value);
        *///?}
    }
}
