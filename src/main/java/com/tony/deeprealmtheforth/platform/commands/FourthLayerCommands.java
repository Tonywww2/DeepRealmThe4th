package com.tony.deeprealmtheforth.platform.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tony.deeprealmtheforth.platform.PlatformIds;
import com.tony.deeprealmtheforth.travel.LandingSearch;
import com.tony.deeprealmtheforth.worldgen.SpiralChunkGenerator;
import com.tony.deeprealmtheforth.worldgen.terrain.TerrainProfile;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class FourthLayerCommands {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, PlatformIds.id("fourth_layer"));
    private static final String RETURN_KEY = "deeprealm_4th_return";
    private static final String LEGACY_RETURN_KEY = "deep_realm_the_forth_return";

    private FourthLayerCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fourthlayer").requires(s -> s.hasPermission(2))
                .then(Commands.literal("enter").executes(c -> enter(c.getSource(), 5, 512))
                        .then(Commands.argument("arm", IntegerArgumentType.integer(1, 8))
                                .executes(c -> enter(c.getSource(), IntegerArgumentType.getInteger(c, "arm"), 512))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(128, 8192))
                                        .executes(c -> enter(c.getSource(), IntegerArgumentType.getInteger(c, "arm"),
                                                IntegerArgumentType.getInteger(c, "radius"))))))
                .then(Commands.literal("return").executes(c -> leave(c.getSource())))
                .then(Commands.literal("updatewater").executes(c -> updateWater(c.getSource(), 1))
                        .then(Commands.argument("chunkRadius", IntegerArgumentType.integer(0, 2))
                                .executes(c -> updateWater(c.getSource(), IntegerArgumentType.getInteger(c, "chunkRadius")))))
                .then(Commands.literal("inspect").executes(c -> inspect(c.getSource(),
                                (int) Math.floor(c.getSource().getPosition().x), (int) Math.floor(c.getSource().getPosition().z)))
                        .then(Commands.argument("x", IntegerArgumentType.integer(-29999900, 29999900))
                                .then(Commands.argument("z", IntegerArgumentType.integer(-29999900, 29999900))
                                        .executes(c -> inspect(c.getSource(), IntegerArgumentType.getInteger(c, "x"),
                                                IntegerArgumentType.getInteger(c, "z"))))))
                .then(Commands.literal("verify").executes(c -> verify(c.getSource())))
                .then(Commands.literal("verifyhydrology")
                        .then(Commands.argument("phase", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(c -> HydrologyVerification.run(c.getSource(), level(c.getSource()), generator(level(c.getSource())),
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(c, "phase")))))
                .then(Commands.literal("verifyclimate").executes(c -> ClimateVerification.run(c.getSource(), level(c.getSource()), generator(level(c.getSource())))))
                .then(Commands.literal("verifybiomes").executes(c -> BiomeVerification.run(c.getSource(), level(c.getSource()), generator(level(c.getSource())))))
                .then(Commands.literal("verifycompat").executes(c -> BiomeCompatVerification.run(c.getSource(), level(c.getSource()), generator(level(c.getSource())))))
                .then(Commands.literal("verifystructures")
                        .then(Commands.argument("structure", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(c -> StructureVerification.run(c.getSource(), level(c.getSource()), generator(level(c.getSource())),
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(c, "structure")))))
                .then(Commands.literal("ecology").executes(c -> {
                    String report = generator(level(c.getSource())).decoration().report();
                    c.getSource().sendSuccess(() -> Component.literal(report), false); return 1;
                }))
                .then(Commands.literal("verifyecology").executes(c -> EcologyVerification.run(c.getSource(), level(c.getSource()), generator(level(c.getSource()))))));
    }

    private static ServerLevel level(CommandSourceStack source) throws CommandSyntaxException {
        ServerLevel level = source.getServer().getLevel(DIMENSION);
        if (level == null) throw error("深境四层未加载，请检查数据包和服务器日志。");
        return level;
    }

    /** Opt-in repair of existing river water; no terrain writes or implicit chunk loads. */
    private static int updateWater(CommandSourceStack source, int radius) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        if (!level.dimension().equals(DIMENSION)) throw error("请在深境四层的河流附近执行补更新。");
        SpiralChunkGenerator generator = generator(level);
        if (generator.terrain().generationVersion() < 3) throw error("补更新仅支持生成版本 3 及以上的主干支流水系。");
        int scheduled = 0, skipped = 0;
        var center = player.chunkPosition();
        for (int cx = center.x-radius; cx <= center.x+radius; cx++) for (int cz = center.z-radius; cz <= center.z+radius; cz++) {
            var chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) { skipped++; continue; }
            for (int x = cx*16; x < cx*16+16; x++) for (int z = cz*16; z < cz*16+16; z++) {
                var c = generator.terrain().sample(x, z);
                if (!c.wet() || c.fluid() != TerrainProfile.Fluid.WATER || (c.arm()!=1 && c.arm()!=5)) continue;
                for (int y = c.top()+1; y <= c.fluidLevel()+1; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    var state = chunk.getBlockState(pos);
                    if (!state.is(Blocks.WATER)) continue;
                    var fluid = state.getFluidState().getType();
                    if (!level.getFluidTicks().hasScheduledTick(pos, fluid)) {
                        level.scheduleTick(pos, fluid, 5 + ((x*31+z*17) & 31));
                        scheduled++;
                    }
                }
            }
        }
        int count=scheduled, missing=skipped;
        source.sendSuccess(() -> Component.literal("已安排 "+count+" 个河水更新；跳过 "+missing
                +" 个未加载区块。未重写地形；平坦水面可能按原版规则保持静止。"), false);
        return scheduled;
    }

    private static SpiralChunkGenerator generator(ServerLevel level) throws CommandSyntaxException {
        if (level.getChunkSource().getGenerator() instanceof SpiralChunkGenerator spiral) return spiral;
        throw error("此维度未使用螺旋生成器。");
    }

    private static int enter(CommandSourceStack source, int arm, int radius) throws CommandSyntaxException {
        if ((arm & 1) == 0) throw error("2、4、6、8 股是虚空；请选择 1、3、5、7。");
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel target = level(source);
        double[] center = generator(target).terrain().layout().armCenter(arm, radius);
        BlockPos landing = findSurface(target, (int) Math.floor(center[0]), (int) Math.floor(center[1]), 32);
        if (landing == null) throw error("此位置附近没有安全陆地，请更换半径。");
        CompoundTag origin = new CompoundTag();
        origin.putString("dimension", player.level().dimension().location().toString());
        origin.putDouble("x", player.getX()); origin.putDouble("y", player.getY()); origin.putDouble("z", player.getZ());
        origin.putFloat("yaw", player.getYRot()); origin.putFloat("pitch", player.getXRot());
        boolean remember = !player.level().dimension().equals(DIMENSION);
        if (!teleport(player, target, landing, player.getYRot(), player.getXRot())) throw error("传送被取消。");
        if (remember) player.getPersistentData().put(RETURN_KEY, origin);
        source.sendSuccess(() -> Component.literal("已进入深境四层第 " + arm + " 股，落点 " + landing.toShortString()), false);
        return 1;
    }

    private static int leave(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!player.level().dimension().equals(DIMENSION)) throw error("你当前不在深境四层。");
        CompoundTag savedData = player.getPersistentData();
        CompoundTag origin = savedData.contains(RETURN_KEY)
                ? savedData.getCompound(RETURN_KEY)
                : savedData.getCompound(LEGACY_RETURN_KEY);
        ServerLevel target = source.getServer().overworld();
        BlockPos preferred = target.getSharedSpawnPos();
        float yaw = player.getYRot(), pitch = 0;
        if (origin.contains("dimension")) {
            ServerLevel saved = source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                    PlatformIds.parse(origin.getString("dimension"))));
            if (saved != null && !saved.dimension().equals(DIMENSION)) {
                target = saved;
                preferred = BlockPos.containing(origin.getDouble("x"), origin.getDouble("y"), origin.getDouble("z"));
                yaw = origin.getFloat("yaw"); pitch = origin.getFloat("pitch");
            }
        }
        BlockPos landing = safe(target, preferred) ? preferred : findSurface(target, preferred.getX(), preferred.getZ(), 32);
        if (landing == null) throw error("原位置被占用，附近也没有安全落点；返回记录已保留。");
        if (!teleport(player, target, landing, yaw, pitch)) throw error("传送被取消，返回记录已保留。");
        savedData.remove(RETURN_KEY);
        savedData.remove(LEGACY_RETURN_KEY);
        source.sendSuccess(() -> Component.literal("已返回，落点 " + landing.toShortString()), false);
        return 1;
    }

    private static boolean teleport(ServerPlayer player, ServerLevel target, BlockPos pos, float yaw, float pitch) {
        boolean success = player.teleportTo(target, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), yaw, pitch);
        if (success) { player.setDeltaMovement(Vec3.ZERO); player.resetFallDistance(); }
        return success;
    }

    private static BlockPos findSurface(ServerLevel level, int x, int z, int radius) {
        return LandingSearch.find(x, z, radius, (candidateX, candidateZ) -> {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidateX, candidateZ);
            BlockPos pos = new BlockPos(candidateX, y, candidateZ);
            return safe(level, pos) ? pos : null;
        });
    }

    private static boolean safe(ServerLevel level, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos) || pos.getY() <= level.getMinBuildHeight()
                || pos.getY() + 1 >= level.getMaxBuildHeight()) return false;
        var floor = level.getBlockState(pos.below());
        return floor.isFaceSturdy(level, pos.below(), Direction.UP) && floor.getFluidState().isEmpty()
                && !floor.is(Blocks.MAGMA_BLOCK) && !floor.is(Blocks.CAMPFIRE) && !floor.is(Blocks.SOUL_CAMPFIRE)
                && !floor.is(Blocks.CACTUS) && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    private static int inspect(CommandSourceStack source, int x, int z) throws CommandSyntaxException {
        SpiralChunkGenerator generator = generator(level(source));
        TerrainProfile.Column column = generator.terrain().sample(x, z);
        String biome = generator.spiralBiomes().biomeAt(x, z).unwrapKey().orElseThrow().location().toString();
        source.sendSuccess(() -> Component.literal("Fourth layer v" + generator.terrain().generationVersion() + " [" + x + ", " + z + "] arm=" + column.arm()
                + " theme=" + column.theme() + " top=" + column.top() + " bottom=" + column.bottom()
                + " fluid=" + column.fluid() + ":" + column.fluidLevel() + " biome=" + biome), false);
        return 1;
    }

    private static int verify(CommandSourceStack source) throws CommandSyntaxException {
        ServerLevel level = level(source);
        SpiralChunkGenerator generator = generator(level);
        int checked = 0;
        long fingerprint = 1;
        for (int radius : new int[]{64, 256, 512}) {
            for (int arm = 1; arm <= 8; arm++) {
                double[] center = generator.terrain().layout().armCenter(arm, radius);
                int x = (int) Math.floor(center[0]), z = (int) Math.floor(center[1]);
                TerrainProfile.Column c = generator.terrain().sample(x, z);
                level.getChunk(x >> 4, z >> 4);
                var predicted = generator.getBaseColumn(x, z, level, level.getChunkSource().randomState());
                for (int y = level.getMinBuildHeight(); y <= c.surface(); y++) {
                    var actual = level.getBlockState(new BlockPos(x, y, z));
                    var expected = generator.terrainBlock(c, y, x, z);
                    if (!predicted.getBlock(y).equals(expected)) throw error("VERIFY FAILED: base-column query disagrees with terrain model");
                    // Native ores/substrates legitimately replace material; void, shell and bottom remain exact.
                    if ((!c.land() || y < c.bottom() + 3 || c.shore()) && !actual.equals(expected))
                        throw error("VERIFY FAILED protected terrain at " + x + "," + y + "," + z + " expected=" + expected + " actual=" + actual);
                    fingerprint = fingerprint * 31 + expected.toString().hashCode();
                }
                if (!c.land()) {
                    for (int y = c.surface() + 1; y < level.getMaxBuildHeight(); y++) {
                        if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) throw error("VERIFY FAILED: decorated void column");
                    }
                }
                checked++;
            }
        }
        int count = checked;
        int wallColumns = 0;
        for (int x = -1024; x <= 1024 && wallColumns < 24; x += 7) {
            for (int z = -1024; z <= 1024 && wallColumns < 24; z += 7) {
                var c = generator.terrain().sample(x, z);
                if (!c.shore()) continue;
                level.getChunk(x >> 4, z >> 4);
                for (int y = c.bottom(); y <= c.top(); y++) {
                    var expected = y == c.top() ? Blocks.SAND : Blocks.SANDSTONE;
                    if (!level.getBlockState(new BlockPos(x, y, z)).is(expected)) throw error("VERIFY FAILED: seawall changed at " + x + "," + y + "," + z);
                }
                wallColumns++;
            }
        }
        if (wallColumns < 24) throw error("VERIFY FAILED: missing seawall probes");
        String hash = Long.toUnsignedString(fingerprint, 16);
        source.sendSuccess(() -> Component.literal("FOURTH_LAYER_VERIFY_OK columns=" + count + " fingerprint=" + hash
                + " seawallColumns=24"
                + " possibleBiomes=" + generator.getBiomeSource().possibleBiomes().size()
                + " landFraction=" + generator.terrain().layout().parameters().landFraction()
                + " twist=" + generator.terrain().layout().parameters().twist()), true);
        return checked;
    }

    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }
}
