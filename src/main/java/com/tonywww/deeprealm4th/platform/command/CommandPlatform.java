package com.tonywww.deeprealm4th.platform.command;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

//? if forge {
import net.minecraftforge.common.util.FakePlayer;
//?} else {
/*import net.neoforged.neoforge.common.util.FakePlayer;
*///?}

/** Versioned primitives used by read-only verification commands. */
public final class CommandPlatform {
    private CommandPlatform() {}

    public static ServerPlayer fakePlayer(ServerLevel level, GameProfile profile) {
        return new FakePlayer(level, profile);
    }

    public static CompoundTag saveWithMetadata(BlockEntity entity, ServerLevel level) {
        //? if <1.21 {
        return entity.saveWithFullMetadata();
        //?} else {
        /*return entity.saveWithFullMetadata(level.registryAccess());
        *///?}
    }
}
