package com.tonywww.deeprealm4th.block;

import com.tonywww.deeprealm4th.platform.registry.AstralBlockRegistration;

import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;
import com.tonywww.deeprealm4th.platform.block.VersionedStarSlurrySeepBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** One nonrenewable Fourth Layer seep fills one ordinary glass bottle. */
public final class StarSlurrySeepBlock extends VersionedStarSlurrySeepBlock {
    public StarSlurrySeepBlock(Properties properties) { super(properties); }

    @Override
    protected boolean accepts(ItemStack held) { return held.is(Items.GLASS_BOTTLE); }

    @Override
    protected void collect(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level.isClientSide) return;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.GLASS_BOTTLE) || !level.getBlockState(pos).getBlock().equals(
                com.tonywww.deeprealm4th.platform.registry.AstralBlockRegistration.STAR_SLURRY_SEEP.get())) return;
        level.removeBlock(pos, false);
        ItemStack filled = new ItemStack(AstralItemCatalog.STAR_SLURRY.get());
        if (!player.getAbilities().instabuild) held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, filled);
        else if (!player.getInventory().add(filled)) Block.popResource(level, pos, filled);
    }
}
