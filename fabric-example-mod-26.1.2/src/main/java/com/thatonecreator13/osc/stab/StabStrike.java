
package com.thatonecreator13.osc.stab;

// PURPOSE: Start the instant vertical Stab 150 blocks above the target column's highest surface,
// with a 40-TNT stack immediately above the final unbreakable or highly blast-resistant block.

import com.thatonecreator13.osc.Payload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.state.BlockState;

public final class StabStrike {
    private static final int START_ABOVE_SURFACE = 150;
    private static final int VERTICAL_SPACING = 3;
    private static final int TNT_PER_LAYER = 7;
    private static final int BOTTOM_STACK_COUNT = 40;
    private static final int TNT_FUSE = 1;
    private static final float STOP_RESISTANCE = 1200.0F;

    private final ServerLevel level;
    private final BlockPos target;

    public StabStrike(ServerLevel level, BlockPos target) {
        this.level = level;
        this.target = target.immutable();
    }

    public boolean tick() {
        int x = target.getX();
        int z = target.getZ();

        // Find the highest non-air block in the column being targeted.
        int highestY = level.getMaxY() - 1;

        while (highestY > level.getMinY()
                && level.getBlockState(
                new BlockPos(x, highestY, z)
        ).isAir()) {
            highestY--;
        }

        int startY = Math.min(
                highestY + START_ABOVE_SURFACE,
                level.getMaxY() - 1
        );

        int stopY = level.getMinY();

        // Find the first unbreakable or extremely blast-resistant block.
        for (int y = startY; y >= level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);

            if (!state.isAir()
                    && (state.getDestroySpeed(level, pos) < 0.0F
                    || state.getBlock().getExplosionResistance()
                    >= STOP_RESISTANCE)) {
                stopY = y;
                break;
            }
        }

        // Detonate the normal stationary vertical column of TNT.
        for (int y = startY; y > stopY; y -= VERTICAL_SPACING) {
            for (int i = 0; i < TNT_PER_LAYER; i++) {
                spawnTnt(
                        x + 0.5,
                        y + i * 0.12,
                        z + 0.5
                );
            }
        }

        // Add 40 TNT directly above the final block.
        // Keep the stack tightly grouped to provide a concentrated upward blast.
        for (int i = 0; i < BOTTOM_STACK_COUNT; i++) {
            spawnTnt(
                    x + 0.5,
                    stopY + 1.0 + i * 0.12,
                    z + 0.5
            );
        }

        return true;
    }

    private void spawnTnt(double x, double y, double z) {
        Entity entity = Payload.spawn(level, x, y, z);

        if (!(entity instanceof PrimedTnt tnt)) {
            return;
        }

        tnt.setFuse(TNT_FUSE);
        tnt.setNoGravity(true);
        tnt.setDeltaMovement(0.0, 0.0, 0.0);
        tnt.hurtMarked = true;
    }
}
