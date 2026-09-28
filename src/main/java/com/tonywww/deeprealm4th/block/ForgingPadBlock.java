package com.tonywww.deeprealm4th.block;

import com.tonywww.deeprealm4th.blockentity.ForgingPadBlockEntity;
import com.tonywww.deeprealm4th.platform.block.VersionedForgingPadBlock;
import com.tonywww.deeprealm4th.platform.menu.ForgingPadMenuOpener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Thin forge pad placed directly above any vanilla-tagged anvil. */
public final class ForgingPadBlock extends VersionedForgingPadBlock implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 2, 15);

    public ForgingPadBlock(Properties properties) { super(properties); }

    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
                                          CollisionContext context) { return SHAPE; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.ANVIL);
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForgingPadBlockEntity(pos, state);
    }
    @Override protected void open(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer server) ForgingPadMenuOpener.open(server, pos);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos,
                                   BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof ForgingPadBlockEntity pad) {
            Containers.dropContents(level, pos, pad);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
