package com.tonywww.deeprealm4th.platform.registry;

import com.tonywww.deeprealm4th.DeepRealmTheForth;
import com.tonywww.deeprealm4th.block.StarSourceBlock;
import com.tonywww.deeprealm4th.block.StarSlurryCrystalBlock;
import com.tonywww.deeprealm4th.block.ForgingPadBlock;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

//? if forge {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

public final class AstralBlockRegistration {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, DeepRealmTheForth.MOD_ID);
    public static final Supplier<Block> STAR_SOURCE = BLOCKS.register("star_source",
            () -> new StarSourceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops()
                    .randomTicks().pushReaction(PushReaction.DESTROY).lightLevel(state -> 4)));
    public static final Supplier<Block> SMALL_STAR_SLURRY_BUD = crystal("small_star_slurry_bud", 3, 4, 1);
    public static final Supplier<Block> MEDIUM_STAR_SLURRY_BUD = crystal("medium_star_slurry_bud", 4, 3, 2);
    public static final Supplier<Block> LARGE_STAR_SLURRY_BUD = crystal("large_star_slurry_bud", 5, 3, 4);
    public static final Supplier<Block> STAR_SLURRY_CLUSTER = crystal("star_slurry_cluster", 7, 3, 5);
    public static final Supplier<Block> FORGING_PAD = BLOCKS.register("forging_pad",
            () -> new ForgingPadBlock(BlockBehaviour.Properties.of().strength(0.5F)
                    .sound(SoundType.WOOL).noOcclusion()));

    private AstralBlockRegistration() {}

    private static Supplier<Block> crystal(String id, int height, int offset, int light) {
        return BLOCKS.register(id, () -> new StarSlurryCrystalBlock(height, offset,
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noOcclusion()
                        .strength(1.5F).sound(SoundType.AMETHYST_CLUSTER)
                        .pushReaction(PushReaction.DESTROY).lightLevel(state -> light)));
    }

    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
