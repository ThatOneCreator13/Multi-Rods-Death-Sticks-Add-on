
package com.thatonecreator13.osc.chunk_deleter;

// PURPOSE: Fire four reduced-power Stab columns at the corners of a 7x7 square.
// Chunk Deleter uses 3 TNT per layer and 10 bottom TNT per corner,
// for 40 bottom TNT total. The normal Stab shot is not modified.

import com.thatonecreator13.osc.Payload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.state.BlockState;

public final class ChunkDeleterStrike {
    private static final int CORNER_OFFSET = 3;
    private static final int START_ABOVE_SURFACE = 150;
    private static final int VERTICAL_SPACING = 3;
    private static final int TNT_PER_LAYER = 3;
    private static final int BOTTOM_TNT_PER_CORNER = 10;
    private static final int TNT_FUSE = 1;
    private static final float STOP_RESISTANCE = 1200.0F;

    private final ServerLevel level;
    private final BlockPos target;

    public ChunkDeleterStrike(ServerLevel level, BlockPos target) {
        this.level = level;
        this.target = target.immutable();
    }

    public boolean tick() {
        int centerX = target.getX();
        int centerZ = target.getZ();

        // Fire all four corners during the same tick.
        fireStabColumn(centerX - CORNER_OFFSET, centerZ - CORNER_OFFSET);
        fireStabColumn(centerX - CORNER_OFFSET, centerZ + CORNER_OFFSET);
        fireStabColumn(centerX + CORNER_OFFSET, centerZ - CORNER_OFFSET);
        fireStabColumn(centerX + CORNER_OFFSET, centerZ + CORNER_OFFSET);

        return true;
    }

    private void fireStabColumn(int x, int z) {
        // Find the highest non-air block in this corner's column.
        int highestY = level.getMaxY() - 1;

        while (highestY > level.getMinY()
                && level.getBlockState(new BlockPos(x, highestY, z)).isAir()) {
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

        // Reduced-power vertical column: 3 TNT per layer instead of 7.
        for (int y = startY; y > stopY; y -= VERTICAL_SPACING) {
            for (int i = 0; i < TNT_PER_LAYER; i++) {
                spawnTnt(
                        x + 0.5,
                        y + i * 0.12,
                        z + 0.5
                );
            }
        }

        // Exactly 10 bottom TNT per corner, totaling 40 across Chunk Deleter.
        for (int i = 0; i < BOTTOM_TNT_PER_CORNER; i++) {
            spawnTnt(
                    x + 0.5,
                    stopY + 1.0 + i * 0.12,
                    z + 0.5
            );
        }
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
