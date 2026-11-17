
// PURPOSE: Launch eight Gate Graters in sequence, each three blocks farther forward.

package com.thatonecreator13.osc.grand_entrence;

import com.thatonecreator13.osc.gate_grater.GateGraterStrike;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GrandEntrenceStrike {

    private static final int TOTAL_GRATERS = 8;
    private static final int TICKS_BETWEEN_GRATERS = 60;
    private static final int FORWARD_SPACING = 3;

    private final ServerLevel level;
    private final BlockPos target;
    private final double lookX;
    private final double lookZ;
    private final int directionX;
    private final int directionZ;

    private final List<GateGraterStrike> activeGraters = new ArrayList<>();

    private int elapsedTicks;
    private int gratersLaunched;

    public GrandEntrenceStrike(
            ServerLevel level,
            BlockPos target,
            double lookX,
            double lookZ
    ) {
        this.level = level;
        this.target = target;
        this.lookX = lookX;
        this.lookZ = lookZ;

        if (Math.abs(lookX) >= Math.abs(lookZ)) {
            directionX = lookX >= 0.0 ? 1 : -1;
            directionZ = 0;
        } else {
            directionX = 0;
            directionZ = lookZ >= 0.0 ? 1 : -1;
        }
    }

    // Returns true when all eight graters have finished.
    public boolean tick() {
        if (gratersLaunched < TOTAL_GRATERS
                && elapsedTicks == gratersLaunched * TICKS_BETWEEN_GRATERS) {
            launchNextGrater();
        }

        Iterator<GateGraterStrike> iterator = activeGraters.iterator();

        while (iterator.hasNext()) {
            GateGraterStrike grater = iterator.next();

            if (grater.tick()) {
                iterator.remove();
            }
        }

        elapsedTicks++;

        return gratersLaunched >= TOTAL_GRATERS && activeGraters.isEmpty();
    }

    private void launchNextGrater() {
        int forwardOffset = gratersLaunched * FORWARD_SPACING;

        BlockPos graterTarget = target.offset(
                directionX * forwardOffset,
                0,
                directionZ * forwardOffset
        );

        activeGraters.add(
                new GateGraterStrike(level, graterTarget, lookX, lookZ)
        );

        gratersLaunched++;
    }
}
