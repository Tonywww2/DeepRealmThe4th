package com.tonywww.deeprealm4th.mixin;

import com.tonywww.deeprealm4th.worldgen.surface.DecorationGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ProtoChunk.class, LevelChunk.class})
public abstract class ChunkWriteMixin {
    @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
    private void fourthLayer$protectDirectChunk(BlockPos pos, BlockState state, boolean moving,
                                                CallbackInfoReturnable<BlockState> cir) {
        if (!DecorationGuard.allow(pos, state)) cir.setReturnValue(null);
    }
}
