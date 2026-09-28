package com.tonywww.deeprealm4th.worldgen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;

public record BiomePool(HolderSet<Biome> biomes, Holder<Biome> fallback) {
    public static final Codec<BiomePool> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomePool::biomes),
            Biome.CODEC.fieldOf("fallback").forGetter(BiomePool::fallback)
    ).apply(i, BiomePool::new));
}
