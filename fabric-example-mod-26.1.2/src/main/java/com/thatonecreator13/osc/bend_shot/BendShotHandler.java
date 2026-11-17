
package com.thatonecreator13.osc.bend_shot;

// PURPOSE: Target and fire the Bend Shot horizontally.

import com.thatonecreator13.osc.StrikeTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class BendShotHandler {
    private BendShotHandler() {
    }

    public static void handle(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos target = StrikeTargeting.findTarget(player);

        if (target == null) {
            return;
        }

        new BendShotStrike(level, target, player.getLookAngle().x,
                player.getLookAngle().z).tick();
    }
}
