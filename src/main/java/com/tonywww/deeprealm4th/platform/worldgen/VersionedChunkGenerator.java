package com.tonywww.deeprealm4th.platform.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

public abstract class VersionedChunkGenerator extends ChunkGenerator {
    protected VersionedChunkGenerator(BiomeSource source) {
        // Decorations are explicitly placed by this generator, never globally injected.
        super(source, biome -> biome.value().getGenerationSettings());
    }

    protected abstract MapCodec<? extends ChunkGenerator> mapCodec();
    protected abstract Codec<? extends ChunkGenerator> forgeCodec();
    protected abstract CompletableFuture<ChunkAccess> generateTerrain(ChunkAccess chunk);

    //? if <1.21 {
    @Override
    protected final Codec<? extends ChunkGenerator> codec() { return forgeCodec(); }

    @Override
    public final CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
            RandomState random, StructureManager structures, ChunkAccess chunk) {
        return generateTerrain(chunk);
    }
    //?} else {
    /*@Override
    protected final MapCodec<? extends ChunkGenerator> codec() { return mapCodec(); }

    @Override
    public final CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
            StructureManager structures, ChunkAccess chunk) {
        return generateTerrain(chunk);
    }
    *///?}
}
