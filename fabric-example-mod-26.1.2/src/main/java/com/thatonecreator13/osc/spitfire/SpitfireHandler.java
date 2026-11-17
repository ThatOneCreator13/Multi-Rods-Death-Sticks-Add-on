
package com.thatonecreator13.osc.spitfire;

// PURPOSE: Find the targeted block and execute Spitfire on the server tick.

import com.thatonecreator13.osc.StrikeTargeting;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SpitfireHandler {
    private static final List<SpitfireStrike> ACTIVE_STRIKES = new ArrayList<>();

    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<SpitfireStrike> iterator = ACTIVE_STRIKES.iterator();

            while (iterator.hasNext()) {
                SpitfireStrike strike = iterator.next();

                if (strike.tick()) {
                    iterator.remove();
                }
            }
        });
    }

    private SpitfireHandler() {
    }

    public static void handle(ServerPlayer player) {
        BlockPos target = StrikeTargeting.findTarget(player);

        if (target == null) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 look = player.getLookAngle();

        ACTIVE_STRIKES.add(new SpitfireStrike(
                level,
                target,
                look.x,
                look.z
        ));
    }
}
