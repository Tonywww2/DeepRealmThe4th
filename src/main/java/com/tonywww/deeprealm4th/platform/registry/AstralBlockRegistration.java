package com.tonywww.deeprealm4th.platform.registry;

import com.tonywww.deeprealm4th.DeepRealmTheForth;
import com.tonywww.deeprealm4th.block.StarSlurrySeepBlock;
import com.tonywww.deeprealm4th.block.ForgingPadBlock;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

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
    public static final Supplier<Block> STAR_SLURRY_SEEP = BLOCKS.register("star_slurry_seep",
            () -> new StarSlurrySeepBlock(BlockBehaviour.Properties.of().noCollission()
                    .strength(0.5F).sound(SoundType.GLASS).lightLevel(state -> 4)));
    public static final Supplier<Block> FORGING_PAD = BLOCKS.register("forging_pad",
            () -> new ForgingPadBlock(BlockBehaviour.Properties.of().strength(0.5F)
                    .sound(SoundType.WOOL).noOcclusion()));

    private AstralBlockRegistration() {}

    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
