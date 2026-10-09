package com.tonywww.deeprealm4th.mixin;

import com.tonywww.deeprealm4th.astral.tooltip.FillerPresentations;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackPresentationMixin {
    @Inject(method = "hasFoil()Z", at = @At("RETURN"), cancellable = true)
    private void deepRealm$presentationFoil(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && FillerPresentations.foil((ItemStack) (Object) this))
            cir.setReturnValue(true);
    }
}
