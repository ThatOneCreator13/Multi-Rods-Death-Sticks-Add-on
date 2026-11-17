
package com.thatonecreator13.mixin;

// PURPOSE: Run Slap Shot block-impact handling only on the server, once per fishing hook.

import com.thatonecreator13.osc.launch.slap_shot.SlapShotHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class SlapShotMixin {

    private static final Set<UUID> MRDEATHSTICKS$PROCESSED_HOOKS =
            ConcurrentHashMap.newKeySet();

    @Inject(method = "onHitBlock", at = @At("HEAD"))
    private void mrdeathsticks$handleSlapShot(
            BlockHitResult hitResult,
            CallbackInfo ci
    ) {
        FishingHook hook = (FishingHook) (Object) this;

        // Never process the client-side copy of a fishing hook.
        if (!(hook.level() instanceof ServerLevel)) {
            return;
        }

        // Prevent repeated impacts from activating the same hook again.
        if (!MRDEATHSTICKS$PROCESSED_HOOKS.add(hook.getUUID())) {
            return;
        }

        System.out.println(
                "[Slap Shot] Server impact accepted at "
                        + hitResult.getBlockPos()
                        + " | Hook: "
                        + hook.getUUID()
        );

        SlapShotHandler.handle(hook, hitResult.getBlockPos());
    }
}
