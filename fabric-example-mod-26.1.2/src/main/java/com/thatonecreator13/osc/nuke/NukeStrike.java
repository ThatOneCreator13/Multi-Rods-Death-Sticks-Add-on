
package com.thatonecreator13.osc.nuke;

// PURPOSE: Launch a dense, expanding downward TNT ring formation,
// optionally lowered for advanced shot types without changing normal Nuke.

import com.thatonecreator13.osc.Payload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;

import java.util.ArrayList;
import java.util.List;

public final class NukeStrike {
    private static final int HEIGHT_ABOVE_SURFACE = 75;
    private static final int TRIGGER_HEIGHT_ABOVE_GROUND = 20;

    private static final int LAUNCHER_FUSE = 200;
    private static final int SPREAD_FUSE = 30;

    private static final double LAUNCHER_HEIGHT_OFFSET = 3.0;
    private static final double FALL_SPEED = -0.65;
    private static final double SPREAD_DOWNWARD_SPEED = -0.8;

    private static final int VOID_DAMAGE_BUFFER = 64;

    private static final double[] RING_RADII = {
            6.0, 11.0, 16.0, 21.0, 26.0,
            31.0, 36.0, 41.0, 46.0, 51.0
    };

    // 480 TNT total, with progressively denser outer rings.
    private static final int[] RING_COUNTS = {
            12, 20, 28, 36, 44,
            52, 60, 68, 76, 84
    };

    private final ServerLevel level;
    private final int targetX;
    private final int targetZ;
    private final int groundY;

    private final List<PrimedTnt> payload = new ArrayList<>();

    private PrimedTnt launcher;
    private boolean finished;

    // Normal Nuke: preserve its original behavior.
    public NukeStrike(ServerLevel level, BlockPos target) {
        this(level, target, 0);
    }

    // Advanced shots can lower the Nuke by a chosen number of blocks.
    public NukeStrike(ServerLevel level, BlockPos target, int verticalOffset) {
        this.level = level;
        this.targetX = target.getX();
        this.targetZ = target.getZ();
        this.groundY = findHighestNonAirBlock(targetX, targetZ) - verticalOffset;

        double centerX = targetX + 0.5;
        double centerZ = targetZ + 0.5;
        double payloadY = groundY + HEIGHT_ABOVE_SURFACE;

        // Keep the launch TNT above the main payload.
        Entity launcherEntity = Payload.spawn(
                level,
                centerX,
                payloadY + LAUNCHER_HEIGHT_OFFSET,
                centerZ
        );

        if (launcherEntity instanceof PrimedTnt tnt) {
            launcher = tnt;
            launcher.setFuse(LAUNCHER_FUSE);
            launcher.setDeltaMovement(0.0, FALL_SPEED, 0.0);
            launcher.hurtMarked = true;
        }

        // Spawn all main payload TNT compactly before the launch.
        int totalPayload = 0;

        for (int count : RING_COUNTS) {
            totalPayload += count;
        }

        for (int i = 0; i < totalPayload; i++) {
            Entity entity = Payload.spawn(
                    level,
                    centerX,
                    payloadY,
                    centerZ
            );

            if (entity instanceof PrimedTnt tnt) {
                tnt.setFuse(LAUNCHER_FUSE);
                tnt.setDeltaMovement(0.0, FALL_SPEED, 0.0);
                tnt.hurtMarked = true;
                payload.add(tnt);
            }
        }
    }

    private int findHighestNonAirBlock(int x, int z) {
        int y = level.getMaxY() - 1;

        while (y > level.getMinY()
                && level.getBlockState(new BlockPos(x, y, z)).isAir()) {
            y--;
        }

        return y;
    }

    public boolean tick() {
        if (finished) {
            return true;
        }

        if (launcher == null || !launcher.isAlive()) {
            finished = true;
            return true;
        }

        if (launcher.getY() <= groundY + TRIGGER_HEIGHT_ABOVE_GROUND) {
            launchPayload();
            finished = true;
            return true;
        }

        if (launcher.getY() < level.getMinY() - VOID_DAMAGE_BUFFER) {
            finished = true;
            return true;
        }

        return false;
    }

    private void launchPayload() {
        // Explode the launcher above the main payload to help drive
        // the TNT outward and downward.
        launcher.setDeltaMovement(0.0, -1.0, 0.0);
        launcher.setFuse(0);
        launcher.hurtMarked = true;

        int payloadIndex = 0;

        // Expand each ring independently, preserving its circular shape.
        for (int ring = 0; ring < RING_RADII.length; ring++) {
            double radius = RING_RADII[ring];
            int count = RING_COUNTS[ring];

            // Approximate horizontal speed needed to reach the radius
            // over the spread fuse, accounting for horizontal drag.
            double dragSum =
                    (1.0 - Math.pow(0.98, SPREAD_FUSE)) / (1.0 - 0.98);

            double horizontalSpeed = radius / dragSum;

            // Rotate each ring slightly to avoid lining up all TNT
            // along the same radial directions.
            double angleOffset = ring * 0.19;

            for (int i = 0; i < count; i++) {
                if (payloadIndex >= payload.size()) {
                    return;
                }

                PrimedTnt tnt = payload.get(payloadIndex++);

                if (!tnt.isAlive()) {
                    continue;
                }

                double angle =
                        (Math.PI * 2.0 * i / count) + angleOffset;

                double velocityX = Math.cos(angle) * horizontalSpeed;
                double velocityZ = Math.sin(angle) * horizontalSpeed;

                tnt.setFuse(SPREAD_FUSE);
                tnt.setDeltaMovement(
                        velocityX,
                        SPREAD_DOWNWARD_SPEED,
                        velocityZ
                );
                tnt.hurtMarked = true;
            }
        }
    }
}
