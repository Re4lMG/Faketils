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
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Farming {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random random = new Random();

    private static final KeyBinding toggleKey = Config.INSTANCE.toggleMacro;
    private static final KeyBinding pauseKey = Config.INSTANCE.pauseMacro;
    private static final KeyBinding resetKey = Config.INSTANCE.resetFakeFails;

    private static boolean keysAreHeld = false;

    private static enum RodPhase { IDLE, SWAP_TO_ROD, WAIT_BEFORE_CLICK, CLICK_ROD, WAIT_BEFORE_RESTORE, RESTORE_SLOT, DONE }
    private static RodPhase rodPhase = RodPhase.IDLE;
    private static long rodPhaseStart = 0;
    private static int originalHotbarSlot = -1;
    private static int rodHotbarSlot = -1;

    private static enum EqState { IDLE, OPENING, SEARCHING_ITEMS, PICKUP_CLICKED, PLACE_CLICKED, FINISHED_ITEMS }
    private static EqState eqState = EqState.IDLE;
    private static long eqStateStart = 0L;
    private static int lastProcessedSyncId = -1;
    private static int itemsUsedThisPhase = 0;
    private static long lastClickTime = 0L;
    private static final long MAX_WAIT_PER_ACTION_MS = 4000L;

    private static enum PestPhase { ROOTED, SQUEAKY }
    private static PestPhase currentPestPhase = PestPhase.ROOTED;

    public static boolean isActive = false;
    public static boolean isPaused = false;
    public static String currentMode = "none";

    public static boolean pestsSpawned = false;

    private static BlockPos lastWaypoint = null;
    private static int ticksOnWaypoint = 0;
    private static int randomDelayTicks = random.nextInt(81) + 20;

    public static BlockPos pauseWaypoint = null;

    private static boolean eqActive = false;

    private static long lastBrokenBlock = 0L;
    private static long lastXp = 0L;
    private static long lastPest = 0L;
    private static int blocksBroken = 0;
    private static boolean isBreaking = false;
    private static long pauseTimeMs = 0;
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
        FtEventBus.onEvent(FtEvent.WorldRender.class, event -> onRenderWorldLast(event));
        FtEventBus.onEvent(FtEvent.HudRender.class, hud -> render(hud.context));
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((oldWorld, newWorld) -> {
            if (isActive) {
                handleToggle();
                releaseAllKeys();
                currentFail = null;
                Utils.log("World unloaded, macro turned off");
            }
        });
        PacketEvent.registerReceive((packet, connection) -> {
            if (packet instanceof PlaySoundS2CPacket soundPacket) {
                String soundId = soundPacket.getSound().getIdAsString();
                if (isActive && !isPaused && soundId.equals("minecraft:entity.experience_orb.pickup")) {
                    lastXp = System.currentTimeMillis();
                }
            }
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString().replaceAll("§.", "");
            Pattern pattern = Pattern.compile("ൠ Pest have spawned in Plot\\s*-\\s*(\\d+)");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find() && Config.INSTANCE.pestFarming) {
                int plot = Integer.parseInt(matcher.group(1));
                lastPest = System.currentTimeMillis();
                pestsSpawned = true;
                Utils.log("Pest spawned in plot " + plot);

                if (mc.player != null && isActive) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            mc.player.networkHandler.sendChatMessage("/eq");
                            eqActive = true;
                            eqState = EqState.OPENING;
                            eqStateStart = System.currentTimeMillis();
                            currentPestPhase = PestPhase.ROOTED;
                            itemsUsedThisPhase = 0;
                            lastProcessedSyncId = -1;
                        } catch (InterruptedException ignored) {}
                    }).start();
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
        if (!Config.INSTANCE.funnyToggle) return;
        if (!Utils.isInGarden()) return;

        updatePestsTimer();

        while (toggleKey.wasPressed()) handleToggle();
        while (pauseKey.wasPressed()) handlePause();
        while (resetKey.wasPressed()) handleReset();

        if (!isActive || isPaused) {
            releaseAllKeys();
            return;
        }

        handleRodSequence();

        if (eqActive) {
            handleEqSequence();
            return;
        }

        updateBPS();
        checkFailSafes();
        updateMode();
        holdKeys();
    }

    private static void handleEqSequence() {
        if (mc.player == null || mc.player.currentScreenHandler == null || mc.player.currentScreenHandler.syncId == 0) {
            if (eqState == EqState.OPENING && System.currentTimeMillis() - eqStateStart > 12000) {
                Utils.log("EQ menu timeout - aborting");
                eqActive = false;
                eqState = EqState.IDLE;
            }
            return;
        }

        ScreenHandler handler = mc.player.currentScreenHandler;
        long now = System.currentTimeMillis();

        int totalSlots = handler.slots.size();
        int playerInvStart = totalSlots - 36;

        int delayMs = 400 + random.nextInt(600);
        if (now - lastClickTime < delayMs) {
            return;
        }

        boolean didAction = false;

        if (eqState == EqState.OPENING) {
            eqState = EqState.SEARCHING_ITEMS;
            eqStateStart = now;
            Utils.log("EQ opened - starting item search (delay-based)");
            return;
        }

        if (eqState == EqState.SEARCHING_ITEMS) {
            Utils.log("Scanning for next " + (currentPestPhase == PestPhase.ROOTED ? "Rooted" : "Squeaky") + " item...");

            for (int slot = playerInvStart; slot < totalSlots; slot++) {
                ItemStack stack = handler.getSlot(slot).getStack();
                if (stack.isEmpty()) continue;

                String name = stack.getName().getString().toLowerCase().replaceAll("§.", "").trim();
                boolean isMatch = currentPestPhase == PestPhase.ROOTED
                        ? name.contains("rooted")
                        : name.contains("squeaky pest");

                if (isMatch) {
                    mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
                    lastClickTime = now;
                    eqState = EqState.PICKUP_CLICKED;
                    eqStateStart = now;
                    itemsUsedThisPhase++;
                    Utils.log("Pickup clicked on " + (currentPestPhase == PestPhase.ROOTED ? "Rooted" : "Squeaky") + " item in slot " + slot);
                    didAction = true;
                    break;
                }
            }

            if (!didAction) {
                eqState = EqState.FINISHED_ITEMS;
                eqStateStart = now;
                Utils.log("No more matching items found - preparing to close");
            }
        }
        else if (eqState == EqState.PICKUP_CLICKED) {
            if (now - eqStateStart >= 400 + random.nextInt(600)) {
                int placeSlot = -1;
                for (int i = 0; i < playerInvStart; i++) {
                    if (handler.getSlot(i).getStack().isEmpty()) {
                        placeSlot = i;
                        break;
                    }
                }
                if (placeSlot == -1) {
                    for (int i = playerInvStart; i < totalSlots; i++) {
                        if (handler.getSlot(i).getStack().isEmpty()) {
                            placeSlot = i;
                            break;
                        }
                    }
                }

                if (placeSlot != -1) {
                    mc.interactionManager.clickSlot(handler.syncId, placeSlot, 0, SlotActionType.PICKUP, mc.player);
                    lastClickTime = now;
                    eqState = EqState.SEARCHING_ITEMS;
                    eqStateStart = now;
                    Utils.log("Place clicked in slot " + placeSlot + " - back to searching");
                    didAction = true;
                } else {
                    Utils.log("No empty slot to place - closing early");
                    mc.player.closeHandledScreen();
                    eqState = EqState.FINISHED_ITEMS;
                }
            }
        }

        if (eqState == EqState.FINISHED_ITEMS && now - eqStateStart > 400 + random.nextInt(300)) {
            mc.player.closeHandledScreen();
            Utils.log("Closing EQ after finishing items");
            startRodSequence(now);
        }
    }

    private static void startRodSequence(long now) {
        PlayerInventoryAccessor inventory = (PlayerInventoryAccessor) mc.player.getInventory();
        int rodSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof FishingRodItem) {
                rodSlot = i;
                break;
            }
        }

        if (rodSlot == -1) {
            Utils.log("No fishing rod found in hotbar");
            eqActive = false;
            eqState = EqState.IDLE;
            return;
        }

        originalHotbarSlot = inventory.getSelectedSlot();
        rodHotbarSlot = rodSlot;

        rodPhase = RodPhase.SWAP_TO_ROD;
        rodPhaseStart = now;
        eqActive = true;

        Utils.log("EQ finished (" + itemsUsedThisPhase + " items used) → starting rod sequence");
    }

    private static void handleRodSequence() {
        if (rodPhase == RodPhase.IDLE || mc.player == null) return;

        long now = System.currentTimeMillis();
        PlayerInventory inventory = mc.player.getInventory();

        switch (rodPhase) {
            case SWAP_TO_ROD:
                if (now - rodPhaseStart >= 50 + random.nextInt(101)) {
                    inventory.setSelectedSlot(rodHotbarSlot);
                    rodPhase = RodPhase.WAIT_BEFORE_CLICK;
                    rodPhaseStart = now;
                }
                break;

            case WAIT_BEFORE_CLICK:
                if (now - rodPhaseStart >= 100 + random.nextInt(151)) {
                    Utils.simulateUseItem(mc.interactionManager);
                    rodPhase = RodPhase.WAIT_BEFORE_RESTORE;
                    rodPhaseStart = now;
                }
                break;

            case WAIT_BEFORE_RESTORE:
                if (now - rodPhaseStart >= 50 + random.nextInt(101)) {
                    rodPhase = RodPhase.RESTORE_SLOT;
                    rodPhaseStart = now;
                }
                break;

            case RESTORE_SLOT:
                if (originalHotbarSlot >= 0 && originalHotbarSlot < 9) {
                    inventory.setSelectedSlot(originalHotbarSlot);
                    Utils.log("Restored original slot: " + originalHotbarSlot);
                } else {
                    Utils.log("Invalid original slot " + originalHotbarSlot + " - skipping restore (likely reset by server)");
                }
                rodPhase = RodPhase.DONE;
                rodPhaseStart = now;
                break;

            case DONE:
                if (now - rodPhaseStart >= 100) {
                    rodPhase = RodPhase.IDLE;
                    eqActive = false;
                    eqState = EqState.IDLE;
                    rodHotbarSlot = -1;

                    if (mc.player != null && originalHotbarSlot >= 0 && originalHotbarSlot < 9) {
                        mc.player.getInventory().setSelectedSlot(originalHotbarSlot);
                        Utils.log("Final slot restore: " + originalHotbarSlot);
                    }

                    if (currentPestPhase == PestPhase.ROOTED) {
                        Utils.log("Rooted pest items handled → pausing macro");
                        currentPestPhase = PestPhase.SQUEAKY;
                        handlePause();
                    } else {
                        Utils.log("Squeaky pest items handled → resuming normal farming");
                    }

                    originalHotbarSlot = -1;
                }
                break;

            default:
                rodPhase = RodPhase.IDLE;
                break;
        }
    }

    private static void updatePestsTimer() {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();

        if (pestsSpawned && now - lastPest > 131000 && Config.INSTANCE.pestFarming) {
            pestsSpawned = false;
            releaseAllKeys();
            mc.player.networkHandler.sendChatMessage("/eq");
            eqActive = true;
            eqState = EqState.OPENING;
            eqStateStart = now;
            currentPestPhase = PestPhase.SQUEAKY;
            itemsUsedThisPhase = 0;
            lastProcessedSyncId = -1;
            mc.player.sendMessage(Text.literal("§7[§bFaketils§7] §ePest Timer ran out!"), false);
            Utils.log("135s timer reached → starting Squeaky phase");
        }
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
            lastXp = 0;
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
            pauseTimeMs = System.currentTimeMillis();

            if (mc.player != null) {
                pauseWaypoint = BlockPos.ofFloored(mc.player.getEntityPos());
                if (Config.INSTANCE.rewarpOnPause) {
                    mc.player.networkHandler.sendChatMessage("/setspawn");
                    mc.player.sendMessage(Text.literal("§7[§bFaketils§7] §eReWarp point set!"), false);
                }
            }
            Utils.log("Macro paused");
        } else {
            lockMouse();
            pauseWaypoint = null;
            lastXp = 0;

            long pausedForMs = System.currentTimeMillis() - pauseTimeMs;
            double pausedForSeconds = pausedForMs / 1000.0;

            if (pausedForSeconds >= 9 && Config.INSTANCE.rewarpOnPause) {
                mc.player.networkHandler.sendChatMessage("/warp garden");
                new Thread(() -> {
                    try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                    mc.player.getAbilities().flying = false;
                    mc.player.sendAbilitiesUpdate();
                }).start();
                mc.player.sendMessage(Text.literal("§7[§bFaketils§7] §eWarping back!"), false);
                Utils.log("Macro resumed (warp garden)");
            } else {
                Utils.log("Macro resumed");
            }
        }
    }

    private static void handleReset() {
        if (!isActive || mc.player == null) return;
        lockedYaw = mc.player.getYaw();
        lockedPitch = mc.player.getPitch();
        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) mc.player.getInventory();
        lockedSlot = inv.getSelectedSlot();
        lockedItemName = mc.player.getMainHandStack().getName().getString();
        lastXp = 5000;
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
            currentFail = "Yaw/pitch changed";
            lastFailTime = System.currentTimeMillis();
            Utils.log("Yaw/pitch changed");
        }

        if (bps == 0.0) {
            if (bpsZeroStartTime == 0L) {
                bpsZeroStartTime = System.currentTimeMillis();
            } else {
                BlockPos playerPos = BlockPos.ofFloored(mc.player.getEntityPos().add(0, 0.5, 0));
                boolean isOnWaypoint = isNearWaypoints(playerPos, 2);
                long delay = isOnWaypoint ? 5000L : 3000L;
                if (System.currentTimeMillis() - bpsZeroStartTime >= delay) {
                    mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    currentFail = "BPS = 0";
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
        if (mc.player == null) return;
        if (!Utils.isInSkyblock() || !Config.INSTANCE.funnyToggle) return;
        if (!Utils.isInGarden()) return;

        String text;
        int color;

        if (!isActive) {
            text = "Macro: OFF";
            color = 0xFFFF4444;
        } else if (isPaused) {
            text = "Macro: PAUSED";
            color = 0xFFFFFF44;
        } else {
            text = "Macro: ACTIVE";
            color = 0xFF44FF44;
        }

        int x = Config.INSTANCE.macroHudX;
        int y = Config.INSTANCE.macroHudY;

        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(text), x, y, color);
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

        BlockPos pos = BlockPos.ofFloored(mc.player.getEntityPos().add(0, 0.5, 0));
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

    private static void onRenderWorldLast(FtEvent.WorldRender event) {
        if (!Utils.isInSkyblock() || !Config.INSTANCE.funnyWaypoints || mc.player == null || mc.world == null || !Utils.isInGarden()) {
            return;
        }

        Vec3d cameraPos = event.camera.getPos();

        for (var entry : FarmingWaypoints.WAYPOINTS.entrySet()) {
            String type = entry.getKey();
            var list = entry.getValue();

            int color = switch (type.toLowerCase()) {
                case "left"  -> 0xFFFF4444;
                case "right" -> 0xFF44FF44;
                case "warp"  -> 0xFFFFFF44;
                default      -> 0xFF4488FF;
            };

            for (BlockPos blockPos : list) {
                RenderUtils.renderWaypointMarker(
                        new Vec3d(blockPos.getX()+0.5, blockPos.getY(), blockPos.getZ()+0.5),
                        cameraPos,
                        color,
                        type,
                        event
                );
            }
        }

        if (pauseWaypoint != null) {
            RenderUtils.renderWaypointMarker(
                    new Vec3d(pauseWaypoint.getX()-0.5, pauseWaypoint.getY(), pauseWaypoint.getZ()-0.5),
                    cameraPos,
                    0xFF4488FF,
                    "Pause",
                    event
            );
        }
    }
}