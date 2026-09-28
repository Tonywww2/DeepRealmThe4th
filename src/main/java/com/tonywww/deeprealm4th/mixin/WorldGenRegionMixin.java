package com.tonywww.deeprealm4th.mixin;

import com.tonywww.deeprealm4th.worldgen.surface.DecorationGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {
    @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
    private void fourthLayer$protectTerrain(BlockPos pos, BlockState state, int flags, int recursion,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!DecorationGuard.allow(pos, state)) cir.setReturnValue(false);
    }
}
