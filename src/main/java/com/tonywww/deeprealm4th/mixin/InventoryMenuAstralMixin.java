package com.tonywww.deeprealm4th.mixin;

import com.tonywww.deeprealm4th.item.AstralContainerItem;
import com.tonywww.deeprealm4th.platform.menu.AstralMenuOpener;
import com.tonywww.deeprealm4th.astral.process.ProjectionInventoryClick;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class InventoryMenuAstralMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void deepRealm$openAstral(int slotId, int button, ClickType clickType,
                                      Player player, CallbackInfo ci) {
        if (clickType != ClickType.PICKUP || button != 1 || slotId < 0) return;
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (!(self instanceof InventoryMenu)) return;
        if (slotId >= self.slots.size()) return;
        if (ProjectionInventoryClick.handle(self, slotId, player)) {
            ci.cancel();
            return;
        }
        if (!player.containerMenu.getCarried().isEmpty()) return;
        Slot slot = self.getSlot(slotId);
        if (slot.container != player.getInventory()
                || !(slot.getItem().getItem() instanceof AstralContainerItem)) return;
        ci.cancel();
        if (player instanceof ServerPlayer serverPlayer) {
            AstralMenuOpener.open(serverPlayer, slot.getContainerSlot());
        }
    }
}
