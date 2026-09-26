package com.tony.deeprealmtheforth.platform.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.biome.BiomeSource;

public abstract class VersionedBiomeSource extends BiomeSource {
    protected abstract MapCodec<? extends BiomeSource> mapCodec();
    protected abstract Codec<? extends BiomeSource> legacyCodec();

    //? if <1.21 {
    @Override
    protected final Codec<? extends BiomeSource> codec() { return legacyCodec(); }
    //?} else {
    /*@Override
    protected final MapCodec<? extends BiomeSource> codec() { return mapCodec(); }
    *///?}
}
