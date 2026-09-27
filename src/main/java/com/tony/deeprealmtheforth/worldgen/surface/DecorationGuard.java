package com.tony.deeprealmtheforth.worldgen.surface;

import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import java.util.concurrent.atomic.LongAdder;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;

/** Scoped to our native decoration call, never to player actions or other dimensions. */
public final class DecorationGuard implements AutoCloseable {
    private static final ThreadLocal<DecorationGuard> ACTIVE = new ThreadLocal<>();
    private final DecorationGuard previous;
    private final TerrainProfile terrain;
    private final LongAdder rejected, writes;
    private final boolean structure;
    private final Map<LevelChunkSection, BlockPos> sections = new IdentityHashMap<>();
    public DecorationGuard(TerrainProfile terrain, LongAdder rejected, LongAdder writes, WorldGenLevel level, ChunkPos center) {
        this(terrain, rejected, writes, level, center, false);
    }
    public DecorationGuard(TerrainProfile terrain, LongAdder rejected, LongAdder writes, WorldGenLevel level, ChunkPos center, boolean structure) {
        this.terrain = terrain; this.rejected = rejected; this.writes = writes;
        this.structure = structure;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            var chunk = level.getChunk(center.x + dx, center.z + dz);
            var array = chunk.getSections();
            for (int i = 0; i < array.length; i++) sections.put(array[i],
                    new BlockPos((center.x + dx) * 16, chunk.getMinBuildHeight() + i * 16, (center.z + dz) * 16));
        }
        previous = ACTIVE.get(); ACTIVE.set(this);
    }
    public static boolean allowSection(LevelChunkSection section, int x, int y, int z, BlockState state) {
        DecorationGuard guard = ACTIVE.get();
        if (guard == null) return true;
        BlockPos origin = guard.sections.get(section);
        // An unknown target is outside this feature's writable generation neighborhood.
        if (origin == null) { guard.rejected.increment(); return false; }
        return allow(origin.offset(x, y, z), state);
    }
    public static boolean allow(BlockPos pos, BlockState state) {
        DecorationGuard guard = ACTIVE.get();
        if (guard == null) return true;
        boolean allowed = guard.test(pos, state);
        if (allowed) guard.writes.increment(); else guard.rejected.increment();
        return allowed;
    }
    private boolean test(BlockPos pos, BlockState state) {
        TerrainProfile.Column c = terrain.sample(pos.getX(), pos.getZ());
        int y = pos.getY();
        if (!c.land() || y < c.bottom() + 3 || !structure && y > c.surface() + 48) return false;
        if (c.shore() && y <= c.top()) return false;
        if (c.arm() == 1 || c.arm() == 5) {
            // Features and structures may still run, but must not dam a channel or
            // excavate its graded containment bank. Waterlogged plants remain legal.
            if (c.wet() && y > c.top() && y <= c.fluidLevel()
                    && state.getFluidState().isEmpty() && !state.is(net.minecraft.world.level.block.Blocks.ICE)) return false;
            if (structure && !c.wet() && y <= c.top() && !state.isSolidRender(EmptyBlockGetter.INSTANCE, pos)) {
                var river = terrain.hydrology().sample(pos.getX(), pos.getZ(), c.top());
                if (river.distance() < 9 && y <= river.containment() && y >= c.top() - 24) return false;
            }
        }
        // Structures may excavate rooms, but cannot turn a river/lava bank into a leak.
        // Unlike the sandstone ocean shore, these banks have ordinary biome materials.
        if (structure && y <= c.top() && !state.isSolidRender(EmptyBlockGetter.INSTANCE, pos)) {
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                var n = terrain.sample(pos.getX() + direction.getStepX(), pos.getZ() + direction.getStepZ());
                if (n.wet() && y >= n.top() - 2 && y <= n.fluidLevel()) return false;
            }
        }
        // Native lakes/springs must not open holes in floating ground or create uncontained fluids.
        if (!structure && state.isAir() && y <= c.top()) return false;
        if (!state.getFluidState().isEmpty()) {
            return c.wet() && y > c.top() && y <= c.fluidLevel()
                    && (state.getFluidState().is(FluidTags.WATER) == (c.fluid() == TerrainProfile.Fluid.WATER));
        }
        if (c.wet() && y <= c.top() && y >= c.top() - 2) return false;
        if (c.wet() && y > c.top() && y <= c.fluidLevel() && state.isAir()) return false;
        return true;
    }
    @Override public void close() { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
}
