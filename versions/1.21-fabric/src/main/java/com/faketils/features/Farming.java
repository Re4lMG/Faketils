package com.faketils.features;

import com.faketils.config.Config;
import com.faketils.events.*;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
    private static int pendingDoubleJumpTicks = 0;

    private static enum WPhase { OPENING, CLICKING, DONE }

    private static WPhase wPhase = WPhase.OPENING;

    private static enum WardrobePhase { IDLE, OPEN_SENT, WAIT_FOR_OPEN, CLICKING_SLOT, WAIT_AFTER_CLICK, CLOSING, DONE }
    private static WardrobePhase wardrobePhase = WardrobePhase.IDLE;
    private static long wardrobePhaseStart = 0L;
    private static boolean wardrobeSuccess = false;

    private static enum EqState { IDLE, OPENING, WAIT_AFTER_OPEN, SEARCHING_ITEMS, PICKUP_CLICKED, PLACE_CLICKED, FINISHED_ITEMS }
    private static EqState eqState = EqState.IDLE;
    private static long eqStateStart = 0L;
    private static int lastProcessedSyncId = -1;
    private static int itemsUsedThisPhase = 0;
    private static long lastClickTime = 0L;
    private static final long MAX_WAIT_PER_ACTION_MS = 4000L;

    private enum SprayPhase {
        IDLE,
        SWAP_TO_SPRAY,
        WAIT_BEFORE_USE,
        USE,
        WAIT_BEFORE_RESTORE,
        RESTORE_SLOT,
        DONE
    }
    private static boolean isSpraying = false;
    private static long sprayNoneDetectedTime = -1L;
    private static SprayPhase sprayPhase = SprayPhase.IDLE;
    private static long sprayPhaseStart = 0L;
    private static int sprayHotbarSlot = -1;
    private static int originalSpraySlot = -1;

    private static int emptyScans = 0;

    private static enum PestPhase { ROOTED, SQUEAKY }
    private static PestPhase currentPestPhase = PestPhase.ROOTED;

    private static int lastSeenSyncId = -1;
    private static long windowOpenedAt = 0L;
    private static boolean windowReady = false;

    public static boolean isActive = false;
    public static boolean isPaused = false;
    public static String currentMode = "none";

    private static boolean killingPests = false;
    private static Vec3d currentPestTarget = null;
    public static boolean pestsSpawned = false;

    private static BlockPos lastWaypoint = null;
    private static int ticksOnWaypoint = 0;
    private static int randomDelayTicks = random.nextInt(81) + 20;

    public static BlockPos pauseWaypoint = null;

    private static long wPhaseStart = 0;

    private static int wardrobeSlot = 0;
    private static boolean wardrobeClicked = false;
    public static int originalPestKillSlot = -1;
    public static int vacuumSlot = -1;
    private static int movementBlockTicks = 0;

    private static boolean eqActive = false;

    private static Vec3d pestOffset = Vec3d.ZERO;
    private static long lastOffsetChange = 0L;
    private static final long OFFSET_CHANGE_INTERVAL_MS = 900L + random.nextInt(600);
    private static final float OFFSET_MAX_HORIZONTAL = 0.5f;
    private static final float OFFSET_MAX_VERTICAL   = 0.5f;
    private static long nextClickTime = 0;

    private static int plot = 0;
    private static long lastBrokenBlock = 0L;
    private static long lastXp = 0L;
    private static long lastPest = 0L;
    private static int blocksBroken = 0;
    private static boolean isBreaking = false;
    private static long pauseTimeMs = 0;
    private static long startTime = 0L;
    private static double bps = 0.0;
    private static boolean waitingForTrades = false;
    private static long lastTradesCommand = 0;
    private static float lockedYaw = 0f;
    private static float lockedPitch = 0f;
    private static long lastFailTime = 0L;
    private static int lockedSlot = -1;
    private static long bpsZeroStartTime = 0L;
    private static final float yawPitchTolerance = 0.5f;
    private static String lockedItemName = null;
    private static final Set<ArmorStandEntity> ignoredPests = new HashSet<>();
    private static long ignoredResetTime = 0;

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
                if (killingPests && soundId.equals("minecraft:entity.silverfish.death")) {
                    if (currentPestTarget != null && mc.world != null) {
                        for (Entity entity : mc.world.getEntities()) {
                            if (entity instanceof ArmorStandEntity armorStand) {
                                Vec3d pos = armorStand.getEntityPos().add(0, 1.15, 0);
                                if (pos.distanceTo(currentPestTarget) < 2.5) {
                                    //ignoredPests.add(armorStand);
                                    ignoredResetTime = System.currentTimeMillis();
                                    break;
                                }
                            }
                        }
                    }

                    //currentPestTarget = null;
                    Utils.log("§cSilverfish death sound detected → skipping pest");
                }
            }
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString().replaceAll("§.", "");
            Pattern pattern = Pattern.compile("ൠ Pest have spawned in Plot\\s*-\\s*(\\d+)");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find() && Config.INSTANCE.pestFarming) {
                plot = Integer.parseInt(matcher.group(1));
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

        handleWindowId();
        updatePestsTimer();

        while (toggleKey.wasPressed()) handleToggle();
        while (pauseKey.wasPressed()) handlePause();
        while (resetKey.wasPressed()) handleReset();

        if (killingPests && isPaused) {
            handleKilling();
            if (currentPestTarget != null && mc.player != null) {
                double distance = mc.player.getEntityPos().distanceTo(currentPestTarget);
                boolean inSweetSpot = distance < 12.0;
                //swapToInfiniVacuum();
                mc.options.useKey.setPressed(inSweetSpot);
            }
        }

        if (!isActive || (isPaused && !killingPests)) {
            //releaseAllKeys();
            return;
        }

        handleRodSequence();
        handleWardrobeSequence();

        if (eqActive && !isSpraying) {
            //releaseAllKeys();
            handleEqSequence();
            return;
        }

        handleTradesScreen();

        if (mc.currentScreen != null) return;

        handleSpray();
        handleSpraySequence();

        if (isSpraying) {
            return;
        }

        if (movementBlockTicks > 0) {
            movementBlockTicks--;
            releaseAllKeys();
        }

        updateBPS();
        checkFailSafes();
        checkInventoryForSell();
        updateMode();
        if (movementBlockTicks == 0 && !killingPests) {
            holdKeys();
        }
    }

    private static void handleJump() {
        if (pendingDoubleJumpTicks > 0 && mc.player != null) {
            if (mc.player.isOnGround()) {
                mc.options.jumpKey.setPressed(true);
                mc.player.jump();
                mc.options.jumpKey.setPressed(false);

                pendingDoubleJumpTicks = -2;
            } else {
                pendingDoubleJumpTicks--;
            }
            return;
        }

        if (pendingDoubleJumpTicks < 0 && mc.player != null) {
            mc.options.jumpKey.setPressed(true);
            mc.player.jump();
            mc.options.jumpKey.setPressed(false);

            pendingDoubleJumpTicks = 0;
        }
    }

    private static void handleWindowId() {
        if (eqActive && mc.player != null && mc.player.currentScreenHandler != null) {
            int syncId = mc.player.currentScreenHandler.syncId;

            if (syncId != 0 && syncId != lastSeenSyncId) {
                lastSeenSyncId = syncId;
                windowOpenedAt = System.currentTimeMillis();
                windowReady = false;

                Utils.log("EQ window updated → syncId = " + syncId);
            }

            if (!windowReady && System.currentTimeMillis() - windowOpenedAt >= 100) {
                windowReady = true;
                Utils.log("EQ window ready for interaction");
            }
        }
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

        if (!windowReady) {
            return;
        }

        ScreenHandler handler = mc.player.currentScreenHandler;

        if (!handler.getCursorStack().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        int delayMs = Config.INSTANCE.swapDelay + random.nextInt(100);
        if (now - lastClickTime < delayMs) {
            return;
        }

        boolean didAction = false;

        if (eqState == EqState.OPENING && isActive) {
            eqState = EqState.WAIT_AFTER_OPEN;
            eqStateStart = now;
            Utils.log("EQ menu detected open - waiting initial delay before scanning");
            return;
        }

        if (eqState == EqState.WAIT_AFTER_OPEN) {
            if (now - eqStateStart >= delayMs) {
                eqState = EqState.SEARCHING_ITEMS;
                eqStateStart = now;
                Utils.log("Initial delay finished - now starting item search");
            }
            return;
        }

        if (eqState == EqState.SEARCHING_ITEMS) {
            Utils.log("Scanning for next " + (currentPestPhase == PestPhase.ROOTED ? "Rooted" : "Squeaky") + " item...");

            for (int i = 0; i < handler.slots.size(); i++) {
                Slot slot = handler.slots.get(i);

                if (!(slot.inventory instanceof PlayerInventory)) continue;

                ItemStack stack = slot.getStack();
                if (stack.isEmpty()) continue;

                String name = stack.getName().getString().toLowerCase().replaceAll("§.", "").trim();
                boolean isMatch = currentPestPhase == PestPhase.ROOTED
                        ? name.contains("rooted")
                        : name.contains("squeaky pest");

                if (isMatch) {
                    mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
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
            if (now - eqStateStart >= Config.INSTANCE.swapDelay + random.nextInt(100)) {
                int placeSlot = -1;
                for (int i = 0; i < handler.slots.size(); i++) {
                    Slot slot = handler.slots.get(i);

                    if (!(slot.inventory instanceof PlayerInventory)) {
                        continue;
                    }

                    if (slot.getStack().isEmpty()) {
                        placeSlot = i;
                        break;
                    }
                }

                if (placeSlot != -1) {
                    mc.interactionManager.clickSlot(handler.syncId, placeSlot, 0, SlotActionType.CLONE, mc.player);
                    lastClickTime = now;
                    eqState = EqState.SEARCHING_ITEMS;
                    eqStateStart = now;
                    Utils.log("Place clicked in slot " + placeSlot + " - back to searching");
                    didAction = true;
                } else {
                    Utils.log("No empty slot to place - closing early");
                    eqState = EqState.FINISHED_ITEMS;
                }
            }
        }

        if (eqState == EqState.FINISHED_ITEMS && now - eqStateStart > 500) {
            mc.player.closeHandledScreen();
            windowReady = false;
            lastSeenSyncId = -1;
            Utils.log("Closing EQ after finishing items");
            if (Config.INSTANCE.petSwapType == 0) startRodSequence(now);
            if (Config.INSTANCE.petSwapType == 1) {
                if (currentPestPhase == PestPhase.ROOTED) {
                    wardrobeSlot = Config.INSTANCE.wardrobeSlot;
                }
                if (currentPestPhase == PestPhase.SQUEAKY) {
                    wardrobeSlot = Config.INSTANCE.wardrobeSlotOld;
                }
                mc.player.networkHandler.sendChatMessage("/wardrobe");
                eqState = EqState.IDLE;
                wardrobePhase = WardrobePhase.OPEN_SENT;
                wardrobePhaseStart = now;
                wardrobeSuccess = false;
                Utils.log("Sent /wardrobe command → starting wardrobe phase");
            }
        }
    }

    private static void handleWardrobeSequence() {
        if (wardrobePhase == WardrobePhase.IDLE) return;
        if (mc.player == null || mc.player.currentScreenHandler == null) return;

        long now = System.currentTimeMillis();
        int syncId = mc.player.currentScreenHandler.syncId;

        if (now - wardrobePhaseStart > 12000) {
            Utils.log("Wardrobe sequence timeout → aborting");
            mc.player.closeHandledScreen();
            resetWardrobeState();
            return;
        }

        switch (wardrobePhase) {
            case OPEN_SENT:
                if (now - wardrobePhaseStart > 400 + random.nextInt(300)) {
                    wardrobePhase = WardrobePhase.CLICKING_SLOT;
                    wardrobePhaseStart = now;
                }
                break;

            case WAIT_FOR_OPEN:
                if (syncId != 0 && syncId != lastSeenSyncId) {
                    lastSeenSyncId = syncId;
                    wardrobePhase = WardrobePhase.CLICKING_SLOT;
                    wardrobePhaseStart = now;
                    Utils.log("Wardrobe window detected (syncId=" + syncId + ")");
                } else if (now - wardrobePhaseStart > 5000) {
                    Utils.log("Wardrobe did not open in time → aborting");
                    resetWardrobeState();
                }
                break;

            case CLICKING_SLOT:
                if (now - wardrobePhaseStart < 300 + random.nextInt(400)) return;

                int targetPreset = wardrobeSlot;

                for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
                    ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
                    if (stack.isEmpty()) continue;

                    String name = stack.getName().getString()
                            .replaceAll("§.", "")
                            .toLowerCase()
                            .trim();

                    if (name.contains("slot") && name.contains(String.valueOf(targetPreset))) {
                        mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.PICKUP, mc.player);
                        Utils.log("Clicked wardrobe preset '" + targetPreset + "' at slot index " + i);
                        wardrobeSuccess = true;
                        wardrobePhase = WardrobePhase.WAIT_AFTER_CLICK;
                        wardrobePhaseStart = now;
                        return;
                    }
                }

                if (now - wardrobePhaseStart > 4000) {
                    Utils.log("Could not find wardrobe slot " + targetPreset + " → aborting");
                    resetWardrobeState();
                }
                break;

            case WAIT_AFTER_CLICK:
                if (now - wardrobePhaseStart > 350 + random.nextInt(250)) {
                    wardrobePhase = WardrobePhase.CLOSING;
                    wardrobePhaseStart = now;
                }
                break;

            case CLOSING:
                mc.player.closeHandledScreen();
                Utils.log("Closing wardrobe");

                wardrobePhase = WardrobePhase.DONE;
                wardrobePhaseStart = now;
                break;

            case DONE:
                if (now - wardrobePhaseStart > 150 + random.nextInt(200)) {
                    if (wardrobeSuccess) {
                        rodPhase = RodPhase.DONE;
                        rodPhaseStart = now;
                        Utils.log("Wardrobe → rod sequence started");
                    } else {
                        Utils.log("Wardrobe failed → rod sequence skipped");
                    }
                    resetWardrobeState();
                }
                break;
        }
    }

    private static void resetWardrobeState() {
        wardrobePhase = WardrobePhase.IDLE;
        wardrobePhaseStart = 0L;
        wardrobeSuccess = false;
        lastSeenSyncId = -1;
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
                if (originalHotbarSlot >= 0 && originalHotbarSlot < 9 && now - rodPhaseStart >= 50 + random.nextInt(101)) {
                    inventory.setSelectedSlot(originalHotbarSlot);
                    Utils.log("Restored original slot: " + originalHotbarSlot);
                } else {
                    Utils.log("Invalid original slot " + originalHotbarSlot + " - skipping restore");
                }
                rodPhase = RodPhase.DONE;
                rodPhaseStart = now;
                break;

            case DONE:
                if (now - rodPhaseStart >= 100) {
                    rodPhase = RodPhase.IDLE;
                    eqState = EqState.IDLE;
                    rodHotbarSlot = -1;

                    if (mc.player != null && originalHotbarSlot >= 0 && originalHotbarSlot < 9) {
                        mc.player.getInventory().setSelectedSlot(originalHotbarSlot);
                        Utils.log("Final slot restore: " + originalHotbarSlot);
                    }

                    if (currentPestPhase == PestPhase.ROOTED) {
                        Utils.log("Rooted pest items handled → pausing macro");
                        handlePause();
                        movementBlockTicks = 10;
                        releaseAllKeys();
                        currentPestPhase = PestPhase.SQUEAKY;
                        new Thread(() -> {
                            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                            mc.player.networkHandler.sendChatMessage("/tptoplot " + plot);
                            if (Config.INSTANCE.pestKilling) swapToInfiniVacuum();
                            pendingDoubleJumpTicks = 10;
                            try { Thread.sleep(350); } catch (InterruptedException ignored) {}
                            if (Config.INSTANCE.pestKilling) handleKilling();
                            if (Config.INSTANCE.pestKilling) {
                                Vec3d pos = mc.player.getEntityPos();
                                Vec3d look = mc.player.getRotationVec(1.0F).normalize();
                                Vec3d target = pos.add(look.multiply(32)).add(0, 4, 0);
                                //FlyHandler.setTarget(target);
                            }
                            if (Config.INSTANCE.pestKilling) killingPests = true;
                        }).start();
                    } else {
                        Utils.log("Squeaky pest items handled → resuming normal farming");
                    }

                    originalHotbarSlot = -1;
                    new Thread(() -> {
                        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                        eqActive = false;
                    }).start();
                }
                break;

            default:
                rodPhase = RodPhase.IDLE;
                break;
        }
    }

    private static boolean swapToInfiniVacuum() {
        if (mc.player == null) return false;
        if (!Config.INSTANCE.pestKilling) return false;

        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) mc.player.getInventory();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            String name = stack.getName().getString()
                    .replaceAll("§.", "")
                    .replaceAll("[™®©]", "")
                    .toLowerCase()
                    .trim();

            if (name.contains("infinivacuum")) {
                vacuumSlot = i;
                break;
            }
        }

        if (vacuumSlot == -1) {
            Utils.log("§cNo InfiniVacuum™ found in hotbar → using current item");
            return false;
        }

        originalPestKillSlot = inv.getSelectedSlot();
        inv.setSelectedSlot(vacuumSlot);
        Utils.log("Swapped to InfiniVacuum™ (slot " + (vacuumSlot + 1) + ")");
        return true;
    }

    private static void updatePestOffset() {
        long now = System.currentTimeMillis();
        if (now - lastOffsetChange < OFFSET_CHANGE_INTERVAL_MS) {
            return;
        }

        float dx = (random.nextFloat() * 2 - 1) * OFFSET_MAX_HORIZONTAL;
        float dy = (random.nextFloat() * 2 - 1) * OFFSET_MAX_VERTICAL;
        float dz = (random.nextFloat() * 2 - 1) * OFFSET_MAX_HORIZONTAL;

        pestOffset = new Vec3d(dx, dy, dz);
        lastOffsetChange = now;

        if (random.nextInt(20) == 0) {
            Utils.log("New pest offset: " + String.format("%.2f, %.2f, %.2f", dx, dy, dz));
        }
    }

    public static void handleKilling() {
        if (mc.player == null || mc.world == null || !Utils.isInGarden()) return;
        if (!Config.INSTANCE.pestKilling) return;

        if (System.currentTimeMillis() - ignoredResetTime > 2000) {
            ignoredPests.clear();
        }

        if (TabListParser.getTabLines().stream().anyMatch(s -> s.contains("Alive: 0"))) {
            if (killingPests) {
                killingPests = false;
                if (originalPestKillSlot >= 0 && originalPestKillSlot < 9) {
                    mc.player.getInventory().setSelectedSlot(originalPestKillSlot);
                    Utils.log("Restored original hotbar slot after pest killing: " + (originalPestKillSlot + 1));
                }
                originalPestKillSlot = -1;
                FlyHandler.stop();
                RotationHandler.reset();
                handlePause();
                currentPestTarget = null;
                pestOffset = Vec3d.ZERO;
                lastOffsetChange = 0L;
                Utils.log("All pests killed → resuming normal farming");
            }
            return;
        }

        if (!killingPests) return;

        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && living.isDead()) {

                ArmorStandEntity closestStand = null;
                double closestDist = Double.MAX_VALUE;

                for (Entity e : mc.world.getEntities()) {
                    if (e instanceof ArmorStandEntity stand) {

                        PestHelper.Pest pest = PestHelper.getPestFromHead(stand);
                        if (pest == null) continue;

                        double dist = stand.squaredDistanceTo(entity);

                        if (dist < closestDist) {
                            closestDist = dist;
                            closestStand = stand;
                        }
                    }
                }

                if (closestStand != null && closestDist < 2) {
                    ignoredPests.add(closestStand);
                    ignoredResetTime = System.currentTimeMillis();
                }
            }
            if (entity instanceof ArmorStandEntity armorStand) {
                if (ignoredPests.contains(armorStand)) continue;
                PestHelper.Pest pest = PestHelper.getPestFromHead(armorStand);
                if (pest != null) {
                    Vec3d baseTarget = armorStand.getEntityPos().add(0, 1.15, 0);
                    //updatePestOffset();
                    Vec3d finalTarget = baseTarget.add(pestOffset);
                    double distSq = playerPos.squaredDistanceTo(finalTarget);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closest = finalTarget;
                    }
                }
            }
        }

        if (closest != null) {
            currentPestTarget = closest;
            FlyHandler.setTarget(closest);
        } else {
            FlyHandler.stop();
            currentPestTarget = null;
        }
    }

    private static void updatePestsTimer() {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();

        if (pestsSpawned && now - lastPest > Config.INSTANCE.pestTime * 1000L && Config.INSTANCE.pestFarming) {
            pestsSpawned = false;
            if (isActive && !isPaused) {
                releaseAllKeys();
                mc.player.networkHandler.sendChatMessage("/eq");
            }
            eqActive = true;
            eqState = EqState.OPENING;
            eqStateStart = now;
            currentPestPhase = PestPhase.SQUEAKY;
            itemsUsedThisPhase = 0;
            lastProcessedSyncId = -1;
            mc.player.sendMessage(Text.literal("§7[§bFaketils§7] §ePest Timer ran out!"), false);
            Utils.log(Config.INSTANCE.pestTime + "s timer reached → starting Squeaky phase");
        }
    }

    private static void handleToggle() {
        isActive = !isActive;
        isPaused = false;

        if (!isActive) {
            releaseAllKeys();
            FlyHandler.stop();
            RotationHandler.reset();
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
            movementBlockTicks = 10;
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
            lastXp = 0;

            boolean shouldWarp = false;

            if (Config.INSTANCE.rewarpOnPause && pauseWaypoint != null && mc.player != null) {
                BlockPos playerPos = BlockPos.ofFloored(mc.player.getEntityPos());

                int dx = Math.abs(playerPos.getX() - pauseWaypoint.getX());
                int dz = Math.abs(playerPos.getZ() - pauseWaypoint.getZ());

                if (dx > 1 || dz > 1) {
                    shouldWarp = true;
                }
            }

            pauseWaypoint = null;

            if (shouldWarp) {
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

    private static float getAngleDifference(float a, float b) {
        float diff = (a - b) % 360.0F;
        if (diff > 180.0F) diff -= 360.0F;
        if (diff < -180.0F) diff += 360.0F;
        return Math.abs(diff);
    }

    private static void checkFailSafes() {
        if (mc.player == null) return;

        if (killingPests) {
            lastXp = System.currentTimeMillis();
            bpsZeroStartTime = 0L;
            return;
        }

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

        if (getAngleDifference(mc.player.getYaw(), lockedYaw) > yawPitchTolerance ||
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
        if (!Utils.isInSkyblock() || mc.player == null || mc.world == null || !Utils.isInGarden()) {
            return;
        }

        Vec3d cameraPos = event.camera.getPos();
        if (Config.INSTANCE.funnyWaypoints) {
            for (var entry : FarmingWaypoints.WAYPOINTS.entrySet()) {
                String type = entry.getKey();
                var list = entry.getValue();

                int color = switch (type.toLowerCase()) {
                    case "left" -> 0xFFFF4444;
                    case "right" -> 0xFF44FF44;
                    case "warp" -> 0xFFFFFF44;
                    default -> 0xFF4488FF;
                };

                for (BlockPos blockPos : list) {
                    RenderUtils.renderWaypointMarker(
                            new Vec3d(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5),
                            cameraPos,
                            color,
                            type,
                            event
                    );
                }
            }
        }

        if (pauseWaypoint != null) {
            RenderUtils.renderWaypointMarker(
                    new Vec3d(pauseWaypoint.getX()+0.5, pauseWaypoint.getY(), pauseWaypoint.getZ()+0.5),
                    cameraPos,
                    0xFF4488FF,
                    "Pause",
                    event
            );
        }
    }

    private static void checkInventoryForSell() {
        if (!Config.INSTANCE.autoSellJunk) return;
        if (mc.player == null) return;
        if (!isActive || isPaused) return;
        if (eqActive) return;
        if (killingPests) return;
        if (waitingForTrades) return;

        PlayerInventory inv = mc.player.getInventory();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;

            String name = stack.getName().getString()
                    .replaceAll("§.", "")
                    .toLowerCase()
                    .trim();

            if (name.endsWith("vinyl")
                    || name.equals("overclocker 3000")
                    || name.equals("chirping stereo")
                    || name.equals("beady eyes")
                    || name.equals("atmospheric filter")
                    || name.equals("clipped wings")
                    || name.equals("wriggling larva")
                    || name.startsWith("bookworm")
                    || name.equals("squeaky toy")
                    || name.equals("mantid claw")) {

                mc.player.networkHandler.sendChatMessage("/trades");
                waitingForTrades = true;

                return;
            }
        }
    }

    private static void handleTradesScreen() {
        if (!waitingForTrades) return;
        if (mc.player == null) return;

        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler == null) return;

        long now = System.currentTimeMillis();
        if (now < nextClickTime) return;

        boolean foundAnySellable = false;

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            if (!(slot.inventory instanceof PlayerInventory)) continue;

            String name = stack.getName().getString()
                    .replaceAll("§.", "")
                    .toLowerCase()
                    .trim();

            if (name.endsWith("vinyl")
                    || name.equals("overclocker 3000")
                    || name.equals("chirping stereo")
                    || name.equals("beady eyes")
                    || name.equals("atmospheric filter")
                    || name.equals("clipped wings")
                    || name.equals("wriggling larva")
                    || name.startsWith("bookworm")
                    || name.equals("squeaky toy")
                    || name.equals("mantid claw")) {

                mc.interactionManager.clickSlot(
                        handler.syncId,
                        i,
                        0,
                        SlotActionType.PICKUP,
                        mc.player
                );

                Utils.log("Sold: " + name);
                foundAnySellable = true;
                nextClickTime = now + 100;
                emptyScans = 0;
                return;
            }
        }

        emptyScans++;

        if (!foundAnySellable && emptyScans >= 20) {
            mc.player.closeHandledScreen();
            waitingForTrades = false;
            emptyScans = 0;
        }

        nextClickTime = now + 100;
    }

    private static void handleSpray() {
        if (!Config.INSTANCE.autoSpray) return;
        if (mc.player == null) return;
        if (sprayPhase != SprayPhase.IDLE) return;
        if (killingPests) return;
        if (!isActive || isPaused) return;

        boolean sprayNone = TabListParser.getTabLines().stream().anyMatch(s -> s.contains("Spray: None"));
        long now = System.currentTimeMillis();

        if (sprayNone) {
            if (sprayNoneDetectedTime == -1L) {
                sprayNoneDetectedTime = now;
            }

            if (now - sprayNoneDetectedTime < 5000) {
                return;
            }

        } else {
            sprayNoneDetectedTime = -1L;
            return;
        }

        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) mc.player.getInventory();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            String name = stack.getName().getString()
                    .replaceAll("§.", "")
                    .toLowerCase()
                    .trim();

            if (name.equals("sprayonator")) {
                originalSpraySlot = inv.getSelectedSlot();
                sprayHotbarSlot = i;

                sprayPhase = SprayPhase.SWAP_TO_SPRAY;
                sprayPhaseStart = System.currentTimeMillis();
                releaseAllKeys();
                isSpraying = true;
                sprayNoneDetectedTime = -1L;

                Utils.log("Starting auto spray");
                break;
            }
        }
    }

    private static void handleSpraySequence() {
        if (sprayPhase == SprayPhase.IDLE || mc.player == null) return;

        long now = System.currentTimeMillis();
        PlayerInventory inv = mc.player.getInventory();

        switch (sprayPhase) {

            case SWAP_TO_SPRAY:
                if (now - sprayPhaseStart >= 50) {
                    inv.setSelectedSlot(sprayHotbarSlot);
                    sprayPhase = SprayPhase.WAIT_BEFORE_USE;
                    sprayPhaseStart = now;
                }
                break;

            case WAIT_BEFORE_USE:
                if (now - sprayPhaseStart >= 100) {
                    Utils.simulateUseItem(mc.interactionManager);
                    sprayPhase = SprayPhase.WAIT_BEFORE_RESTORE;
                    sprayPhaseStart = now;
                }
                break;

            case WAIT_BEFORE_RESTORE:
                if (now - sprayPhaseStart >= 150) {
                    sprayPhase = SprayPhase.RESTORE_SLOT;
                    sprayPhaseStart = now;
                }
                break;

            case RESTORE_SLOT:
                if (now - sprayPhaseStart >= 50) {
                    if (originalSpraySlot >= 0 && originalSpraySlot < 9) {
                        inv.setSelectedSlot(originalSpraySlot);
                        Utils.log("Final slot restore: " + originalHotbarSlot);
                    }
                    sprayPhase = SprayPhase.DONE;
                    sprayPhaseStart = now;
                }
                break;

            case DONE:
                if (now - sprayPhaseStart >= 50) {
                    sprayPhase = SprayPhase.IDLE;
                    sprayHotbarSlot = -1;
                    originalSpraySlot = -1;
                    isSpraying = false;
                    holdKeys();
                    Utils.log("Auto spray finished");
                }
                break;
        }
    }
}