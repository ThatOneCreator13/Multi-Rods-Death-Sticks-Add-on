
package com.thatonecreator13.osc.world_flattener;

// PURPOSE: Register and manage active World Flattener sequences.

import com.thatonecreator13.osc.StrikeTargeting;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class WorldFlattenerHandler {
    private static final List<WorldFlattenerStrike> ACTIVE_STRIKES =
            new ArrayList<>();

    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<WorldFlattenerStrike> iterator =
                    ACTIVE_STRIKES.iterator();

            while (iterator.hasNext()) {
                WorldFlattenerStrike strike = iterator.next();

                if (strike.tick()) {
                    iterator.remove();
                }
            }
        });
    }

    private WorldFlattenerHandler() {
    }

    public static void handle(ServerPlayer player) {
        BlockPos target = StrikeTargeting.findTarget(player);

        if (target == null) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ACTIVE_STRIKES.add(new WorldFlattenerStrike(level, target));
    }
}
