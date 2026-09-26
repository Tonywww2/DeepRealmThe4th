package com.tony.deeprealmtheforth.platform.commands;

import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;

/** Exact allowlist of this project's disposable saves, never arbitrary player worlds. */
final class VerificationWorlds {
    private static final Set<String> NAMES=Set.of("verification-world-vortex-v8-shores", "verification-world-vortex-v9-hydrology",
            "verification-world-vortex-v10-edges", "verification-world-vortex-v11-marine",
            "verification-world-vortex-v11-compat");
    private VerificationWorlds() {}
    static boolean allowed(CommandSourceStack source,ServerLevel level) {
        return NAMES.contains(source.getServer().getWorldData().getLevelName()) && level.getSeed()==42
                && source.getServer().getPlayerList().getPlayers().isEmpty();
    }
}
