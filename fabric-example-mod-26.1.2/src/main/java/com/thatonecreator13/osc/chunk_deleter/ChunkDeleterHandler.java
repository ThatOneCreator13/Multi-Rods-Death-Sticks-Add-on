
package com.thatonecreator13.osc.chunk_deleter;

// PURPOSE: Find the player's target and activate Chunk Deleter on the server.

import com.thatonecreator13.osc.StrikeTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ChunkDeleterHandler {
    private ChunkDeleterHandler() {
    }

    public static void handle(ServerPlayer player) {
        BlockPos target = StrikeTargeting.findTarget(player);

        if (target == null) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        new ChunkDeleterStrike(level, target).tick();
    }
}
