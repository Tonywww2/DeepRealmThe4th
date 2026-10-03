package com.tonywww.deeprealm4th.platform.block;

import net.minecraft.world.level.block.AmethystBlock;

//? if !forge {
/*import com.mojang.serialization.MapCodec;
import com.tonywww.deeprealm4th.block.StarSourceBlock;
*///?}

/** Keeps the block codec added in 1.21 out of the shared growth logic. */
public abstract class VersionedStarSourceBlock extends AmethystBlock {
    protected VersionedStarSourceBlock(Properties properties) { super(properties); }

    //? if !forge {
    /*private static final MapCodec<StarSourceBlock> CODEC = simpleCodec(StarSourceBlock::new);

    @Override
    public MapCodec<StarSourceBlock> codec() { return CODEC; }
    *///?}
}
