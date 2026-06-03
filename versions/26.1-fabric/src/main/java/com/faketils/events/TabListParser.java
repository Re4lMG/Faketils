package com.faketils.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class TabListParser {

    public static List<String> getTabLines() {
        Minecraft client = Minecraft.getInstance();

        if (client.getConnection() == null) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();

        for (PlayerInfo entry : client.getConnection().getOnlinePlayers()) {

            Component displayName = entry.getTabListDisplayName();
            if (displayName == null) continue;

            String line = displayName.getString();

            if (!line.isBlank()) {
                lines.add(line);
            }
        }

        return lines;
    }
}