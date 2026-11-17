
package com.thatonecreator13.osc.nuke;

// PURPOSE: Target and schedule Nuke shots.

import com.thatonecreator13.osc.StrikeTargeting;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NukeHandler {

    private static final List<NukeStrike> ACTIVE_NUKES =
            new ArrayList<>();

    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<NukeStrike> iterator = ACTIVE_NUKES.iterator();

            while (iterator.hasNext()) {
                NukeStrike strike = iterator.next();

                if (strike.tick()) {
                    iterator.remove();
                }
            }
        });
    }

    private NukeHandler() {
    }

    public static void handle(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // Reuse the same 60-block target finder as Stab.
        BlockPos target = StrikeTargeting.findTarget(player);

        if (target == null) {
            return;
        }

        ACTIVE_NUKES.add(new NukeStrike(level, target));
    }
}
