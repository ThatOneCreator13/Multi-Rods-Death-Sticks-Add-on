
// PURPOSE: Cancel only fall damage while Slap Shot's temporary protection is active.

package com.thatonecreator13.mixin;

import com.thatonecreator13.osc.launch.slap_shot.SlapShotLaunch;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SlapShotFallProtectionMixin {

    @Inject(
            method = "causeFallDamage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mrdeathsticks$protectFromFallDamage(
            double fallDistance, float damageModifier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (SlapShotLaunch.isFallProtected(entity)) {
            cir.setReturnValue(false);
        }
    }
}
