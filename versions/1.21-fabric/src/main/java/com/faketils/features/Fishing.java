package com.faketils.features;

import com.faketils.Faketils;
import com.faketils.events.RotationHandler;
import com.faketils.mixin.PlayerInventoryAccessor;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Fishing {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Random random = new Random();

    private static int clickTimer = 0;
    private static boolean hasClickedOnce = false;
    private static boolean scheduledClick = false;
    private static int delayTimer = 0;

    private static int weaponState = 0;
    private static int originalSlot = 0;
    private static int weaponSlot = 0;
    private static int delayCounter = 0;
    private static int clickCount = 0;

    private static final float LOOK_DOWN_PITCH = 89f;
    private static float savedYaw = 0f;
    private static float savedPitch = 0f;

    private static boolean slugFishingActive = false;
    private static long slugStartTime = 0L;
    private static int lastBobberId = -1;

    private static final Set<Integer> handledArmorStands = new HashSet<>();

    private static final int STATE_IDLE              = 0;
    private static final int STATE_PRE_ROTATE        = 1;
    private static final int STATE_SWITCH_DELAY      = 2;
    private static final int STATE_SWITCH_TO_WEAPON  = 3;
    private static final int STATE_WAIT_SWITCH       = 4;
    private static final int STATE_CLICK_WEAPON      = 5;
    private static final int STATE_WAIT_SWITCH_BACK  = 6;
    private static final int STATE_SWITCH_BACK       = 7;
    private static final int STATE_POST_ROTATE       = 8;
    private static final int STATE_DONE              = 9;

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            onClientTick();
            onRenderWorldLast();
        });
    }

    private static void onRenderWorldLast() {
        if (!Utils.isInSkyblock() || !Faketils.config().fishingHelper) return;
        if (mc.screen != null || mc.player == null || mc.level == null) return;

        ItemStack heldItem = mc.player.getMainHandItem();
        if (!(heldItem.getItem() instanceof FishingRodItem)) return;

        handledArmorStands.removeIf(id -> mc.level.getEntity(id) == null);

        for (ArmorStand armorStand : mc.level.getEntitiesOfClass(ArmorStand.class,
                mc.player.getBoundingBox().inflate(64), e -> true)) {

            if (armorStand.isRemoved() || !armorStand.hasCustomName()) continue;

            Component customName = armorStand.getCustomName();
            if (customName == null) continue;

            if (customName.getString().equals("!!!")) {
                if (!handledArmorStands.contains(armorStand.getId())) {
                    if (delayTimer == 0) {
                        scheduledClick = true;
                        handledArmorStands.add(armorStand.getId());
                        Utils.log("Detected ArmorStand (ID: " + armorStand.getId() + ")");
                    }
                }
            }
        }
    }

    private static void onClientTick() {
        if (!Utils.isInSkyblock() || !Faketils.config().fishingHelper) return;
        if (mc.screen != null || mc.player == null || mc.gameMode == null) return;

        var player = mc.player;
        var gameMode = mc.gameMode;
        PlayerInventoryAccessor inventory = (PlayerInventoryAccessor) player.getInventory();

        if (Faketils.config().slugFishing) {
            var bobber = player.fishing;
            if (bobber != null) {
                if (bobber.getId() != lastBobberId) {
                    lastBobberId = bobber.getId();
                    slugStartTime = System.currentTimeMillis();
                    slugFishingActive = true;
                    Utils.log("Slug fishing cooldown started.");
                }
            } else if (lastBobberId != -1) {
                lastBobberId = -1;
                slugFishingActive = false;
            }

            if (slugFishingActive) {
                long elapsed = System.currentTimeMillis() - slugStartTime;
                if (elapsed < 10_000) {
                    scheduledClick = false;
                    return;
                } else {
                    slugFishingActive = false;
                    Utils.log("Slug fishing cooldown ended.");
                }
            }
        }

        if (scheduledClick) {
            clickTimer = random.nextInt(6) + 5;
            hasClickedOnce = true;

            Utils.simulateUseItem(gameMode);
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            scheduledClick = false;

            if (Faketils.config().fishingHelperKilling && !Faketils.config().fishingHelperKillingWeapon.isEmpty()) {
                WeaponResult weapon = WeaponDetector.findWeapon();
                if (weapon != null) {
                    originalSlot = inventory.getSelectedSlot();
                    weaponSlot = weapon.slot;

                    if (Faketils.config().fishingLookDown) {
                        savedYaw = player.getYRot();
                        savedPitch = player.getXRot();
                        RotationHandler.setTarget(savedYaw, LOOK_DOWN_PITCH);
                        weaponState = STATE_PRE_ROTATE;
                    } else {
                        weaponState = STATE_SWITCH_DELAY;
                        delayCounter = random.nextInt(4) + 2;
                    }
                    Utils.log("Weapon found in slot " + weaponSlot + ", switching...");
                }
            }
        }

        if (weaponState > STATE_IDLE) {
            switch (weaponState) {
                case STATE_PRE_ROTATE -> {
                    if (!RotationHandler.active) {
                        weaponState = STATE_SWITCH_DELAY;
                        delayCounter = random.nextInt(4) + 2;
                    }
                }
                case STATE_SWITCH_DELAY -> {
                    if (delayCounter-- <= 0) {
                        weaponState = STATE_SWITCH_TO_WEAPON;
                    }
                }
                case STATE_SWITCH_TO_WEAPON -> {
                    inventory.setSelectedSlot(weaponSlot);
                    delayCounter = random.nextInt(4) + 2;
                    weaponState = STATE_WAIT_SWITCH;
                }
                case STATE_WAIT_SWITCH -> {
                    if (delayCounter-- <= 0) {
                        weaponState = STATE_CLICK_WEAPON;
                        delayCounter = random.nextInt(4) + 3;
                    }
                }
                case STATE_CLICK_WEAPON -> {
                    if (delayCounter-- <= 0) {
                        Utils.simulateUseItem(gameMode);
                        clickCount++;

                        int maxClicks = Faketils.config().fishingHelperKillingAmount;
                        if (clickCount >= maxClicks) {
                            clickCount = 0;
                            weaponState = STATE_WAIT_SWITCH_BACK;
                            delayCounter = random.nextInt(4) + 2;
                        } else {
                            delayCounter = random.nextInt(4) + 3;
                        }
                    }
                }
                case STATE_WAIT_SWITCH_BACK -> {
                    if (delayCounter-- <= 0) {
                        weaponState = STATE_SWITCH_BACK;
                    }
                }
                case STATE_SWITCH_BACK -> {
                    inventory.setSelectedSlot(originalSlot);
                    Utils.log("Switched back to original slot.");

                    if (Faketils.config().fishingLookDown) {
                        RotationHandler.setTarget(savedYaw, savedPitch);
                        weaponState = STATE_POST_ROTATE;
                    } else {
                        weaponState = STATE_DONE;
                    }
                }
                case STATE_POST_ROTATE -> {
                    if (!RotationHandler.active) {
                        weaponState = STATE_DONE;
                    }
                }
                case STATE_DONE -> {
                    weaponState = STATE_IDLE;
                    Utils.log("Weapon sequence finished, ready for reel.");
                }
            }
            return;
        }

        if (clickTimer > 0) {
            clickTimer--;
            if (clickTimer == 0 && hasClickedOnce) {
                Utils.simulateUseItem(gameMode);
                hasClickedOnce = false;
            }
        }
    }

    public static class WeaponResult {
        public final int slot;
        public final KeyMapping hotbarKey;

        public WeaponResult(int slot, KeyMapping hotbarKey) {
            this.slot = slot;
            this.hotbarKey = hotbarKey;
        }
    }

    public static class WeaponDetector {
        public static WeaponResult findWeapon() {
            var player = mc.player;
            if (player == null) return null;

            String targetName = Faketils.config().fishingHelperKillingWeapon;

            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.isEmpty()) continue;

                Component displayName = stack.getHoverName();
                if (displayName.getString().contains(targetName)) {
                    Utils.log("Found weapon '" + targetName + "' in slot " + slot);
                    return new WeaponResult(slot, mc.options.keyHotbarSlots[slot]);
                }
            }
            return null;
        }
    }
}