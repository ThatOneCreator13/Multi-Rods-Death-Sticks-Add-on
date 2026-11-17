
// PURPOSE: Save and retrieve independent Slap Shot destinations on individual fishing rods.

package com.thatonecreator13.osc.launch.slap_shot;

import com.lincoln.multirods.RodIdManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

public final class SlapShotDestination {

    private static final String X_KEY = "SlapShotX";
    private static final String Y_KEY = "SlapShotY";
    private static final String Z_KEY = "SlapShotZ";
    private static final String SET_KEY = "SlapShotConfigured";
    private static final String ROD_KEY = "SlapShotRodId";

    private SlapShotDestination() {
    }

    public record Destination(int x, int y, int z) {
    }

    public static void save(ItemStack rod, int x, int y, int z) {
        UUID rodId = RodIdManager.getId(rod);

        CustomData existing = rod.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing == null
                ? new CompoundTag()
                : existing.copyTag();

        tag.putInt(X_KEY, x);
        tag.putInt(Y_KEY, y);
        tag.putInt(Z_KEY, z);
        tag.putBoolean(SET_KEY, true);
        tag.putString(ROD_KEY, rodId.toString());

        rod.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(tag)
        );
    }

    public static Destination get(ItemStack rod) {
        if (rod.isEmpty()) {
            return null;
        }

        CustomData customData = rod.get(DataComponents.CUSTOM_DATA);

        if (customData == null) {
            return null;
        }

        CompoundTag tag = customData.copyTag();

        if (!tag.getBooleanOr(SET_KEY, false)) {
            return null;
        }

        UUID rodId = RodIdManager.getId(rod);

        String savedRodId = tag.getString(ROD_KEY).orElse("");

        if (!rodId.toString().equals(savedRodId)) {
            return null;
        }

        return new Destination(
                tag.getInt(X_KEY).orElse(0),
                tag.getInt(Y_KEY).orElse(0),
                tag.getInt(Z_KEY).orElse(0)
        );
    }

    public static boolean hasDestination(ItemStack rod) {
        return get(rod) != null;
    }
}
