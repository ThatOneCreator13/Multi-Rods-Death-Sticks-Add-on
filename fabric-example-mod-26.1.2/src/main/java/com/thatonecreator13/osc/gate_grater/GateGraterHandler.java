
package com.thatonecreator13.osc.gate_grater;

// PURPOSE: Target and manage active Gate Grater strikes.

import com.thatonecreator13.osc.StrikeTargeting;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GateGraterHandler {
    private static final List<GateGraterStrike> ACTIVE_STRIKES =
            new ArrayList<>();

    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<GateGraterStrike> iterator =
                    ACTIVE_STRIKES.iterator();

            while (iterator.hasNext()) {
                GateGraterStrike strike = iterator.next();

                if (strike.tick()) {
                    iterator.remove();
                }
            }
        });
    }

    private GateGraterHandler() {
    }

    public static void handle(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos target = StrikeTargeting.findTarget(player);

        if (target == null) {
            return;
        }

        double lookX = player.getLookAngle().x;
        double lookZ = player.getLookAngle().z;

        ACTIVE_STRIKES.add(
                new GateGraterStrike(level, target, lookX, lookZ)
        );
    }
}
