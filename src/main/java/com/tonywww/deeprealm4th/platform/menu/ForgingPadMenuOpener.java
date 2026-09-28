package com.tonywww.deeprealm4th.platform.menu;

import com.tonywww.deeprealm4th.blockentity.ForgingPadBlockEntity;
import com.tonywww.deeprealm4th.menu.ForgingPadMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

//? if forge {
import net.minecraftforge.network.NetworkHooks;
//?}

public final class ForgingPadMenuOpener {
    private ForgingPadMenuOpener() {}

    public static void open(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockEntity(pos) instanceof ForgingPadBlockEntity pad)) return;
        SimpleMenuProvider provider = new SimpleMenuProvider(
                (id, inventory, ignored) -> new ForgingPadMenu(id, inventory, pos, pad),
                Component.translatable("block.deeprealm_4th.forging_pad"));
        //? if forge {
        NetworkHooks.openScreen(player, provider, buffer -> buffer.writeBlockPos(pos));
        //?} else {
        /*player.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
        *///?}
    }
}
