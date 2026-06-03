package com.faketils.features;

import com.faketils.Faketils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class TipAll {

    private static final Minecraft mc = Minecraft.getInstance();
    private static long lastTipTime = 0L;
    private static final long INTERVAL = 10 * 60 * 1000L;

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick());
    }

    public static void onTick() {
        if (mc.hasSingleplayerServer()) return;
        if (mc.player == null) return;
        if (!Faketils.config().tipAll) return;

        long now = System.currentTimeMillis();
        if (now - lastTipTime >= INTERVAL) {
            mc.getConnection().sendChat("/tipall");
            lastTipTime = now;
        }
    }
}