package com.faketils.events;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TabListParser {

    public static List<String> getTabLines() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getNetworkHandler() == null) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();

        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {

            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;

            String line = displayName.getString();

            if (!line.isBlank()) {
                lines.add(line);
            }
        }

        return lines;
    }
}