package com.faketils;

import com.faketils.commands.Command;
import com.faketils.config.Config;
import com.faketils.events.FtEvent;
import com.faketils.events.FtEventBus;
import com.faketils.features.Farming;
import com.faketils.features.FishingTickHandler;
import com.faketils.features.PestHelper;
import com.faketils.utils.FarmingTitleRenderer;
import com.faketils.utils.Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
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
        config = Config.INSTANCE;
        config.initialize();

        Command.register();

        FishingTickHandler.initialize();
        Farming.initialize();
        FarmingTitleRenderer.init();
        PestHelper.initialize();

        WorldRenderEvents.LAST.register(context -> {
            FtEventBus.emit(new FtEvent.WorldRender(context));
        });
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

    public static void saveAll() {
        config.markDirty();
        config.writeData();
    }
}