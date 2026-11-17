
package com.thatonecreator13.osc.launch.slap_shot;

// PURPOSE: Launch affected players and entities smoothly using consistent upward velocity, then teleport them to their saved destinations with fall-only protection.

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SlapShotLaunch {

    private static final int WIND_CHARGE_COUNT = 8;
    private static final int ASCENT_TICKS = 100;
    private static final int FALL_PROTECTION_TICKS = 15 * 20;

    private static final double ASCENT_HEIGHT = 150.0;
    private static final double ASCENT_SPEED = (ASCENT_HEIGHT / ASCENT_TICKS) * 5.0;
    private static final double LANDING_HEIGHT = 150.0;

    private static final double CHARGE_RADIUS = 2.0;
    private static final double WIND_CHARGE_EFFECT_RADIUS = 3.5;

    private static final Map<UUID, LaunchState> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> FALL_PROTECTION = new HashMap<>();

    private static boolean registered = false;

    private SlapShotLaunch() {
    }

    private static final class LaunchState {

        private final Entity entity;
        private final ServerLevel level;

        private final double startX;
        private final double startY;
        private final double startZ;

        private final int destinationX;
        private final int destinationZ;

        private final double destinationCenterX;
        private final double destinationCenterZ;

        private final boolean originalNoGravity;
        private final boolean originalMayfly;
        private final boolean originalFlying;

        private int elapsedTicks;

        private LaunchState(
                Entity entity,
                ServerLevel level,
                SlapShotDestination.Destination destination
        ) {
            this.entity = entity;
            this.level = level;

            this.startX = entity.getX();
            this.startY = entity.getY();
            this.startZ = entity.getZ();

            this.destinationX = destination.x();
            this.destinationZ = destination.z();

            this.destinationCenterX = destination.x() + 0.5;
            this.destinationCenterZ = destination.z() + 0.5;

            this.originalNoGravity = entity.isNoGravity();

            if (entity instanceof ServerPlayer player) {
                this.originalMayfly = player.getAbilities().mayfly;
                this.originalFlying = player.getAbilities().flying;
            } else {
                this.originalMayfly = false;
                this.originalFlying = false;
            }
        }
    }

    public static boolean isFallProtected(Entity entity) {
        Integer remaining = FALL_PROTECTION.get(entity.getUUID());
        return remaining != null && remaining > 0;
    }

    public static void start(
            ServerPlayer shooter,
            ServerLevel level,
            SlapShotDestination.Destination destination,
            BlockPos target
    ) {
        registerTick();

        Vec3 center = Vec3.atCenterOf(target);

        List<Entity> affectedEntities = findAffectedEntities(level, center);

        int highestBlockY = getHighestBlockY(
                level,
                destination.x(),
                destination.z()
        );

        int playerCount = 0;
        int otherEntityCount = 0;

        for (Entity entity : affectedEntities) {

            if (ACTIVE.containsKey(entity.getUUID())) {
                continue;
            }

            LaunchState state = new LaunchState(
                    entity,
                    level,
                    destination
            );

            ACTIVE.put(entity.getUUID(), state);

            prepareEntity(entity);

            if (entity instanceof ServerPlayer player) {
                player.sendSystemMessage(
                        Component.literal("SLAP SHOT ACTIVATED!")
                );
                playerCount++;
            } else {
                otherEntityCount++;
            }
        }

        spawnWindChargeBurst(shooter, level, center);

        shooter.sendSystemMessage(
                Component.literal(
                        "[Slap Shot] Payload detonated! "
                                + playerCount + " players launched, "
                                + otherEntityCount + " other entities launched."
                )
        );

        if (affectedEntities.isEmpty()) {
            shooter.sendSystemMessage(
                    Component.literal(
                            "[Slap Shot] No entities were inside the payload radius."
                    )
            );
        }
    }

    private static int getHighestBlockY(
            ServerLevel level,
            int x,
            int z
    ) {
        return level.getHeight(
                Heightmap.Types.MOTION_BLOCKING,
                x,
                z
        );
    }

    private static void prepareEntity(Entity entity) {
        entity.setNoGravity(true);

        // Give players and mobs identical upward velocity.
        entity.setDeltaMovement(0.0, ASCENT_SPEED, 0.0);

        entity.fallDistance = 0.0F;
        entity.hurtMarked = true;

        if (entity instanceof ServerPlayer player) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static void restoreEntity(
            Entity entity,
            LaunchState state
    ) {
        entity.setNoGravity(state.originalNoGravity);

        if (entity instanceof ServerPlayer player) {
            player.getAbilities().mayfly = state.originalMayfly;
            player.getAbilities().flying = state.originalFlying;
            player.onUpdateAbilities();
        }

        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
        entity.hurtMarked = true;
    }

    private static void registerTick() {
        if (registered) {
            return;
        }

        registered = true;

        ServerTickEvents.END_SERVER_TICK.register(
                SlapShotLaunch::tick
        );
    }

    private static List<Entity> findAffectedEntities(
            ServerLevel level,
            Vec3 center
    ) {
        List<Entity> affected = new ArrayList<>();

        double searchRadius =
                CHARGE_RADIUS + WIND_CHARGE_EFFECT_RADIUS;

        AABB searchArea = new AABB(
                center.x - searchRadius,
                center.y - WIND_CHARGE_EFFECT_RADIUS,
                center.z - searchRadius,
                center.x + searchRadius,
                center.y + WIND_CHARGE_EFFECT_RADIUS,
                center.z + searchRadius
        );

        List<Entity> nearby = level.getEntitiesOfClass(
                Entity.class,
                searchArea,
                entity -> !entity.isRemoved()
                        && !(entity instanceof FishingHook)
                        && !(entity instanceof WindCharge)
        );

        for (Entity entity : nearby) {
            if (isInsideChargeRadius(entity, center)) {
                affected.add(entity);
            }
        }

        return affected;
    }

    private static boolean isInsideChargeRadius(
            Entity entity,
            Vec3 center
    ) {
        for (int i = 0; i < WIND_CHARGE_COUNT; i++) {

            double angle = (Math.PI * 2.0 * i) / WIND_CHARGE_COUNT;

            double offsetX = Math.cos(angle) * CHARGE_RADIUS;
            double offsetZ = Math.sin(angle) * CHARGE_RADIUS;

            Vec3 chargePosition = center.add(
                    offsetX,
                    0.0,
                    offsetZ
            );

            if (entity.position().distanceToSqr(chargePosition)
                    <= WIND_CHARGE_EFFECT_RADIUS * WIND_CHARGE_EFFECT_RADIUS) {
                return true;
            }
        }

        return false;
    }

    private static void spawnWindChargeBurst(
            ServerPlayer shooter,
            ServerLevel level,
            Vec3 center
    ) {
        for (int i = 0; i < WIND_CHARGE_COUNT; i++) {

            double angle = (Math.PI * 2.0 * i) / WIND_CHARGE_COUNT;

            double offsetX = Math.cos(angle) * CHARGE_RADIUS;
            double offsetZ = Math.sin(angle) * CHARGE_RADIUS;

            WindCharge charge = new WindCharge(
                    shooter,
                    level,
                    center.x + offsetX,
                    center.y,
                    center.z + offsetZ
            );

            charge.setDeltaMovement(0.0, -1.0, 0.0);

            level.addFreshEntity(charge);
        }
    }

    private static void tick(MinecraftServer server) {

        Iterator<Map.Entry<UUID, Integer>> protectionIterator =
                FALL_PROTECTION.entrySet().iterator();

        while (protectionIterator.hasNext()) {

            Map.Entry<UUID, Integer> entry = protectionIterator.next();

            int remaining = entry.getValue() - 1;

            if (remaining <= 0) {
                protectionIterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }

        Iterator<Map.Entry<UUID, LaunchState>> iterator =
                ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {

            LaunchState state = iterator.next().getValue();
            Entity entity = state.entity;

            if (!entity.isAlive()
                    || entity.isRemoved()
                    || entity.level() != state.level) {

                FALL_PROTECTION.remove(entity.getUUID());

                if (!entity.isRemoved()) {
                    restoreEntity(entity, state);
                }

                iterator.remove();
                continue;
            }

            state.elapsedTicks++;

            if (state.elapsedTicks < ASCENT_TICKS) {

                // Maintain the same upward speed without teleporting.
                entity.setDeltaMovement(
                        0.0,
                        ASCENT_SPEED,
                        0.0
                );

                entity.fallDistance = 0.0F;
                entity.hurtMarked = true;

                continue;
            }

            // Teleport after the full 100-tick ascent.
            int highestBlockY = getHighestBlockY(
                    state.level,
                    state.destinationX,
                    state.destinationZ
            );

            double landingY = highestBlockY + LANDING_HEIGHT;

            entity.setPos(
                    state.destinationCenterX,
                    landingY,
                    state.destinationCenterZ
            );

            entity.setDeltaMovement(Vec3.ZERO);
            entity.fallDistance = 0.0F;
            entity.hurtMarked = true;

            FALL_PROTECTION.put(
                    entity.getUUID(),
                    FALL_PROTECTION_TICKS
            );

            restoreEntity(entity, state);

            if (entity instanceof ServerPlayer player) {
                player.sendSystemMessage(
                        Component.literal(
                                "[Slap Shot] Teleported! Fall protection active for 15 seconds."
                        )
                );
            }

            iterator.remove();
        }
    }
}
