package com.faketils;

import com.faketils.commands.Command;
import com.faketils.config.Config;
import com.faketils.features.FishingTickHandler;
import com.faketils.utils.Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import java.io.File;

public class Faketils implements ClientModInitializer {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static Screen currentGui = null;
    public static final String MOD_ID = "faketils";
    public static final String NAME = "Faketils";

    public static File configDirectory;
    public static Config config;

    @Override
    public void onInitializeClient() {
        configDirectory = new File(mc.runDirectory, MOD_ID);
        configDirectory.mkdirs();
        config = Config.INSTANCE;
        config.initialize();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            Command.register(dispatcher, registryAccess);
        });
        FishingTickHandler.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (currentGui != null) {
                client.setScreen(currentGui);
                currentGui = null;
            }

            if (client.player != null && client.world != null) {
                if (client.world.getTime() % 20 == 0L) {
                    Utils.isInSkyblock();
                }
            }
        });
    }

    public static void saveAll() {
        config.markDirty();
        config.writeData();
    }
}