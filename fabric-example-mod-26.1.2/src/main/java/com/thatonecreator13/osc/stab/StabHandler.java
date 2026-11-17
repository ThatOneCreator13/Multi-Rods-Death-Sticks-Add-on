
package com.thatonecreator13.osc.stab;

// PURPOSE: Delay Stab by one second before firing.

import com.thatonecreator13.osc.StrikeTargeting;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class StabHandler {

    private static final int FIRE_DELAY_TICKS = 20;

    private static final List<PendingStab> PENDING = new ArrayList<>();

    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<PendingStab> iterator = PENDING.iterator();

            while (iterator.hasNext()) {
                PendingStab pending = iterator.next();

                pending.ticksRemaining--;

                if (pending.ticksRemaining <= 0) {
                    new StabStrike(pending.level, pending.target).tick();
                    iterator.remove();
                }
            }
        });
    }

    private StabHandler() {
    }

    public static void handle(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos target = StrikeTargeting.findTarget(player);

        if (target == null) {
            return;
        }

        PENDING.add(new PendingStab(level, target));
    }

    private static final class PendingStab {
        private final ServerLevel level;
        private final BlockPos target;
        private int ticksRemaining = FIRE_DELAY_TICKS;

        private PendingStab(ServerLevel level, BlockPos target) {
            this.level = level;
            this.target = target;
        }
    }
}
