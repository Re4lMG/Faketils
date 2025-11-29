package com.faketils.utils;

import com.faketils.Faketils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Utils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static String stripColorCodes(String string) {
        return Formatting.strip(string);
    }

    public static void log(String message) {
        if (!Faketils.config.debug) return;
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§7[§bFaketils§7] §f" + message), false);
        }
    }

    public static boolean isInSkyblock() {
        if (mc.world == null || mc.player == null) return false;
        if (mc.isInSingleplayer()) return false;

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (objective == null) return false;
        return stripColorCodes(objective.getDisplayName().getString()).toLowerCase().contains("skyblock");
    }
}