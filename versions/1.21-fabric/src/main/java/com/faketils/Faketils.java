package com.faketils;

import com.faketils.commands.Command;
import com.faketils.config.Config;
import com.faketils.events.FlyHandler;
import com.faketils.events.RotationHandler;
import com.faketils.events.WalkingHandler;
import com.faketils.features.*;
import com.faketils.utils.FarmingTitleRenderer;
import com.faketils.utils.Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class Faketils implements ClientModInitializer {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static Screen currentGui = null;
    public static final String MOD_ID = "faketils";
    public static final String NAME = "Faketils";

    public static Config config() {
        return Config.HANDLER.instance();
    }

    @Override
    public void onInitializeClient() {
        Config.HANDLER.load();

        Command.register();

        //DanceRoomSolver.initialize();
        WalkingHandler.init();
        RotationHandler.init();
        FlyHandler.init();
        TipAll.initialize();
        Experiments.init();
        Harp.init();
        Fishing.initialize();
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