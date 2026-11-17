
package com.thatonecreator13.osc.spitfire;

// PURPOSE: Fire four horizontal Bend Shot lanes from the corners of a vertical 7x7 formation.

import com.thatonecreator13.osc.Payload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.state.BlockState;

public final class SpitfireStrike {
    private static final int RANGE = 300;
    private static final int SPACING = 4;
    private static final int TNT_PER_POSITION = 5;
    private static final int TNT_FUSE = 1;
    private static final int FORMATION_OFFSET = 3;
    private static final float STOP_RESISTANCE = 1200.0F;

    private final ServerLevel level;
    private final BlockPos target;
    private final int directionX;
    private final int directionZ;

    public SpitfireStrike(
            ServerLevel level,
            BlockPos target,
            double lookX,
            double lookZ
    ) {
        this.level = level;
        this.target = target.immutable();

        // Snap the look direction to the closest cardinal direction.
        if (Math.abs(lookX) >= Math.abs(lookZ)) {
            directionX = lookX >= 0.0 ? 1 : -1;
            directionZ = 0;
        } else {
            directionX = 0;
            directionZ = lookZ >= 0.0 ? 1 : -1;
        }
    }

    public boolean tick() {
        int centerX = target.getX();
        int centerY = target.getY();
        int centerZ = target.getZ();

        // The four lanes occupy the corners of a vertical 7x7 formation.
        for (int verticalOffset : new int[]{-FORMATION_OFFSET, FORMATION_OFFSET}) {
            for (int sideOffset : new int[]{-FORMATION_OFFSET, FORMATION_OFFSET}) {
                fireLane(centerX, centerY + verticalOffset, centerZ, sideOffset);
            }
        }

        return true;
    }

    private void fireLane(int centerX, int y, int centerZ, int sideOffset) {
        for (int distance = 0; distance < RANGE; distance += SPACING) {
            int currentX = centerX + directionX * distance;
            int currentZ = centerZ + directionZ * distance;

            // Move sideways relative to the firing direction.
            if (directionX != 0) {
                currentZ += sideOffset;
            } else {
                currentX += sideOffset;
            }

            BlockPos checkPos = new BlockPos(currentX, y, currentZ);

            // Each corner lane stops independently at blocking blocks.
            if (isBlocking(checkPos)) {
                break;
            }

            for (int i = 0; i < TNT_PER_POSITION; i++) {
                double offset = (i - (TNT_PER_POSITION - 1) / 2.0) * 0.12;

                double spawnX = currentX + 0.5
                        + (directionZ != 0 ? offset : 0.0);
                double spawnY = y + 0.5;
                double spawnZ = currentZ + 0.5
                        + (directionX != 0 ? offset : 0.0);

                Entity entity = Payload.spawn(
                        level, spawnX, spawnY, spawnZ
                );

                if (!(entity instanceof PrimedTnt tnt)) {
                    continue;
                }

                tnt.setFuse(TNT_FUSE);
                tnt.setNoGravity(true);
                tnt.setDeltaMovement(0.0, 0.0, 0.0);
                tnt.hurtMarked = true;
            }
        }
    }

    private boolean isBlocking(BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            return false;
        }

        return state.getDestroySpeed(level, pos) < 0.0F
                || state.getBlock().getExplosionResistance()
                >= STOP_RESISTANCE;
    }
}
