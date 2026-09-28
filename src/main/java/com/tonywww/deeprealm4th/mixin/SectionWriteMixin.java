package com.tonywww.deeprealm4th.mixin;

import com.tonywww.deeprealm4th.worldgen.surface.DecorationGuard;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** OreFeature uses BulkSectionAccess, bypassing both WorldGenRegion and ChunkAccess. */
@Mixin(LevelChunkSection.class)
public abstract class SectionWriteMixin {
    @Inject(method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true)
    private void fourthLayer$protectBulkWrite(int x, int y, int z, BlockState state, boolean lock,
                                              CallbackInfoReturnable<BlockState> cir) {
        LevelChunkSection self = (LevelChunkSection) (Object) this;
        if (!DecorationGuard.allowSection(self, x, y, z, state)) cir.setReturnValue(self.getBlockState(x, y, z));
    }
}
