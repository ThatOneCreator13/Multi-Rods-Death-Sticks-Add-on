
// PURPOSE: Route named fishing rods to their corresponding shot handlers.

package com.thatonecreator13.mixin;

import com.thatonecreator13.osc.bend_shot.BendShotHandler;
import com.thatonecreator13.osc.chunk_deleter.ChunkDeleterHandler;
import com.thatonecreator13.osc.gate_grater.GateGraterHandler;
import com.thatonecreator13.osc.grand_entrence.GrandEntrenceHandler;
import com.thatonecreator13.osc.nuke.NukeHandler;
import com.thatonecreator13.osc.spitfire.SpitfireHandler;
import com.thatonecreator13.osc.stab.StabHandler;
import com.thatonecreator13.osc.world_flattener.WorldFlattenerHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingRodItem.class)
public abstract class StrikeFishingRodMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void multirods$handleShot(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        ItemStack stack = player.getItemInHand(hand);
        String name = stack.getHoverName().getString();

        boolean isStab = "Stab".equals(name);
        boolean isNuke = "Nuke".equals(name);
        boolean isBendShot = "Bend Shot".equals(name);
        boolean isGateGrater = "Gate Grater".equals(name);
        boolean isChunkDeleter = "Chunk Deleter".equals(name);
        boolean isWorldFlattener = "World Flattener".equals(name);
        boolean isSpitfire = "Spitfire".equals(name);
        boolean isGrandEntrence = "Grand Entrence".equals(name);

        if (!isStab
                && !isNuke
                && !isBendShot
                && !isGateGrater
                && !isChunkDeleter
                && !isWorldFlattener
                && !isSpitfire
                && !isGrandEntrence) {
            return;
        }

        cir.setReturnValue(InteractionResult.SUCCESS);

        if (level.isClientSide()
                || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (isStab) {
            StabHandler.handle(serverPlayer);
        } else if (isNuke) {
            NukeHandler.handle(serverPlayer);
        } else if (isBendShot) {
            BendShotHandler.handle(serverPlayer);
        } else if (isGateGrater) {
            GateGraterHandler.handle(serverPlayer);
        } else if (isChunkDeleter) {
            ChunkDeleterHandler.handle(serverPlayer);
        } else if (isWorldFlattener) {
            WorldFlattenerHandler.handle(serverPlayer);
        } else if (isSpitfire) {
            SpitfireHandler.handle(serverPlayer);
        } else if (isGrandEntrence) {
            GrandEntrenceHandler.handle(serverPlayer);
        }
    }
}
