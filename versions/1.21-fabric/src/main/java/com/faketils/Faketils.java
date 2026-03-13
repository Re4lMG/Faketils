package com.faketils;

import com.faketils.commands.Command;
import com.faketils.config.Config;
import com.faketils.events.FlyHandler;
import com.faketils.events.RotationHandler;
import com.faketils.features.*;
import com.faketils.utils.FarmingTitleRenderer;
import com.faketils.utils.Utils;
import net.fabricmc.api.ClientModInitializer;
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
        Config.HANDLER.load();
        config = Config.INSTANCE;

        Command.register();

        //DanceRoomSolver.initialize();
        RotationHandler.init();
        FlyHandler.init();
        FishingTickHandler.initialize();
        Farming.initialize();
        FarmingTitleRenderer.init();
        SphinxSolver.init();
        PestHelper.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                if (currentGui != null) {
                    client.setScreen(currentGui);
                    currentGui = null;
                }
                if (client.world.getTime() % 20 == 0L) {
                    Utils.isInSkyblock();
                }
            }
        });
    }
}