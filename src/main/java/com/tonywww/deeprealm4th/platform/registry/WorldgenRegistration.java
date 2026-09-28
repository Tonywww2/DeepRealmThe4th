package com.tonywww.deeprealm4th.platform.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.tonywww.deeprealm4th.DeepRealmTheForth;
import com.tonywww.deeprealm4th.worldgen.SpiralChunkGenerator;
import com.tonywww.deeprealm4th.worldgen.biome.SpiralBiomeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;

//? if forge {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

public final class WorldgenRegistration {
    //? if <1.21 {
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, DeepRealmTheForth.MOD_ID);
    private static final DeferredRegister<Codec<? extends BiomeSource>> BIOMES =
            DeferredRegister.create(Registries.BIOME_SOURCE, DeepRealmTheForth.MOD_ID);
    static {
        GENERATORS.register("spiral", () -> SpiralChunkGenerator.CODEC);
        BIOMES.register("spiral", () -> SpiralBiomeSource.CODEC);
    }
    //?} else {
    /*private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, DeepRealmTheForth.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOMES =
            DeferredRegister.create(Registries.BIOME_SOURCE, DeepRealmTheForth.MOD_ID);
    static {
        GENERATORS.register("spiral", () -> SpiralChunkGenerator.MAP_CODEC);
        BIOMES.register("spiral", () -> SpiralBiomeSource.MAP_CODEC);
    }
    *///?}

    private WorldgenRegistration() {}
    public static void register(IEventBus bus) { GENERATORS.register(bus); BIOMES.register(bus); }
}
