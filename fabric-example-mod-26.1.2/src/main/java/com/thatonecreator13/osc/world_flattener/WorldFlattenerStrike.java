
package com.thatonecreator13.osc.world_flattener;

// PURPOSE: Schedule eight Nuke strikes at the same X/Z position,
// 60 ticks apart, lowering each successive Nuke by three blocks.

import com.thatonecreator13.osc.nuke.NukeStrike;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class WorldFlattenerStrike {
    private static final int NUKE_COUNT = 8;
    private static final int TICKS_BETWEEN_NUKES = 60;
    private static final int BLOCKS_LOWER_PER_NUKE = 3;

    private final ServerLevel level;
    private final BlockPos target;
    private final List<NukeStrike> activeNukes = new ArrayList<>();

    private int elapsedTicks;
    private int nextNukeIndex;

    public WorldFlattenerStrike(ServerLevel level, BlockPos target) {
        this.level = level;
        this.target = target.immutable();
    }

    public boolean tick() {
        // Launch a new Nuke every 60 ticks, beginning immediately.
        if (nextNukeIndex < NUKE_COUNT
                && elapsedTicks >= nextNukeIndex * TICKS_BETWEEN_NUKES) {

            int verticalOffset = nextNukeIndex * BLOCKS_LOWER_PER_NUKE;

            activeNukes.add(
                    new NukeStrike(level, target, verticalOffset)
            );

            nextNukeIndex++;
        }

        // Tick every Nuke that has not reached its launch point yet.
        Iterator<NukeStrike> iterator = activeNukes.iterator();

        while (iterator.hasNext()) {
            NukeStrike nuke = iterator.next();

            if (nuke.tick()) {
                iterator.remove();
            }
        }

        elapsedTicks++;

        // Finish scheduling after all eight Nukes have been launched.
        return nextNukeIndex >= NUKE_COUNT && activeNukes.isEmpty();
    }
}
