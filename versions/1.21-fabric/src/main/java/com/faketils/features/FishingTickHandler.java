package com.faketils.features;

import com.faketils.config.Config;
import com.faketils.mixin.PlayerInventoryAccessor;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class FishingTickHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
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

    private static boolean slugFishingActive = false;
    private static long slugStartTime = 0L;
    private static int lastBobberId = -1;

    private static final Set<Integer> handledArmorStands = new HashSet<>();

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());
        WorldRenderEvents.AFTER_ENTITIES.register(context -> onRenderWorldLast());
    }

    private static void onRenderWorldLast() {
        if (!Utils.isInSkyblock() || !Config.INSTANCE.fishingHelper) return;
        if (mc.currentScreen != null || mc.player == null || mc.world == null) return;

        ItemStack heldItem = mc.player.getMainHandStack();
        if (!(heldItem.getItem() instanceof FishingRodItem)) return;

        handledArmorStands.removeIf(id -> mc.world.getEntityById(id) == null);

        for (ArmorStandEntity armorStand : mc.world.getEntitiesByClass(ArmorStandEntity.class,
                mc.player.getBoundingBox().expand(64), e -> true)) {

            if (armorStand.isRemoved() || !armorStand.hasCustomName()) continue;

            Text customName = armorStand.getCustomName();
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
        if (!Utils.isInSkyblock() || !Config.INSTANCE.fishingHelper) return;
        if (mc.currentScreen != null || mc.player == null || mc.interactionManager == null) return;

        var player = mc.player;
        var interactionManager = mc.interactionManager;
        PlayerInventoryAccessor inventory = (PlayerInventoryAccessor) player.getInventory();

        if (Config.INSTANCE.slugFishing) {
            var bobber = player.fishHook;
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

            simulateUseItem(interactionManager);

            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            scheduledClick = false;

            if (Config.INSTANCE.fishingHelperKilling && !Config.INSTANCE.fishingHelperKillingWeapon.isEmpty()) {
                WeaponResult weapon = WeaponDetector.findWeapon();
                if (weapon != null) {
                    originalSlot = inventory.getSelectedSlot();
                    weaponSlot = weapon.slot;
                    weaponState = 1;
                    delayCounter = random.nextInt(4) + 2;
                    Utils.log("Weapon found in slot " + weaponSlot + ", switching...");
                }
            }
        }

        if (weaponState > 0) {
            switch (weaponState) {
                case 1 -> {
                    if (delayCounter-- <= 0) {
                        weaponState = 2;
                    }
                }
                case 2 -> {
                    inventory.setSelectedSlot(weaponSlot);
                    delayCounter = random.nextInt(4) + 2;
                    weaponState = 3;
                }
                case 3 -> {
                    if (delayCounter-- <= 0) {
                        weaponState = 4;
                    }
                }
                case 4 -> {
                    if (delayCounter-- <= 0) {
                        simulateUseItem(interactionManager);
                        clickCount++;
                        delayCounter = random.nextInt(4) + 3;

                        int maxClicks = Config.INSTANCE.fishingHelperKillingAmount + 1;
                        if (clickCount >= maxClicks) {
                            clickCount = 0;
                            weaponState = 5;
                            delayCounter = random.nextInt(4) + 2;
                        }
                    }
                }
                case 5 -> {
                    if (delayCounter-- <= 0) {
                        weaponState = 6;
                    }
                }
                case 6 -> {
                    inventory.setSelectedSlot(originalSlot);
                    weaponState = 0;
                    Utils.log("Switched back to original slot.");
                }
            }
            return;
        }

        if (clickTimer > 0) {
            clickTimer--;
            if (clickTimer == 0 && hasClickedOnce) {
                simulateUseItem(interactionManager);
                hasClickedOnce = false;
            }
        }
    }

    private static void simulateUseItem(ClientPlayerInteractionManager interactionManager) {
        interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
    }

    public static class WeaponResult {
        public final int slot;
        public final KeyBinding hotbarKey;

        public WeaponResult(int slot, KeyBinding hotbarKey) {
            this.slot = slot;
            this.hotbarKey = hotbarKey;
        }
    }

    public static class WeaponDetector {
        public static WeaponResult findWeapon() {
            var player = mc.player;
            if (player == null) return null;

            String targetName = Config.INSTANCE.fishingHelperKillingWeapon;

            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = player.getInventory().getStack(slot);
                if (stack.isEmpty()) continue;

                Text displayName = stack.getName();
                if (displayName.getString().contains(targetName)) {
                    Utils.log("Found weapon '" + targetName + "' in slot " + slot);
                    return new WeaponResult(slot, mc.options.hotbarKeys[slot]);
                }
            }
            return null;
        }
    }
}