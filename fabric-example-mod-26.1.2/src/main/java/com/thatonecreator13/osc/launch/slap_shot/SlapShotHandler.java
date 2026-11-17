
// PURPOSE: Detect Slap Shot receivers and target launch payloads using the shared 60-block targeting system.

package com.thatonecreator13.osc.launch.slap_shot;

import com.lincoln.multirods.FishingHookAccess;
import com.lincoln.multirods.MultiRodData;
import com.lincoln.multirods.RodIdManager;
import com.thatonecreator13.osc.StrikeTargeting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SlapShotHandler {

    private static final Pattern LEGACY_COORDINATE =
            Pattern.compile("^\\s*([XYZ])\\s*:\\s*(-?\\d+)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private SlapShotHandler() {
    }

    public static void handle(FishingHook hook, BlockPos hitPos) {

        if (!(hook.level() instanceof ServerLevel level)) {
            return;
        }

        if (!(hook instanceof FishingHookAccess access)) {
            return;
        }

        UUID rodId = access.multirods$getRodId();

        if (rodId == null) {
            return;
        }

        UUID ownerId = MultiRodData.getOwner(rodId);

        ServerPlayer player = ownerId == null
                ? null
                : level.getServer().getPlayerList().getPlayer(ownerId);

        if (player == null
                && hook.getPlayerOwner() instanceof ServerPlayer hookOwner) {
            player = hookOwner;
        }

        if (player == null || !player.isAlive()) {
            return;
        }

        ItemStack rod = findRod(player, rodId);

        if (rod == null) {
            player.sendSystemMessage(Component.literal(
                    "[Slap Shot] ERROR: Original rod not found."
            ));
            return;
        }

        // Receiver detection remains independent of launch targeting.
        BlockPos receiverPos = null;

        if (isPressurePlate(level, hitPos)) {
            receiverPos = hitPos;
        } else if (isPressurePlate(level, hitPos.above())) {
            receiverPos = hitPos.above();
        }

        if (receiverPos != null) {

            SlapShotDestination.Destination destination =
                    readReceiver(level, receiverPos);

            if (destination == null) {
                player.sendSystemMessage(Component.literal(
                        "[Slap Shot] There's no coords!"
                ));
                return;
            }

            SlapShotDestination.save(
                    rod,
                    destination.x(),
                    destination.y(),
                    destination.z()
            );

            SlapShotDestination.Destination verification =
                    SlapShotDestination.get(rod);

            if (verification == null) {
                player.sendSystemMessage(Component.literal(
                        "[Slap Shot] ERROR: Could not save destination."
                ));
                return;
            }

            player.sendSystemMessage(Component.literal(
                    "[Slap Shot] Destination saved: "
                            + verification.x() + ", "
                            + verification.y() + ", "
                            + verification.z()
            ));

            return;
        }

        SlapShotDestination.Destination destination =
                SlapShotDestination.get(rod);

        if (destination == null) {
            player.sendSystemMessage(Component.literal(
                    "[Slap Shot] This rod has no saved destination. Cast onto a receiver plate first."
            ));
            return;
        }

        // Use the same 60-block targeting system as the other Death Sticks shots.
        BlockPos target = StrikeTargeting.findTarget(player);

        if (target == null) {
            player.sendSystemMessage(Component.literal(
                    "[Slap Shot] No valid target within 60 blocks."
            ));
            return;
        }

        SlapShotLaunch.start(player, level, destination, target);
    }

    private static boolean isPressurePlate(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock()
                instanceof BasePressurePlateBlock;
    }

    private static ItemStack findRod(ServerPlayer player, UUID rodId) {

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (stack.getItem() instanceof FishingRodItem
                    && rodId.equals(RodIdManager.getId(stack))) {
                return stack;
            }
        }

        ItemStack offhand = player.getOffhandItem();

        if (offhand.getItem() instanceof FishingRodItem
                && rodId.equals(RodIdManager.getId(offhand))) {
            return offhand;
        }

        return null;
    }

    private static SlapShotDestination.Destination readReceiver(
            ServerLevel level,
            BlockPos platePos
    ) {

        for (Direction direction : Direction.values()) {

            BlockPos signPos = platePos.relative(direction);
            BlockState signState = level.getBlockState(signPos);

            if (!(signState.getBlock() instanceof SignBlock)) {
                continue;
            }

            if (!(level.getBlockEntity(signPos) instanceof SignBlockEntity sign)) {
                continue;
            }

            Direction signFacing;

            if (signState.getBlock() instanceof WallSignBlock) {

                signFacing = signState.getValue(
                        WallSignBlock.FACING
                );

            } else if (signState.getBlock() instanceof StandingSignBlock) {

                int rotation = signState.getValue(
                        StandingSignBlock.ROTATION
                );

                signFacing = Direction.fromYRot(rotation * 22.5F);

            } else {
                continue;
            }

            if (signFacing != direction.getOpposite()) {
                continue;
            }

            String[] lines = new String[4];

            for (int i = 0; i < 4; i++) {
                lines[i] = sign.getFrontText()
                        .getMessages(false)[i]
                        .getString()
                        .trim();
            }

            String combined = String.join(" ", lines).trim();
            String[] coordinates = combined.split("\\s*,\\s*");

            if (coordinates.length == 3) {
                try {
                    int x = Integer.parseInt(coordinates[0].trim());
                    int y = Integer.parseInt(coordinates[1].trim());
                    int z = Integer.parseInt(coordinates[2].trim());

                    return new SlapShotDestination.Destination(x, y, z);

                } catch (NumberFormatException ignored) {
                    // Try legacy coordinate format.
                }
            }

            Integer x = null;
            Integer y = null;
            Integer z = null;

            for (String line : lines) {

                Matcher matcher = LEGACY_COORDINATE.matcher(line);

                if (!matcher.matches()) {
                    continue;
                }

                try {
                    int value = Integer.parseInt(matcher.group(2));

                    switch (matcher.group(1).toUpperCase()) {
                        case "X" -> x = value;
                        case "Y" -> y = value;
                        case "Z" -> z = value;
                    }

                } catch (NumberFormatException ignored) {
                    return null;
                }
            }

            if (x != null && y != null && z != null) {
                return new SlapShotDestination.Destination(x, y, z);
            }
        }

        return null;
    }
}
