
package com.thatonecreator13.osc.bend_shot;

// PURPOSE: Create a 300-block horizontal TNT stab that stops at unbreakable blocks.

import com.thatonecreator13.osc.Payload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.state.BlockState;

public final class BendShotStrike {
    private static final int RANGE = 300;
    private static final int SPACING = 4;
    private static final int TNT_PER_POSITION = 5;
    private static final int TNT_FUSE = 1;
    private static final float STOP_RESISTANCE = 1200.0F;

    private final ServerLevel level;
    private final BlockPos target;
    private final int directionX;
    private final int directionZ;

    public BendShotStrike(
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
        int x = target.getX();
        int y = target.getY();
        int z = target.getZ();

        for (int distance = 0; distance < RANGE; distance += SPACING) {
            int currentX = x + directionX * distance;
            int currentZ = z + directionZ * distance;

            // Stop before any unbreakable or extremely blast-resistant block.
            if (isBlocking(new BlockPos(currentX, y, currentZ))) {
                break;
            }

            for (int i = 0; i < TNT_PER_POSITION; i++) {
                // Keep all TNT centered on the horizontal strike line.
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

        return true;
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
