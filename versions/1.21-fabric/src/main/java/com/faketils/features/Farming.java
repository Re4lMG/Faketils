package com.faketils.features;

import com.faketils.config.Config;
import com.faketils.events.FtEvent;
import com.faketils.events.FtEventBus;
import com.faketils.events.PacketEvent;
import com.faketils.mixin.PlayerInventoryAccessor;
import com.faketils.utils.FarmingWaypoints;
import com.faketils.utils.RenderUtils;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

public class Farming {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random random = new Random();

    private static final KeyBinding toggleKey = Config.INSTANCE.toggleMacro;
    private static final KeyBinding pauseKey = Config.INSTANCE.pauseMacro;
    private static final KeyBinding resetKey = Config.INSTANCE.resetFakeFails;

    private static boolean keysAreHeld = false;

    public static boolean isActive = false;
    public static boolean isPaused = false;
    public static String currentMode = "none";

    private static BlockPos lastWaypoint = null;
    private static int ticksOnWaypoint = 0;
    private static int randomDelayTicks = random.nextInt(81) + 20;

    public static BlockPos pauseWaypoint = null;

    private static long lastBrokenBlock = 0L;
    private static long lastXp = 0L;
    private static int blocksBroken = 0;
    private static boolean isBreaking = false;
    private static long startTime = 0L;
    private static double bps = 0.0;
    private static float lockedYaw = 0f;
    private static float lockedPitch = 0f;
    private static long lastFailTime = 0L;
    private static int lockedSlot = -1;
    private static long bpsZeroStartTime = 0L;
    private static final float yawPitchTolerance = 0.5f;
    private static String lockedItemName = null;

    private static boolean isMouseLocked = false;

    private static final List<net.minecraft.block.Block> farmableBlocks = List.of(
            net.minecraft.block.Blocks.WHEAT,
            net.minecraft.block.Blocks.CARROTS,
            net.minecraft.block.Blocks.POTATOES,
            net.minecraft.block.Blocks.CARVED_PUMPKIN,
            net.minecraft.block.Blocks.MELON,
            net.minecraft.block.Blocks.SUGAR_CANE,
            net.minecraft.block.Blocks.CACTUS,
            net.minecraft.block.Blocks.COCOA,
            net.minecraft.block.Blocks.RED_MUSHROOM,
            net.minecraft.block.Blocks.BROWN_MUSHROOM,
            net.minecraft.block.Blocks.NETHER_WART,
            net.minecraft.block.Blocks.ROSE_BUSH,
            net.minecraft.block.Blocks.SUNFLOWER
    );

    private static String currentFail = null;

    public static String getCurrentFail() {
        return currentFail;
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());
        FtEventBus.onEvent(FtEvent.WorldRender.class, event -> {
            onRenderWorldLast(event.worldContext);
        });
        FtEventBus.onEvent(FtEvent.HudRender.class, hud -> {
            render(hud.context);
        });
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((oldWorld, newWorld) -> {
            if (isActive) {
                handleToggle();
                releaseAllKeys();
                currentFail = null;
                Utils.log("World unloaded – macro turned off");
            }
        });
        PacketEvent.registerReceive((packet, connection) -> {
            if (packet instanceof PlaySoundS2CPacket soundPacket) {
                Utils.logSound(soundPacket);
                SoundEvent id = soundPacket.getSound().value();
                if (id != null && id.equals(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP)) {
                    lastXp = System.currentTimeMillis();
                }
            }
        });
    }

    public static boolean isMouseLocked() {
        return isMouseLocked;
    }

    public static void onBlockBroken(BlockPos pos) {
        if (!isActive || mc.world == null || mc.player == null) return;

        var block = mc.world.getBlockState(pos).getBlock();

        if (farmableBlocks.contains(block)) {
            if (startTime == 0L) startTime = System.currentTimeMillis();
            isBreaking = true;
            blocksBroken++;
            lastBrokenBlock = System.currentTimeMillis();
        }
    }

    private static void onClientTick() {
        if (currentFail != null && System.currentTimeMillis() - lastFailTime > 2000) {
            currentFail = null;
        }
        if (mc.currentScreen != null || !Utils.isInSkyblock() || !Config.INSTANCE.funnyToggle) {
            return;
        }

        while (toggleKey.wasPressed()) handleToggle();
        while (pauseKey.wasPressed()) handlePause();
        while (resetKey.wasPressed()) handleReset();

        if (!isActive || isPaused) {
            releaseAllKeys();
            return;
        }

        updateBPS();
        checkFailSafes();
        updateMode();
        holdKeys();
    }

    private static void handleToggle() {
        isActive = !isActive;
        isPaused = false;

        if (!isActive) {
            releaseAllKeys();
            currentMode = "none";
            lastWaypoint = null;
            pauseWaypoint = null;
            ticksOnWaypoint = 0;
            unlockMouse();
        } else if (mc.player != null) {
            lockedYaw = mc.player.getYaw();
            lockedPitch = mc.player.getPitch();
            PlayerInventoryAccessor inv = (PlayerInventoryAccessor) mc.player.getInventory();
            lockedSlot = inv.getSelectedSlot();
            lockedItemName = mc.player.getMainHandStack().getName().getString();
            lockMouse();
            pauseWaypoint = null;
        }
        Utils.log("Macro toggled: " + isActive);
    }

    private static void handlePause() {
        if (!isActive) return;

        isPaused = !isPaused;
        if (isPaused) {
            releaseAllKeys();
            unlockMouse();
            if (mc.player != null) {
                pauseWaypoint = BlockPos.ofFloored(mc.player.getPos());
            }
            Utils.log("Macro paused");
        } else {
            lockMouse();
            pauseWaypoint = null;
            Utils.log("Macro resumed");
        }
    }

    private static void handleReset() {
        if (!isActive || mc.player == null) return;

        lockedYaw = mc.player.getYaw();
        lockedPitch = mc.player.getPitch();
        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) mc.player.getInventory();
        lockedSlot = inv.getSelectedSlot();
        lockedItemName = mc.player.getMainHandStack().getName().getString();
        Utils.log("Reset fake fails");
    }

    private static void updateBPS() {
        if (isBreaking && System.currentTimeMillis() - lastBrokenBlock > 1000) {
            bps = 0.0;
            isBreaking = false;
            blocksBroken = 0;
            startTime = 0L;
        } else if (isBreaking) {
            double secondsElapsed = (System.currentTimeMillis() - startTime) / 1000.0;
            bps = blocksBroken / secondsElapsed;
        }
    }

    private static void checkFailSafes() {
        if (mc.player == null) return;

        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) mc.player.getInventory();
        int currentSlot = inv.getSelectedSlot();
        String currentItemName = mc.player.getMainHandStack().getName().getString();

        if (isActive && System.currentTimeMillis() - lastXp > 5000) {
            mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            currentFail = "NO XP";
            lastFailTime = System.currentTimeMillis();
            Utils.log("NO XP PACKET");
        }

        if (isActive && currentSlot != lockedSlot) {
            mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            currentFail = "Slot changed";
            lastFailTime = System.currentTimeMillis();
            Utils.log("Slot changed");
        }

        if (isActive && !currentItemName.equals(lockedItemName)) {
            mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            currentFail = "Item changed";
            lastFailTime = System.currentTimeMillis();
            Utils.log("Item changed from " + lockedItemName + " to " + currentItemName);
        }

        if (Math.abs(mc.player.getYaw() - lockedYaw) > yawPitchTolerance ||
                Math.abs(mc.player.getPitch() - lockedPitch) > yawPitchTolerance) {
            mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            currentFail ="Yaw/pitch changed";
            lastFailTime = System.currentTimeMillis();
            Utils.log("Yaw/pitch changed");
        }

        if (bps == 0.0) {
            if (bpsZeroStartTime == 0L) {
                bpsZeroStartTime = System.currentTimeMillis();
            } else {
                BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos().add(0, 0.5, 0));
                boolean isOnWaypoint = isNearWaypoints(playerPos, 2);
                long delay = isOnWaypoint ? 5000L : 3000L;
                if (System.currentTimeMillis() - bpsZeroStartTime >= delay) {
                    mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    currentFail ="BPS = 0";
                    lastFailTime = System.currentTimeMillis();
                    Utils.log("BPS = 0 for " + (delay / 1000) + " seconds");
                }
            }
        } else {
            bpsZeroStartTime = 0L;
        }

        if (mc.player.getY() < 63) {
            currentMode = "none";
            releaseAllKeys();
        }
    }

    private static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!Utils.isInSkyblock() || !Config.INSTANCE.funnyToggle) {
            return;
        }

        String text;
        int color;

        if (!isActive) {
            text = "Macro: OFF";
            color = 0xFFFF4444; // red
        } else if (isPaused) {
            text = "Macro: PAUSED";
            color = 0xFFFFFF44; // yellow
        } else {
            text = "Macro: ACTIVE";
            color = 0xFF44FF44; // green
        }

        int x = Config.INSTANCE.macroHudX;
        int y = Config.INSTANCE.macroHudY;

        ctx.drawTextWithShadow(
                mc.textRenderer,
                Text.literal(text),
                x,
                y,
                color
        );
    }

    private static boolean isNearWaypoints(BlockPos pos, int range) {
        List<BlockPos> right = FarmingWaypoints.WAYPOINTS.getOrDefault("right", List.of());
        List<BlockPos> left = FarmingWaypoints.WAYPOINTS.getOrDefault("left", List.of());
        List<BlockPos> warp = FarmingWaypoints.WAYPOINTS.getOrDefault("warp", List.of());

        return right.stream().anyMatch(p -> Math.abs(p.getX() - pos.getX()) <= range && Math.abs(p.getZ() - pos.getZ()) <= range) ||
                left.stream().anyMatch(p -> Math.abs(p.getX() - pos.getX()) <= range && Math.abs(p.getZ() - pos.getZ()) <= range) ||
                warp.stream().anyMatch(p -> Math.abs(p.getX() - pos.getX()) <= range && Math.abs(p.getZ() - pos.getZ()) <= range);
    }

    private static void updateMode() {
        if (mc.player == null) return;

        BlockPos pos = BlockPos.ofFloored(mc.player.getPos().add(0, 0.5, 0));
        List<BlockPos> right = FarmingWaypoints.WAYPOINTS.getOrDefault("right", List.of());
        List<BlockPos> left = FarmingWaypoints.WAYPOINTS.getOrDefault("left", List.of());
        List<BlockPos> warp = FarmingWaypoints.WAYPOINTS.getOrDefault("warp", List.of());

        String targetMode = "none";
        if (right.contains(pos)) targetMode = "right";
        else if (left.contains(pos)) targetMode = "left";
        else if (warp.contains(pos)) targetMode = "warp";

        if (!targetMode.equals("none")) {
            if (lastWaypoint == null || !lastWaypoint.equals(pos)) {
                lastWaypoint = pos;
                ticksOnWaypoint = 1;
            } else {
                ticksOnWaypoint++;
                int requiredTicks = Config.INSTANCE.instaSwitch ? 15 : randomDelayTicks;
                if (ticksOnWaypoint >= requiredTicks) {
                    if (targetMode.equals("warp")) {
                        mc.player.networkHandler.sendChatMessage("/warp garden");
                        currentMode = "none";
                        releaseAllKeys();
                        lastWaypoint = null;
                        ticksOnWaypoint = 0;
                    } else {
                        currentMode = targetMode;
                    }
                    if (!Config.INSTANCE.instaSwitch) {
                        randomDelayTicks = random.nextInt(81) + 20;
                    }
                }
            }
        } else {
            lastWaypoint = null;
            ticksOnWaypoint = 0;
        }
    }

    private static void holdKeys() {
        if (currentMode.equals("none")) return;
        mc.options.forwardKey.setPressed(currentMode.equals("right") || (currentMode.equals("left") && Config.INSTANCE.farmType == 0));
        mc.options.backKey.setPressed(currentMode.equals("left") && (Config.INSTANCE.farmType == 1 || Config.INSTANCE.farmType == 2));
        mc.options.leftKey.setPressed(currentMode.equals("left") && (Config.INSTANCE.farmType == 0 || Config.INSTANCE.farmType == 2));
        mc.options.rightKey.setPressed(currentMode.equals("right"));
        mc.options.attackKey.setPressed(true);
        keysAreHeld = true;
    }

    private static void releaseAllKeys() {
        if (!keysAreHeld) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
        keysAreHeld = false;
    }

    private static void lockMouse() {
        if (!isMouseLocked) {
            isMouseLocked = true;
            Utils.log("Mouse locked");
        }
    }

    private static void unlockMouse() {
        if (isMouseLocked) {
            isMouseLocked = false;
            Utils.log("Mouse unlocked");
        }
    }

    private static void onRenderWorldLast(WorldRenderContext context) {
        if (!Utils.isInSkyblock() || !Config.INSTANCE.funnyWaypoints ||
                mc.player == null || mc.world == null) return;

        MatrixStack matrices = context.matrixStack();
        float tickDelta = context.tickCounter().getDynamicDeltaTicks();

        Vec3d refPos = context.camera().getPos();

        for (var entry : FarmingWaypoints.WAYPOINTS.entrySet()) {
            String type = entry.getKey();
            var list = entry.getValue();

            int color = switch (type.toLowerCase()) {
                case "left"  -> 0xFFFF4444;   // red
                case "right" -> 0xFF44FF44;   // green
                case "warp"  -> 0xFFFFFF44;   // yellow
                default      -> 0xFF4488FF;   // cyan
            };

            for (BlockPos blockPos : list) {
                Vec3d waypointPos = new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                RenderUtils.renderWaypointMarker(matrices, waypointPos, refPos, color, type);
            }
        }

        if (pauseWaypoint != null) {
            Vec3d pausePos = new Vec3d(pauseWaypoint.getX(), pauseWaypoint.getY(), pauseWaypoint.getZ());
            int pauseColor = 0xFF4488FF;

            RenderUtils.renderWaypointMarker(matrices, pausePos, refPos, pauseColor, "Pause");
        }
    }
}