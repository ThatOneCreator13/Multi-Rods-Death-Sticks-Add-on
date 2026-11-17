
package com.thatonecreator13.osc.gate_grater;

// PURPOSE: Drop a stacked TNT payload, form it into a vertical disk,
// and launch it horizontally using one launcher TNT.

import com.thatonecreator13.osc.Payload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;

import java.util.ArrayList;
import java.util.List;

public class GateGraterStrike {
    private static final double BACK_OFFSET = 20.0;
    private static final double DROP_HEIGHT = 75.0;
    private static final double FALL_SPEED = -0.65;
    private static final double FORWARD_SPEED = 1.2;

    private static final double LAUNCHER_REAR_OFFSET = 1.5;

    private static final int PAYLOAD_TNT_COUNT = 96;
    private static final double DISK_RADIUS = 7.0;

    private static final int FALLING_FUSE = 200;
    private static final int ATTACK_FUSE = 30;

    private final ServerLevel level;
    private final BlockPos target;
    private final List<PrimedTnt> payload = new ArrayList<>();

    private final double directionX;
    private final double directionZ;
    private final double centerX;
    private final double centerZ;

    private double centerY;
    private PrimedTnt launcher;

    private boolean launched;
    private boolean finished;
    private int launchTicks;

    public GateGraterStrike(
            ServerLevel level,
            BlockPos target,
            double lookX,
            double lookZ
    ) {
        this.level = level;
        this.target = target;

        if (Math.abs(lookX) >= Math.abs(lookZ)) {
            directionX = lookX >= 0.0 ? 1.0 : -1.0;
            directionZ = 0.0;
        } else {
            directionX = 0.0;
            directionZ = lookZ >= 0.0 ? 1.0 : -1.0;
        }

        centerX = target.getX() + 0.5 - directionX * BACK_OFFSET;
        centerZ = target.getZ() + 0.5 - directionZ * BACK_OFFSET;

        int surfaceY = findHighestNonAirBlock(
                (int) Math.floor(centerX),
                (int) Math.floor(centerZ)
        );

        centerY = Math.min(
                surfaceY + DROP_HEIGHT,
                level.getMaxY() - 2.0
        );

        spawnStackedPayload();
        spawnLauncher();
    }

    private int findHighestNonAirBlock(int x, int z) {
        int topY = level.getMinY();

        for (int y = level.getMinY(); y < level.getMaxY(); y++) {
            BlockPos pos = new BlockPos(x, y, z);

            if (!level.getBlockState(pos).isAir()) {
                topY = y;
            }
        }

        return topY;
    }

    private void spawnStackedPayload() {
        for (int i = 0; i < PAYLOAD_TNT_COUNT; i++) {
            Entity entity = Payload.spawn(level, centerX, centerY, centerZ);

            if (!(entity instanceof PrimedTnt tnt)) {
                continue;
            }

            tnt.setFuse(FALLING_FUSE);
            tnt.setNoGravity(true);
            tnt.setDeltaMovement(0.0, FALL_SPEED, 0.0);

            payload.add(tnt);
        }
    }

    private void spawnLauncher() {
        double launcherX = centerX - directionX * LAUNCHER_REAR_OFFSET;
        double launcherZ = centerZ - directionZ * LAUNCHER_REAR_OFFSET;

        Entity entity = Payload.spawn(level, launcherX, centerY, launcherZ);

        if (entity instanceof PrimedTnt tnt) {
            launcher = tnt;
            launcher.setFuse(FALLING_FUSE);
            launcher.setNoGravity(true);
            launcher.setDeltaMovement(0.0, FALL_SPEED, 0.0);
        }
    }

    public boolean tick() {
        if (finished) {
            return true;
        }

        if (launched) {
            launchTicks++;

            for (PrimedTnt tnt : payload) {
                if (tnt.isAlive()) {
                    tnt.setNoGravity(true);
                    tnt.setDeltaMovement(
                            directionX * FORWARD_SPEED,
                            0.0,
                            directionZ * FORWARD_SPEED
                    );
                }
            }

            if (launchTicks >= ATTACK_FUSE) {
                finished = true;
                return true;
            }

            return false;
        }

        if (payload.isEmpty() || launcher == null || !launcher.isAlive()) {
            finished = true;
            return true;
        }

        centerY += FALL_SPEED;

        for (PrimedTnt tnt : payload) {
            if (tnt.isAlive()) {
                tnt.setNoGravity(true);
                tnt.setPos(centerX, centerY, centerZ);
                tnt.setDeltaMovement(0.0, FALL_SPEED, 0.0);
            }
        }

        launcher.setNoGravity(true);
        launcher.setDeltaMovement(0.0, FALL_SPEED, 0.0);

        if (centerY <= target.getY() + 1.0) {
            launchPayload();
        }

        return false;
    }

    private void launchPayload() {
        if (launched) {
            return;
        }

        launched = true;
        launchTicks = 0;

        for (int i = 0; i < payload.size(); i++) {
            PrimedTnt tnt = payload.get(i);

            double radius = DISK_RADIUS
                    * Math.sqrt((i + 0.5) / payload.size());

            double angle = i * Math.PI * (3.0 - Math.sqrt(5.0));

            double sidewaysOffset = Math.cos(angle) * radius;
            double verticalOffset = Math.sin(angle) * radius;

            double diskX = centerX + directionZ * sidewaysOffset;
            double diskY = centerY + verticalOffset;
            double diskZ = centerZ - directionX * sidewaysOffset;

            if (!tnt.isAlive()) {
                continue;
            }

            tnt.setPos(diskX, diskY, diskZ);
            tnt.setNoGravity(true);
            tnt.setFuse(ATTACK_FUSE);
            tnt.setDeltaMovement(
                    directionX * FORWARD_SPEED,
                    0.0,
                    directionZ * FORWARD_SPEED
            );
        }

        if (launcher != null && launcher.isAlive()) {
            launcher.setNoGravity(true);
            launcher.setDeltaMovement(
                    directionX * FORWARD_SPEED,
                    0.0,
                    directionZ * FORWARD_SPEED
            );
            launcher.setFuse(0);
        }
    }
}
