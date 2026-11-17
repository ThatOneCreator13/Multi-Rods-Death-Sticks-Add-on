
// PURPOSE: Register and tick Grand Entrence attacks started by named fishing rods.

package com.thatonecreator13.osc.grand_entrence;

import com.thatonecreator13.osc.StrikeTargeting;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GrandEntrenceHandler {

    private static final List<GrandEntrenceStrike> ACTIVE_STRIKES =
            new ArrayList<>();

    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<GrandEntrenceStrike> iterator =
                    ACTIVE_STRIKES.iterator();

            while (iterator.hasNext()) {
                GrandEntrenceStrike strike = iterator.next();

                if (strike.tick()) {
                    iterator.remove();
                }
            }
        });
    }

    private GrandEntrenceHandler() {
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

        ACTIVE_STRIKES.add(
                new GrandEntrenceStrike(
                        level,
                        target,
                        look.x,
                        look.z
                )
        );
    }
}
