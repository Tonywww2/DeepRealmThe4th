package com.tonywww.deeprealm4th.travel;

import com.tonywww.deeprealm4th.platform.config.AstralConfig;
import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Consumes the catalyst only after successful void entry. */
public final class MimeticVoidTravel {
    private MimeticVoidTravel() {}

    public static void tick(ServerPlayer player) {
        if (!AstralConfig.permitsVoidEntry(player.level().dimension())) return;
        InteractionHand hand = heldCatalyst(player);
        if (hand == null) return;
        // The End starts applying void damage below its build limit. Try entry as
        // soon as the player crosses that limit, before a later tick can kill them.
        if (player.getY() >= player.level().getMinBuildHeight()) return;
        if (FourthLayerTravel.enterFromVoid(player)) {
            ItemStack catalyst = player.getItemInHand(hand);
            if (!player.getAbilities().instabuild) catalyst.shrink(1);
        }
    }

    private static InteractionHand heldCatalyst(ServerPlayer player) {
        if (player.getMainHandItem().is(AstralItemCatalog.MIMETIC_STAR_SLURRY.get()))
            return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().is(AstralItemCatalog.MIMETIC_STAR_SLURRY.get()))
            return InteractionHand.OFF_HAND;
        return null;
    }
}
