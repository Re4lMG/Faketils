package com.faketils.utils;

import com.faketils.Faketils;
import com.faketils.events.TabListParser;
import com.mojang.authlib.properties.Property;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.DisplaySlot;

import java.util.*;

public class Utils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static String stripColorCodes(String s) {
        return s == null ? "" : ChatFormatting.stripFormatting(s);
    }

    public static String cleanSB(String s) {
        if (s == null) return "";

        String stripped = ChatFormatting.stripFormatting(s);

        return stripped
                .replaceAll("[^\\p{Print}]", "")
                .trim();
    }

    public static void log(String message) {
        if (!Faketils.config().debug) return;
        if (mc.player != null) {
            mc.player.sendSystemMessage(
                    Component.literal("§7[§bFaketils§7] §f" + message)
            );
        }
    }

    public static String getHeadTexture(ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD) || !stack.has(DataComponents.PROFILE)) {
            return "";
        }

        ResolvableProfile profileComponent = stack.get(DataComponents.PROFILE);
        if (profileComponent == null) return "";

        com.mojang.authlib.GameProfile profile = profileComponent.partialProfile();
        if (profile.properties() == null) return "";

        return profile.properties().get("textures").stream()
                .filter(Objects::nonNull)
                .map(Property::value)
                .findFirst()
                .orElse("");
    }

    public static void simulateUseItem(MultiPlayerGameMode interactionManager) {
        if (mc.player == null) return;
        interactionManager.useItem(mc.player, InteractionHand.MAIN_HAND);
    }

    public static List<String> getSidebarLines() {
        List<String> lines = new ArrayList<>();

        if (mc.level == null) return lines;

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

        if (objective == null) return lines;

        List<Component> textLines = new ArrayList<>();

        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            String name = entry.owner();
            PlayerTeam team = scoreboard.getPlayersTeam(name);
            if (team == null) continue;

            Component line = Component.empty()
                    .copy()
                    .append(team.getPlayerPrefix())
                    .append(team.getPlayerSuffix());

            if (!line.getString().trim().isEmpty()) {
                textLines.add(line);
            }
        }

        Collections.reverse(textLines);

        for (Component t : textLines) {
            lines.add(t.getString());
        }

        return lines;
    }

    public static boolean isInSkyblock() {
        if (mc.level == null || mc.player == null) return false;
        if (mc.hasSingleplayerServer()) return false;

        Objective obj = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);

        if (obj == null) return false;

        String objectiveName = stripColorCodes(obj.getDisplayName().getString()).toLowerCase(Locale.ROOT);
        return objectiveName.contains("skyblock");
    }

    public static boolean isInGarden() {
        if (!isInSkyblock()) return false;
        return TabListParser.getTabLines().contains("Area: Garden");
    }

    public static String getCurrentArea() {
        for (String line : TabListParser.getTabLines()) {
            if (line.startsWith("Area:")) {
                return line.substring(5).trim();
            }
        }
        return "Unknown";
    }
}