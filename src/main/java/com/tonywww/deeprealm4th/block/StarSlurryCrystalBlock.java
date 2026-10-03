package com.tonywww.deeprealm4th.block;

import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;
import com.tonywww.deeprealm4th.platform.block.VersionedStarSlurryCrystalBlock;
import com.tonywww.deeprealm4th.platform.registry.AstralBlockRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;

/** Four growth stages share attachment behavior; only a mature cluster can fill a bottle. */
public final class StarSlurryCrystalBlock extends VersionedStarSlurryCrystalBlock {
    private final boolean mature;

    public StarSlurryCrystalBlock(int height, int offset, Properties properties) {
        super(height, offset, properties);
        this.mature = height == 7;
    }

    @Override
    protected boolean accepts(ItemStack held) { return mature && held.is(Items.GLASS_BOTTLE); }

    @Override
    protected void collect(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level.isClientSide) return;
        ItemStack held = player.getItemInHand(hand);
        if (!accepts(held) || !level.getBlockState(pos).is(AstralBlockRegistration.STAR_SLURRY_CLUSTER.get())) return;
        // Restore contained water instead of deleting it when the harvested crystal was waterlogged.
        level.setBlockAndUpdate(pos, state.getValue(WATERLOGGED)
                ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        ItemStack filled = new ItemStack(AstralItemCatalog.STAR_SLURRY.get());
        if (!player.getAbilities().instabuild) held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, filled);
        else if (!player.getInventory().add(filled)) Block.popResource(level, pos, filled);
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
    }
}
