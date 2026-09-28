package com.tonywww.deeprealm4th.platform;

import com.tonywww.deeprealm4th.DeepRealmTheForth;
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
