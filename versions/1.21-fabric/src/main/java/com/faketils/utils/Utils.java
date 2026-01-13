package com.faketils.utils;

import com.faketils.Faketils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class Utils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static String stripColorCodes(String s) {
        return s == null ? "" : Formatting.strip(s);
    }

    public static String cleanSB(String s) {
        if (s == null) return "";

        String stripped = Formatting.strip(s);

        return stripped
                .replaceAll("[^\\p{Print}]", "")
                .trim();
    }

    public static void log(String message) {
        if (!Faketils.config.debug) return;
        if (mc.player != null) {
            mc.player.sendMessage(
                    Text.literal("§7[§bFaketils§7] §f" + message),
                    false
            );
        }
    }
    public static void logSound(PlaySoundS2CPacket packet) {
        Identifier id = Registries.SOUND_EVENT.getId(packet.getSound().value());

        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();

        float volume = packet.getVolume();
        float pitch = packet.getPitch();

        log(
                "[Sound] " + id +
                        " @ " + x + ", " + y + ", " + z +
                        " vol=" + volume +
                        " pitch=" + pitch
        );
    }

    public static List<String> getSidebarLines() {
        List<String> lines = new ArrayList<>();

        if (mc.world == null) return lines;

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective =
                scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (objective == null) return lines;

        List<Text> textLines = new ArrayList<>();

        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
            String name = entry.name().toString();
            Team team = scoreboard.getTeam(name);
            if (team == null) continue;

            Text line = Text.empty()
                    .append(team.getPrefix())
                    .append(team.getSuffix());

            if (!line.getString().trim().isEmpty()) {
                textLines.add(line);
            }
        }

        Collections.reverse(textLines);

        for (Text t : textLines) {
            lines.add(t.getString());
        }

        return lines;
    }

    public static boolean isInSkyblock() {
        if (mc.world == null || mc.player == null) return false;
        if (mc.isInSingleplayer()) return false;

        ScoreboardObjective obj = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (obj == null) return false;

        String objectiveName = stripColorCodes(obj.getDisplayName().getString()).toLowerCase(Locale.ROOT);
        return objectiveName.contains("skyblock");
    }

    public static class FtVec {
        public final double x;
        public final double y;
        public final double z;

        public FtVec(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vec3d toVec3d() {
            return new Vec3d(x, y, z);
        }

        public FtVec add(double x, double y, double z) {
            return new FtVec(this.x + x, this.y + y, this.z + z);
        }

        public Vec3d center() {
            return new Vec3d(x + 0.5, y + 0.5, z + 0.5);
        }
    }
}