package com.faketils.features;

import com.faketils.Faketils;
import com.faketils.config.Config;
import com.faketils.events.*;
import com.faketils.mixin.PlayerInventoryAccessor;
import com.faketils.utils.FarmingWaypoints;
import com.faketils.utils.RenderUtils;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Farming {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random random = new Random();

    private static final KeyBinding toggleKey = Faketils.config().toggleMacro;
    private static final KeyBinding pauseKey = Faketils.config().pauseMacro;
    private static final KeyBinding resetKey = Faketils.config().resetFakeFails;

    public static boolean keysAreHeld = false;

    private static enum RodPhase { IDLE, SWAP_TO_ROD, WAIT_BEFORE_CLICK, CLICK_ROD, WAIT_BEFORE_RESTORE, RESTORE_SLOT, DONE }
    private static RodPhase rodPhase = RodPhase.IDLE;
    private static long rodPhaseStart = 0;
    private static int originalHotbarSlot = -1;
    private static int rodHotbarSlot = -1;
    private static int pendingDoubleJumpTicks = 0;

    private static enum WPhase { IDLE, OPENING, CLICKING, DONE }

    private static WPhase wPhase = WPhase.IDLE;

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
    private static long lastPestScan = 0L;

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

    private static boolean isLcVacuum = false;
    private static volatile Vec3d lastAngryParticlePos = null;
    private static long lastAngryParticleTime = 0L;
    private static volatile boolean lcRunning = false;

    private static int emptyScans = 0;

    private static enum PestPhase { ROOTED, SQUEAKY }
    private static PestPhase currentPestPhase = PestPhase.ROOTED;

    private static int lastSeenSyncId = -1;
    private static long windowOpenedAt = 0L;
    private static boolean windowReady = false;

    public static boolean isActive = false;
    public static boolean isPaused = false;
    public static String currentMode = "none";

    public static boolean killingPests = false;
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

    private record ParticleSample(Vec3d pos, long time) {}
    private static final java.util.ArrayDeque<ParticleSample> particleSamples = new java.util.ArrayDeque<>();
    private static volatile Vec3d extrapolatedPestPos = null;
    private static long lastLcClickTime = 0L;
    private static final long LC_CLICK_INTERVAL_MS = 2000L;
    private static final long PARTICLE_WINDOW_MS = 1500L;

    private static enum PhillipPhase { IDLE, COMMAND_SENT, WAIT_FOR_OPEN, CLICKING_ITEM, WAIT_AFTER_CLICK, CLOSING, DONE }
    private static PhillipPhase phillipPhase = PhillipPhase.IDLE;
    private static long phillipPhaseStart = 0L;

    private static int plot = 0;
    private static long lastBrokenBlock = 0L;
    private static long lastXp = 0L;
    private static long lastPest = 0L;
    private static int blocksBroken = 0;
    private static boolean isBreaking = false;
    private static long pauseTimeMs = 0;
    private static long startTime = 0L;
    private static double bps = 0.0;
    private static boolean isMining = false;
    private static BlockPos miningPos = null;
    private static boolean waitingForTrades = false;
    private static boolean worldChanged = false;
    private static float lockedYaw = 0f;
    private static float lockedPitch = 0f;
    private static long lastFailTime = 0L;
    private static int lockedSlot = -1;
    private static long bpsZeroStartTime = 0L;
    private static final float yawPitchTolerance = 0.5f;
    private static String lockedItemName = null;
    private static final Set<ArmorStandEntity> ignoredPests = new HashSet<>();
    private static long ignoredResetTime = 0;
    private static Vec3d lastSentPestTarget = null;
    private static ArmorStandEntity currentPestStand = null;
    private static final double RETARGET_DIST_SQ = 9.0;
    private static boolean needsRetarget = false;
    private static long lastTogglePauseTime = 0L;
    private static final long FAILSAFE_BLOCK_MS = 3000L;
    private static Vec3d lastPestStandPos = null;
    private static final double TELEPORT_THRESHOLD_SQ = 16.0;
    private static LivingEntity currentPestMob = null;
    private static final double PEST_PAIR_RADIUS_SQ = 4.0 * 4.0;
    private static final double PEST_LIVING_RADIUS = 3.5;
    private static boolean hasPaused = false;

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
    private static String currentState = "idle";

    public static String getCurrentFail() {
        return currentFail;
    }
    public static String getCurrentState() {
        return currentState;
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());
        FtEventBus.onEvent(FtEvent.WorldRender.class, event -> onRenderWorldLast(event));
        FtEventBus.onEvent(FtEvent.HudRender.class, hud -> render(hud.context));
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((oldWorld, newWorld) -> {
            if (isActive) {
                handleToggle();
                releaseAllKeys();
                worldChanged = true;
                currentState = "idle";
//                new Thread(() -> {
//                    if (mc.player == null) return;
//                    try {
//                        for (int i = 0; i < 25; i++) {
//                            worldChange();
//                            Thread.sleep(80);
//                        }
//                    } catch (InterruptedException ignored) {}
//                }).start();
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
            if (packet instanceof net.minecraft.network.packet.s2c.play.ParticleS2CPacket pp) {
                if (lcRunning && pp.getParameters().getType() == net.minecraft.particle.ParticleTypes.ENCHANT) {
                    Vec3d pos = new Vec3d(pp.getX(), pp.getY(), pp.getZ());
                    long now = System.currentTimeMillis();
                    lastAngryParticlePos = pos;
                    lastAngryParticleTime = now;

                    synchronized (particleSamples) {
                        particleSamples.addLast(new ParticleSample(pos, now));
                        while (!particleSamples.isEmpty() && now - particleSamples.peekFirst().time() > PARTICLE_WINDOW_MS) {
                            particleSamples.pollFirst();
                        }
                        if (particleSamples.size() >= 2) {
                            extrapolatedPestPos = computeExtrapolatedPos(now);
                        }
                    }
                }
            }
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString().replaceAll("§.", "");
            Pattern pattern = Pattern.compile("ൠ Pest have spawned in Plot\\s*-\\s*(\\d+)");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find() && Faketils.config().pestFarming) {
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
                            currentState = "Changing EQ";
                            eqState = EqState.OPENING;
                            eqStateStart = System.currentTimeMillis();
                            currentPestPhase = PestPhase.ROOTED;
                            itemsUsedThisPhase = 0;
                            lastProcessedSyncId = -1;
                        } catch (InterruptedException ignored) {}
                    }).start();
                }
            }

            if (text.contains("There are not any Pests on your Garden right now!")) {
                if (killingPests) {
                    killingPests = false;
                    hasPaused = false;
                    isLcVacuum = false;
                    lastSentPestTarget = null;
                    lcRunning = false;
                    if (originalPestKillSlot >= 0 && originalPestKillSlot < 9) {
                        mc.player.getInventory().setSelectedSlot(originalPestKillSlot);
                        Utils.log("Restored original hotbar slot after pest killing: " + (originalPestKillSlot + 1));
                    }
                    originalPestKillSlot = -1;
                    lastPestStandPos = null;
                    currentPestStand = null;
                    FlyHandler.stop();
                    RotationHandler.reset();
                    currentPestMob = null;
                    needsRetarget = true;
                    handlePause();
                    if (!Faketils.config().pestFarming) {
                        currentMode = null;
                        mc.player.networkHandler.sendChatMessage("/warp garden");
                        new Thread(() -> {
                            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                            mc.player.getAbilities().flying = false;
                            mc.player.sendAbilitiesUpdate();
                        }).start();
                    }
                    currentPestTarget = null;
                    pestOffset = Vec3d.ZERO;
                    lastOffsetChange = 0L;
                    Utils.log("All pests killed → resuming normal farming");
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
        if (!Faketils.config().funnyToggle) return;
        if (!Utils.isInGarden()) return;

        updatePestsTimer();

        while (toggleKey.wasPressed()) handleToggle();
        while (pauseKey.wasPressed()) handlePause();
        while (resetKey.wasPressed()) handleReset();

        if (killingPests && isPaused) {
            currentState = "Killing pests";
            resetKilling();
            handleKilling();
            if (currentPestTarget != null && mc.player != null) {
                double distance = mc.player.getEntityPos().distanceTo(currentPestTarget);
                boolean inSweetSpot = distance < 8.0;
                mc.options.useKey.setPressed(inSweetSpot);
            }
            if (mc.currentScreen != null && mc.player != null && mc.currentScreen.getTitle().getString()
                    .replaceAll("§.", "").trim().toLowerCase().contains("stereo harmony")) mc.player.closeHandledScreen();
        }

        if (isActive && !isPaused && Faketils.config().pestKilling) {mc.options.useKey.setPressed(false);}

        if (!isActive || (isPaused && !killingPests)) {
            currentState = "idle";
            releaseAllKeys();
            return;
        }

        handleRodSequence();
        handleWardrobeSequence();

        if (eqActive && !isSpraying) {
            releaseAllKeys();
            handleEqSequence();
            return;
        }

        handleTradesScreen();
        handlePestBuff();
        nonPfarmingKilling();

        if (mc.currentScreen != null) {
            lastXp = System.currentTimeMillis();
            return;
        }

        handleSpray();
        handleSpraySequence();

        if (isSpraying) {
            currentState = "Spraying";
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

    private static void worldChange() {
        currentFail = "World Changed";
        mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
    }

    private static void nonPfarmingKilling() {
        if (!Faketils.config().pestKilling) return;
        if (Faketils.config().pestFarming) return;
        TabListParser.getTabLines().stream()
                .filter(s -> s.contains("Alive: "))
                .findFirst()
                .ifPresent(line -> {
                    try {
                        int alive = Integer.parseInt(line.replaceAll(".*Alive:\\s+(\\d+).*", "$1").trim());
                        if (alive > 4) {
                            killingPests = true;
                            if (!hasPaused) {
                                hasPaused = true;
                                handlePause();
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                });
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

        if (mc.currentScreen == null) return;
        if (!mc.currentScreen.getTitle().getString().replaceAll("§.", "").trim().toLowerCase().contains("your equipment")) return;

        ScreenHandler handler = mc.player.currentScreenHandler;

        if (!handler.getCursorStack().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        int delayMs = Faketils.config().swapDelay + random.nextInt(100);
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
                    mc.interactionManager.clickSlot(handler.syncId, i, 2, SlotActionType.CLONE, mc.player);
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
            if (now - eqStateStart >= Faketils.config().swapDelay + random.nextInt(100)) {
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
                    mc.interactionManager.clickSlot(handler.syncId, placeSlot, 2, SlotActionType.CLONE, mc.player);
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
            if (Faketils.config().petSwapType == Config.PetSwapType.ROD) startRodSequence(now);
            if (Faketils.config().petSwapType == Config.PetSwapType.ARMOR) {
                if (currentPestPhase == PestPhase.ROOTED) {
                    wardrobeSlot = Faketils.config().wardrobeSlot;
                }
                if (currentPestPhase == PestPhase.SQUEAKY) {
                    wardrobeSlot = Faketils.config().wardrobeSlotOld;
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

        if (mc.currentScreen == null) return;
        if (!mc.currentScreen.getTitle().getString().replaceAll("§.", "").trim().toLowerCase().contains("wardrobe")) return;

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

                if (now - wardrobePhaseStart > 15000) {
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
                            if (Faketils.config().pestKilling) swapToInfiniVacuum();
                            pendingDoubleJumpTicks = 10;
                            try { Thread.sleep(350); } catch (InterruptedException ignored) {}
                            if (Faketils.config().pestKilling) handleKilling();
                            if (Faketils.config().pestKilling) {
                                killingPests = true;
                                isLcVacuum = true;
                                lcVacuum();
                            }
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
        if (!Faketils.config().pestKilling) return false;

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

    private static void lcVacuum() {
        if (!isLcVacuum || !Faketils.config().pestKilling || lcRunning || mc.player == null) return;
        lcRunning = true;
        new Thread(() -> {
            try {
                while (lcRunning && killingPests) {
                    long now = System.currentTimeMillis();

                    if (currentPestTarget != null) {
                        Thread.sleep(50);
                        continue;
                    }

                    Vec3d target = extrapolatedPestPos;
                    if (target != null && now - lastAngryParticleTime < 2000L) {
                        final Vec3d flyTarget = target;
                        needsRetarget = true;
                        mc.execute(() -> FlyHandler.flyTo(flyTarget));
                        Thread.sleep(150);
                    }

                    if (now - lastLcClickTime >= LC_CLICK_INTERVAL_MS) {
                        lastLcClickTime = now;
                        needsRetarget = true;
                        mc.execute(() -> {
                            if (mc.player == null) return;
                            PlayerInventoryAccessor inv = (PlayerInventoryAccessor) mc.player.getInventory();
                            inv.setSelectedSlot(vacuumSlot);
                            mc.interactionManager.attackBlock(mc.player.getBlockPos(), net.minecraft.util.math.Direction.UP);
                            mc.options.attackKey.setPressed(true);
                        });
                        Thread.sleep(120);
                        mc.execute(() -> mc.options.attackKey.setPressed(false));
                        Utils.log("§eLC → no signal, clicked vacuum");
                    }

                    Thread.sleep(50);
                }
            } catch (InterruptedException ignored) {
            } finally {
                lcRunning = false;
                extrapolatedPestPos = null;
                needsRetarget = true;
                lastLcClickTime = 0L;
                synchronized (particleSamples) { particleSamples.clear(); }
                Utils.log("§cLC Vacuum stopped");
            }
        }, "lc-vacuum").start();
    }

    private static void resetKilling() {
        if (mc.player == null || mc.world == null || !Utils.isInGarden()) return;
        if (!Faketils.config().pestKilling) return;

        if (TabListParser.getTabLines().stream().anyMatch(s -> s.contains("Alive: 0"))) {
            if (killingPests) {
                killingPests = false;
                hasPaused = false;
                isLcVacuum = false;
                lastSentPestTarget = null;
                lcRunning = false;
                if (originalPestKillSlot >= 0 && originalPestKillSlot < 9) {
                    mc.player.getInventory().setSelectedSlot(originalPestKillSlot);
                    Utils.log("Restored original hotbar slot after pest killing: " + (originalPestKillSlot + 1));
                }
                originalPestKillSlot = -1;
                lastPestStandPos = null;
                currentPestStand = null;
                FlyHandler.stop();
                RotationHandler.reset();
                currentPestMob = null;
                needsRetarget = true;
                handlePause();
                if (!Faketils.config().pestFarming) {
                    currentMode = null;
                    mc.player.networkHandler.sendChatMessage("/warp garden");
                    new Thread(() -> {
                        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                        mc.player.getAbilities().flying = false;
                        mc.player.sendAbilitiesUpdate();
                    }).start();
                }
                currentPestTarget = null;
                pestOffset = Vec3d.ZERO;
                lastOffsetChange = 0L;
                Utils.log("All pests killed → resuming normal farming");
            }
        }
    }

    public static void handleKilling() {
        if (mc.player == null || mc.world == null || !Utils.isInGarden()) return;
        if (!Faketils.config().pestKilling) return;
        if (!killingPests) return;

        ArmorStandEntity bestStand = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity stand)) continue;
            if (PestHelper.getPestFromHead(stand) == null) continue;

            Vec3d standPos = stand.getEntityPos();
            boolean hasPairedMob = false;

            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof LivingEntity living)) continue;
                if (living instanceof ArmorStandEntity) continue;
                if (living instanceof net.minecraft.entity.player.PlayerEntity) continue;
                if (living.isDead()) continue;
                if (!(living instanceof net.minecraft.entity.mob.SilverfishEntity) &&
                        !(living instanceof net.minecraft.entity.passive.BatEntity)) continue;
                if (living.squaredDistanceTo(standPos.x, standPos.y, standPos.z) <= PEST_PAIR_RADIUS_SQ) {
                    hasPairedMob = true;
                    break;
                }
            }

            if (!hasPairedMob) continue;

            double distSq = stand.squaredDistanceTo(mc.player);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestStand = stand;
            }
        }

        if (bestStand != null) {
            Vec3d target = bestStand.getEntityPos().add(0, 1.15, 0).add(pestOffset);
            currentPestTarget = target;
            FlyHandler.flyTo(target);
        } else {
            currentPestTarget = null;
        }
    }

    private static void updatePestsTimer() {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();

        if (pestsSpawned && now - lastPest > Faketils.config().pestTime * 1000L && Faketils.config().pestFarming) {
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
            Utils.log(Faketils.config().pestTime + "s timer reached → starting Squeaky phase");
        }
    }

    private static void handleToggle() {
        lastTogglePauseTime = System.currentTimeMillis();
        isActive = !isActive;
        isPaused = false;
        if (!isActive) {
            lastXp = System.currentTimeMillis();
            releaseAllKeys();
            FlyHandler.stop();
            hasPaused = false;
            killingPests = false;
            isLcVacuum = false;
            lastSentPestTarget = null;
            lcRunning = false;
            currentPestTarget = null;
            pestOffset = Vec3d.ZERO;
            lastOffsetChange = 0L;
            originalPestKillSlot = -1;
            needsRetarget = true;
            currentPestStand = null;
            currentPestMob = null;
            lastPestStandPos = null;
            RotationHandler.reset();
            currentMode = "none";
            lastWaypoint = null;
            pauseWaypoint = null;
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
            }
            ticksOnWaypoint = 0;
            unlockMouse();
        } else if (mc.player != null) {
            lastXp = System.currentTimeMillis();
            lockedYaw = mc.player.getYaw();
            lockedPitch = mc.player.getPitch();
            PlayerInventoryAccessor inv = (PlayerInventoryAccessor) mc.player.getInventory();
            lockedSlot = inv.getSelectedSlot();
            mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
            lockedItemName = mc.player.getMainHandStack().getName().getString();
            lockMouse();
            lastXp = 0;
            pauseWaypoint = null;
        }
        Utils.log("Macro toggled: " + isActive);
    }

    private static void handlePause() {
        if (!isActive) return;
        lastTogglePauseTime = System.currentTimeMillis();
        isPaused = !isPaused;

        if (isPaused) {
            lastXp = System.currentTimeMillis();
            releaseAllKeys();
            movementBlockTicks = 10;

            unlockMouse();
            pauseTimeMs = System.currentTimeMillis();

            if (mc.player != null) {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
                pauseWaypoint = BlockPos.ofFloored(mc.player.getEntityPos());
                if (Faketils.config().rewarpOnPause) {
                    mc.player.networkHandler.sendChatMessage("/setspawn");
                    mc.player.sendMessage(Text.literal("§7[§bFaketils§7] §eReWarp point set!"), false);
                }
            }
            Utils.log("Macro paused");
        } else {
            lastXp = System.currentTimeMillis();
            lockMouse();
            releaseAllKeys();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
            }
            lastXp = 0;

            boolean shouldWarp = false;

            if (Faketils.config().rewarpOnPause && pauseWaypoint != null && mc.player != null) {
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
        mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
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
        if (System.currentTimeMillis() - lastTogglePauseTime < FAILSAFE_BLOCK_MS) return;

        if (killingPests) {
            lastXp = System.currentTimeMillis();
            bpsZeroStartTime = 0L;
            return;
        }

        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) mc.player.getInventory();
        int currentSlot = inv.getSelectedSlot();
        String currentItemName = mc.player.getMainHandStack().getName().getString();

        if (!isActive) return;

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
        if (!Utils.isInSkyblock() || !Faketils.config().funnyToggle) return;
        if (!Utils.isInGarden()) return;
        if (Faketils.config().noHuds) return;

        String text;
        int color;

        if (!isActive) {
            text = "Off";
            color = 0xFFFF4444;
        } else if (isPaused) {
            text = "Paused";
            color = 0xFFFFFF44;
        } else {
            text = "On";
            color = 0xFF44FF44;
        }

        int x = Faketils.config().macroHudX;
        int y = Faketils.config().macroHudY;

        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.scale(2f, 2f);

        ctx.drawTextWithShadow(mc.textRenderer, Text.literal("§7Macro: ").append(Text.literal(text).withColor(color)), x, y, 0xFFFFFFFF);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal("§7State: ").append(Text.literal(getCurrentState()).withColor(0xFFAAAAAA)), x, y + 9, 0xFFFFFFFF);

        matrices.popMatrix();
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
        float yaw = ((mc.player.getYaw() % 360) + 360) % 360;
        boolean facingSouthOrNorth = (yaw >= 315 || yaw < 45) || (yaw >= 135 && yaw < 225);
        boolean facingEastOrWest = (yaw >= 45 && yaw < 135) || (yaw >= 225 && yaw < 315);

        List<BlockPos> right = FarmingWaypoints.WAYPOINTS.getOrDefault("right", List.of());
        List<BlockPos> left = FarmingWaypoints.WAYPOINTS.getOrDefault("left", List.of());
        List<BlockPos> warp = FarmingWaypoints.WAYPOINTS.getOrDefault("warp", List.of());

        String targetMode = "none";
        BlockPos matchedWaypoint = null;

        if (warp.contains(pos)) {
            matchedWaypoint = pos;
            targetMode = "warp";
        } else if (facingSouthOrNorth) {
            for (int x = -1000; x <= 1000; x++) {
                BlockPos candidate = new BlockPos(pos.getX() + x, pos.getY(), pos.getZ());
                if (right.contains(candidate)) {
                    matchedWaypoint = candidate;
                    targetMode = "right";
                    break;
                }
                if (left.contains(candidate)) {
                    matchedWaypoint = candidate;
                    targetMode = "left";
                    break;
                }
            }
        } else if (facingEastOrWest) {
            for (int z = -1000; z <= 1000; z++) {
                BlockPos candidate = new BlockPos(pos.getX(), pos.getY(), pos.getZ() + z);
                if (right.contains(candidate)) {
                    matchedWaypoint = candidate;
                    targetMode = "right";
                    break;
                }
                if (left.contains(candidate)) {
                    matchedWaypoint = candidate;
                    targetMode = "left";
                    break;
                }
            }
        }

        if (!targetMode.equals("none")) {
            if (lastWaypoint == null || !lastWaypoint.equals(matchedWaypoint)) {
                lastWaypoint = matchedWaypoint;
                ticksOnWaypoint = 1;
            } else {
                ticksOnWaypoint++;
                int requiredTicks = Faketils.config().instaSwitch ? 15 : randomDelayTicks;
                if (ticksOnWaypoint >= requiredTicks) {
                    if (targetMode.equals("warp")) {
                        mc.player.networkHandler.sendChatMessage("/warp garden");
                        currentMode = "none";
                        releaseAllKeys();
                        lastWaypoint = null;
                        ticksOnWaypoint = 0;
                    } else {
                        currentMode = targetMode;
                        currentState = targetMode;
                    }
                    if (!Faketils.config().instaSwitch) {
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

        if (currentMode.equals("left")) {
            mc.options.forwardKey.setPressed(Faketils.config().leftForward);
            mc.options.backKey.setPressed(Faketils.config().leftBack);
            mc.options.leftKey.setPressed(Faketils.config().leftLeft);
            mc.options.rightKey.setPressed(Faketils.config().leftRight);
        } else if (currentMode.equals("right")) {
            mc.options.forwardKey.setPressed(Faketils.config().rightForward);
            mc.options.backKey.setPressed(Faketils.config().rightBack);
            mc.options.leftKey.setPressed(Faketils.config().rightLeft);
            mc.options.rightKey.setPressed(Faketils.config().rightRight);
        }

        mc.options.attackKey.setPressed(true);
        keysAreHeld = true;
    }

    private static void releaseAllKeys() {
        if (!keysAreHeld) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.useKey.setPressed(false);
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
        if (Faketils.config().funnyWaypoints) {
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
        if (!Faketils.config().autoSellJunk) return;
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

            if (isSellable(name)) {

                mc.player.networkHandler.sendChatMessage("/trades");
                waitingForTrades = true;
                wPhase = WPhase.OPENING;
                long now = System.currentTimeMillis();
                wPhaseStart = now;
                return;
            }
        }
    }

    private static void handleTradesScreen() {
        if (!waitingForTrades) return;
        if (mc.player == null) return;
        if (mc.currentScreen == null) return;

        ScreenHandler handler = mc.player.currentScreenHandler;
        if (!mc.currentScreen.getTitle().getString().replaceAll("§.", "").trim().toLowerCase().contains("trades")) return;
        long now = System.currentTimeMillis();

        if (wPhase == WPhase.OPENING && now - wPhaseStart > 500) {
            wPhase = WPhase.CLICKING;
            wPhaseStart = now;
        }

        if (wPhase == WPhase.CLICKING && now - wPhaseStart > 300) {
            boolean found = false;

            for (int i = 0; i < handler.slots.size(); i++) {
                Slot slot = handler.slots.get(i);
                ItemStack stack = slot.getStack();

                if (stack.isEmpty()) continue;
                if (!(slot.inventory instanceof PlayerInventory)) continue;

                String name = stack.getName().getString()
                        .replaceAll("§.", "")
                        .toLowerCase()
                        .trim();

                if (isSellable(name)) {
                    mc.interactionManager.clickSlot(handler.syncId, i, 2, SlotActionType.CLONE, mc.player);
                    Utils.log("Sold: " + name);
                    wPhaseStart = now;
                    found = true;
                    break;
                }
            }

            if (!found) {
                wPhase = WPhase.DONE;
                wPhaseStart = now;
            }
        }

        if (wPhase == WPhase.DONE && now - wPhaseStart > 1000) {
            mc.player.closeHandledScreen();
            waitingForTrades = false;
        }
    }

    private static void handlePestBuff() {
        if (!Faketils.config().autoPhillip) return;
        if (mc.player == null) return;
        if (!isActive || isPaused) return;
        if (eqActive) return;
        if (killingPests) return;

        if (phillipPhase == PhillipPhase.IDLE) {
            if (TabListParser.getTabLines().stream().anyMatch(s -> s.contains("Bonus: INACTIVE"))) {
                mc.player.networkHandler.sendChatMessage("/call phillip");
                phillipPhase = PhillipPhase.COMMAND_SENT;
                phillipPhaseStart = System.currentTimeMillis();
                Utils.log("Phillip command sent");
            }
            return;
        }

        long now = System.currentTimeMillis();

        switch (phillipPhase) {
            case COMMAND_SENT:
                if (now - phillipPhaseStart > 500 + random.nextInt(300)) {
                    phillipPhase = PhillipPhase.WAIT_FOR_OPEN;
                    phillipPhaseStart = now;
                }
                break;

            case WAIT_FOR_OPEN:
                if (mc.currentScreen == null) {
                    if (now - phillipPhaseStart > 5000) {
                        Utils.log("Phillip screen never opened → aborting");
                        phillipPhase = PhillipPhase.IDLE;
                    }
                    break;
                }

                String title = mc.currentScreen.getTitle().getString().replaceAll("§.", "").trim().toLowerCase();
                if (title.contains("pesthunter")) {
                    phillipPhase = PhillipPhase.CLICKING_ITEM;
                    phillipPhaseStart = now;
                    Utils.log("Pesthunter screen detected");
                } else if (now - phillipPhaseStart > 5000) {
                    Utils.log("Wrong screen open: " + title + " → aborting");
                    mc.player.closeHandledScreen();
                    phillipPhase = PhillipPhase.IDLE;
                }
                break;

            case CLICKING_ITEM:
                if (now - phillipPhaseStart < 300 + random.nextInt(300)) break;

                if (mc.player.currentScreenHandler == null) {
                    phillipPhase = PhillipPhase.IDLE;
                    break;
                }

                ScreenHandler handler = mc.player.currentScreenHandler;
                boolean clicked = false;

                for (int i = 0; i < handler.slots.size(); i++) {
                    ItemStack stack = handler.slots.get(i).getStack();
                    if (stack.isEmpty()) continue;

                    String name = stack.getName().getString().replaceAll("§.", "").trim().toLowerCase();
                    if (name.contains("empty vacuum bag")) {
                        mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                        Utils.log("Clicked Empty Vacuum Bag at slot " + i);
                        phillipPhase = PhillipPhase.WAIT_AFTER_CLICK;
                        phillipPhaseStart = now;
                        clicked = true;
                        break;
                    }
                }

                if (!clicked) {
                    if (now - phillipPhaseStart > 8000) {
                        Utils.log("Empty Vacuum Bag not found → aborting");
                        mc.player.closeHandledScreen();
                        phillipPhase = PhillipPhase.IDLE;
                    }
                }
                break;

            case WAIT_AFTER_CLICK:
                if (now - phillipPhaseStart > 400 + random.nextInt(200)) {
                    phillipPhase = PhillipPhase.CLOSING;
                    phillipPhaseStart = now;
                }
                break;

            case CLOSING:
                mc.player.closeHandledScreen();
                Utils.log("Closing Pesthunter screen");
                phillipPhase = PhillipPhase.DONE;
                phillipPhaseStart = now;
                break;

            case DONE:
                if (now - phillipPhaseStart > 300) {
                    phillipPhase = PhillipPhase.IDLE;
                    Utils.log("Phillip sequence complete");
                }
                break;
        }
    }

    private static void handleSpray() {
        if (!Faketils.config().autoSpray) return;
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

    private static Vec3d computeExtrapolatedPos(long now) {
        ParticleSample first = particleSamples.peekFirst();
        ParticleSample last  = particleSamples.peekLast();

        double dtSec = (last.time() - first.time()) / 1000.0;
        if (dtSec < 0.05) return last.pos();

        Vec3d velocity = last.pos().subtract(first.pos()).multiply(1.0 / dtSec);

        return last.pos().add(velocity.x * 100, velocity.y * 0.4, velocity.z * 100);
    }

    private static boolean isSellable(String name) {
        return name.endsWith("vinyl")
                || name.equals("overclocker 3000")
                || name.equals("chirping stereo")
                || name.equals("beady eyes")
                || name.equals("atmospheric filter")
                || name.equals("clipped wings")
                || name.equals("wriggling larva")
                || name.startsWith("bookworm")
                || name.equals("squeaky toy")
                || name.equals("mantid claw");
    }
}