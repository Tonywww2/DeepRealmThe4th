package com.tony.deeprealmtheforth.astral;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

//? if forge {
import net.minecraftforge.network.NetworkHooks;
//?}

public final class AstralMenuOpener {
    private AstralMenuOpener() {}

    /** Opens only a stack actually present in this player's own inventory. */
    public static void open(ServerPlayer player, int inventorySlot) {
        ItemStack owner = player.getInventory().getItem(inventorySlot);
        if (owner.isEmpty() || !(owner.getItem() instanceof AstralContainerItem container)) return;
        ContainerLayout layout = container.layout(owner);
        SimpleMenuProvider provider = new SimpleMenuProvider(
                (id, inventory, ignored) -> new AstralMenu(id, inventory, inventorySlot, owner, layout),
                owner.getHoverName());
        //? if forge {
        NetworkHooks.openScreen(player, provider,
                buffer -> AstralMenu.writeOpeningData(buffer, inventorySlot, layout));
        //?} else {
        /*player.openMenu(provider,
                buffer -> AstralMenu.writeOpeningData(buffer, inventorySlot, layout));
        *///?}
    }
}
