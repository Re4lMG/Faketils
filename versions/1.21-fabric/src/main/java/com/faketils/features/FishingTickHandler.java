package com.faketils.features;

import com.faketils.Faketils;
import com.faketils.mixin.PlayerInventoryAccessor;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class FishingTickHandler {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean scheduledClick = false;
    private static int clickTimer = 0;
    private static boolean hasClickedOnce = false;
    private static int delayTimer = 0;
    private static int fireVeilState = 0;
    private static int originalSlot = 0;
    private static int veilSlot = 0;
    private static int delayCounter = 0;
    private static int fishingKeyTimer = 0;
    private static final Set<Integer> handledArmorStands = new HashSet<>();
    private static final Random random = new Random();

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());
        WorldRenderEvents.AFTER_ENTITIES.register(context -> onRenderWorldLast());
    }

    private static void onRenderWorldLast() {
        if (!Faketils.config.fishingHelper) return;
        if (mc.currentScreen != null) return;
        if (mc.world == null) return;

        var player = mc.player;
        if (player == null) return;
        var heldItem = player.getMainHandStack();
        if (heldItem.getItem() != Items.FISHING_ROD) return;

        for (var entity : mc.world.getEntities()) {
            if (entity instanceof ArmorStandEntity armorStand) {
                if (armorStand.isRemoved() || !armorStand.hasCustomName()) continue;

                var name = armorStand.getCustomName();
                if (name == null) continue;

                String nameString = name.getString();

                if (nameString.equals("!!!") && !handledArmorStands.contains(armorStand.getId())) {
                    if (delayTimer == 0) {
                        scheduledClick = true;
                        handledArmorStands.add(armorStand.getId());
                        Utils.log("EntityID: " + armorStand.getId());
                    }
                }
            }
        }

        handledArmorStands.removeIf(id -> mc.world.getEntityById(id) == null);
    }

    private static void onClientTick() {
        if (!Faketils.config.fishingHelper) return;
        if (mc.currentScreen != null) return;
        var player = mc.player;
        if (player == null) return;

        PlayerInventoryAccessor inventoryAccessor = (PlayerInventoryAccessor) player.getInventory();

        if (fishingKeyTimer > 0) {
            fishingKeyTimer--;
            if (fishingKeyTimer == 0) {
                mc.options.useKey.setPressed(false);
                Utils.log("Key released (fishingKeyTimer)");
            }
        }

        if (fireVeilState > 0) {
            switch (fireVeilState) {
                case 1:
                    if (delayCounter > 0) delayCounter--;
                    else {
                        fireVeilState = 2;
                        Utils.log("FireVeilState 1 -> 2");
                    }
                    break;
                case 2:
                    inventoryAccessor.setSelectedSlot(veilSlot);
                    delayCounter = random.nextInt(4) + 2;
                    fireVeilState = 3;
                    Utils.log("Switched to veil slot " + veilSlot);
                    break;
                case 3:
                    if (delayCounter > 0) delayCounter--;
                    else {
                        if (fishingKeyTimer == 0) {
                            mc.options.useKey.setPressed(true);
                            fishingKeyTimer = 1;
                            Utils.log("Fire Veil key pressed");
                        }
                        fireVeilState = 4;
                        delayCounter = 1;
                    }
                    break;
                case 4:
                    if (delayCounter > 0) delayCounter--;
                    else {
                        fireVeilState = 5;
                        delayCounter = random.nextInt(4) + 2;
                        Utils.log("Fire Veil key released");
                    }
                    break;
                case 5:
                    if (delayCounter > 0) delayCounter--;
                    else {
                        fireVeilState = 6;
                        Utils.log("FireVeilState 5 -> 6");
                    }
                    break;
                case 6:
                    inventoryAccessor.setSelectedSlot(originalSlot);
                    delayCounter = random.nextInt(4) + 2;
                    fireVeilState = 7;
                    Utils.log("Switched back to original slot " + originalSlot);
                    break;
                case 7:
                    if (delayCounter > 0) delayCounter--;
                    else {
                        fireVeilState = 8;
                        Utils.log("FireVeilState 7 -> 8");
                    }
                    break;
                case 8:
                    if (fishingKeyTimer == 0) {
                        mc.options.useKey.setPressed(true);
                        fishingKeyTimer = 1;
                        Utils.log("Re-cast key pressed (Fire Veil)");
                    }
                    fireVeilState = 0;
                    break;
            }
            return;
        }

        if (scheduledClick && fishingKeyTimer == 0) {
            mc.options.useKey.setPressed(true);
            fishingKeyTimer = 1;
            scheduledClick = false;
            hasClickedOnce = true;
            clickTimer = random.nextInt(6) + 5;
            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            Utils.log("Initial reel-in key pressed");

            if (Faketils.config.fishingHelperFireVeil) {
                var fireVeil = FireVeilDetector.findFireVeil();
                if (fireVeil != null) {
                    boolean shouldUse = true;

                    if (Faketils.config.fishingHelperFireVeilGalatea) {
                        shouldUse = random.nextInt(100) < 10;
                    }

                    if (shouldUse) {
                        originalSlot = inventoryAccessor.getSelectedSlot();
                        veilSlot = fireVeil.slot;
                        fireVeilState = 1;
                        delayCounter = random.nextInt(4) + 2;
                        hasClickedOnce = false;
                        Utils.log("Fire Veil queued, veilSlot: " + veilSlot);
                    }
                }
            }
        }

        if (clickTimer > 0 && fishingKeyTimer == 0 && hasClickedOnce) {
            clickTimer--;
            if (clickTimer == 0) {
                mc.options.useKey.setPressed(true);
                fishingKeyTimer = 1;
                hasClickedOnce = false;
                Utils.log("Re-cast key pressed (non-Fire Veil)");
            }
        }
    }

    public static class FireVeilResult {
        public final int slot;
        public final KeyBinding keyBind;

        public FireVeilResult(int slot, KeyBinding keyBind) {
            this.slot = slot;
            this.keyBind = keyBind;
        }
    }

    public static class FireVeilDetector {
        public static FireVeilResult findFireVeil() {
            var player = mc.player;
            if (player == null) return null;

            for (int slot = 0; slot <= 8; slot++) {
                var stack = player.getInventory().getStack(slot);
                if (stack.isEmpty()) continue;

                var name = stack.getName();
                if (name.getString().contains("Fire Veil Wand")) {
                    Utils.log("Veil at " + slot);
                    return new FireVeilResult(slot, mc.options.hotbarKeys[slot]);
                }
            }
            return null;
        }
    }
}