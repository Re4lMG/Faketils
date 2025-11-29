package com.faketils.commands;

import com.faketils.Faketils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;

public class Command {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("ft")
                .executes(Command::execute)
        );

        dispatcher.register(ClientCommandManager.literal("faketils")
                .executes(Command::execute)
        );

        dispatcher.register(ClientCommandManager.literal("faketil")
                .executes(Command::execute)
        );
    }

    private static int execute(CommandContext<FabricClientCommandSource> context) {
        Faketils.currentGui = Faketils.config.gui();
        return 1;
    }
}