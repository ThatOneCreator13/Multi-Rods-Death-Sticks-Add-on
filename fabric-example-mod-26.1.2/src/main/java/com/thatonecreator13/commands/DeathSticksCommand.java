
package com.thatonecreator13.commands;

// PURPOSE: Register /multirods deathsticks give to give the player every Creative Death Stick and a normal Slap Shot fishing rod.

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DeathSticksCommand implements ModInitializer {

    private static final String[] DEATH_STICK_NAMES = {
            "Stab",
            "Nuke",
            "Bend Shot",
            "Gate Grater",
            "Chunk Deleter",
            "World Flattener",
            "Spitfire",
            "Grand Entrence"
    };

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        registerCommands(dispatcher)
        );
    }

    private static void registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("multirods")
                        .then(Commands.literal("deathsticks")
                                .then(Commands.literal("give")
                                        .executes(context -> {
                                            ServerPlayer player =
                                                    context.getSource().getPlayerOrException();

                                            giveDeathSticks(player);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "[Death Sticks] Given all 8 Death Sticks and 1 normal Slap Shot rod!"
                                                    ),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                        )
        );
    }

    private static void giveDeathSticks(ServerPlayer player) {

        // Give all eight named Death Sticks.
        for (String name : DEATH_STICK_NAMES) {

            ItemStack rod = new ItemStack(Items.FISHING_ROD);

            rod.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal(name)
            );

            player.getInventory().placeItemBackInInventory(rod);
        }

        // Give one ordinary, unnamed fishing rod for Slap Shot.
        ItemStack slapShotRod = new ItemStack(Items.FISHING_ROD);

        player.getInventory().placeItemBackInInventory(slapShotRod);
    }
}
