
package com.thatonecreator13.osc;

// PURPOSE: Shared entity payload for every shot in the add-on.

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

public final class Payload {

    // Change this value to change the entity used by every shot.
    public static Identifier PAYLOAD_ID =
            Identifier.fromNamespaceAndPath("minecraft", "tnt");

    private Payload() {
    }

    /**
     * Creates and spawns the configured payload.
     * Shot-specific behavior, including TNT fuse length, belongs
     * in the individual shot implementation.
     *
     * @return the spawned entity, or null if spawning failed.
     */
    public static Entity spawn(
            ServerLevel level,
            double x,
            double y,
            double z
    ) {
        EntityType<?> type =
                BuiltInRegistries.ENTITY_TYPE.getValue(PAYLOAD_ID);

        if (type == null) {
            return null;
        }

        Entity entity = type.create(level, EntitySpawnReason.TRIGGERED);

        if (entity == null) {
            return null;
        }

        entity.setPos(x, y, z);

        if (!level.addFreshEntity(entity)) {
            return null;
        }

        return entity;
    }
}
