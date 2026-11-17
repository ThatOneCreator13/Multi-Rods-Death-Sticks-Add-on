
package com.thatonecreator13.osc;

// PURPOSE: Find a valid block target for the Stab shot.

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class StrikeTargeting {

    public static final double MAX_RANGE = 60.0;

    private StrikeTargeting() {
    }

    public static BlockPos findTarget(ServerPlayer player) {
        HitResult hit = player.pick(MAX_RANGE, 1.0F, false);

        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        if (!(hit instanceof BlockHitResult blockHit)) {
            return null;
        }

        return blockHit.getBlockPos();
    }
}
