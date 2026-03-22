package com.faketils.features;

import com.faketils.Faketils;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Experiments {
    private enum ExperimentType {
        CHRONOMATRON,
        ULTRASEQUENCER,
        SUPERPAIRS, // not implemented
        END,
        NONE
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
                onScreenOpen(screen);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
                onTick(client);
        });
    }

    private static final int START_DELAY_MIN = 234;
    private static final int START_DELAY_MAX = 678;
    private static final int END_DELAY_MIN = 777;
    private static final int END_DELAY_MAX = 3333;

    private static final Random rng = new Random();

    private static ExperimentType currentExperiment = ExperimentType.NONE;
    private static final ArrayList<Integer> chronomatronOrder = new ArrayList<>(28);
    private static final Map<Integer, Integer> ultrasequencerOrder = new HashMap<>();

    private static int lastAdded = 0, clicks = 0;
    private static long startDelay = -1, endDelay = -1, clickDelay = -1;
    private static boolean sequenceAdded = false;

    public static void onScreenOpen(Screen screen) {
        if (!Faketils.config().exp) {
            clearAll();
            return;
        }

        if (!(screen instanceof GenericContainerScreen)) {
            clearAll();
            return;
        }

        String title = screen.getTitle().getString();
        if (title.startsWith("Chronomatron (")) {
            Utils.log("Chronomatron detected");
            currentExperiment = ExperimentType.CHRONOMATRON;
        } else if (title.startsWith("Ultrasequencer (")) {
            Utils.log("Ultrasequencer detected");
            currentExperiment = ExperimentType.ULTRASEQUENCER;
        } else if (title.contains("Over")) {
            Utils.log("Experiment over.");
            currentExperiment = ExperimentType.END;
        } else {
            clearAll();
        }
    }

    public static void onTick(MinecraftClient client) {
        if (!Faketils.config().exp ||
                currentExperiment == ExperimentType.NONE ||
                client.player == null) {
            return;
        }

        if (!(client.currentScreen instanceof GenericContainerScreen)) {
            clearAll();
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        ItemStack center = handler.slots.get(49).getStack();
        long now = System.currentTimeMillis();

        if (startDelay == -1) {
            startDelay = now + rng.nextInt(START_DELAY_MAX - START_DELAY_MIN) + START_DELAY_MIN;
            Utils.log("Start delay: " + (startDelay - now));
        }

        if (now < startDelay) return;

        switch (currentExperiment) {
            case CHRONOMATRON -> tickChrono(client, handler, now);
            case ULTRASEQUENCER -> tickUltra(client, handler, now);
            case END -> tickEnd(client, now);
            default -> {}
        }
    }

    private static void tickEnd(MinecraftClient client, long now) {
        if (endDelay == -1) {
            endDelay = now + rng.nextInt(END_DELAY_MAX - END_DELAY_MIN) + END_DELAY_MIN;
            Utils.log("End delay: " + (endDelay - now) + "ms");
        }

        if (now > endDelay) {
            client.player.closeHandledScreen();
            clearAll();
        }
    }

    private static void tickChrono(MinecraftClient client, ScreenHandler handler, long now) {
        ItemStack flag = handler.slots.get(49).getStack();
        DefaultedList<Slot> container = handler.slots;

        if (flag.isOf(Items.GLOWSTONE) &&
                !container.get(lastAdded).getStack().hasGlint()) {
            sequenceAdded = false;
            if (chronomatronOrder.size() > 10) {
                client.player.closeHandledScreen();
            }
        }

        if (!sequenceAdded && flag.isOf(Items.CLOCK)) {
            for (int i = 10; i <= 43; i++) {
                ItemStack stack = container.get(i).getStack();
                if (!stack.isEmpty() && stack.hasGlint()) {
                    chronomatronOrder.add(i);
                    Utils.log("Added glowing slot: " + i);
                    lastAdded = i;
                    sequenceAdded = true;
                    clicks = 0;
                    break;
                }
            }

            if (!sequenceAdded) {
                Utils.log("No glowing items found.");
                sequenceAdded = true;
            }
        }

        if (sequenceAdded && flag.isOf(Items.CLOCK) &&
                chronomatronOrder.size() > clicks) {

            if (clickDelay == -1) {
                clickDelay = now + rng.nextInt(750 - 250) + 250;
                Utils.log("Chrono Click " + (clicks + 1) + " in " + (clickDelay - now) + "ms");
            }

            if (now > clickDelay) {
                clickSlot(client, handler, chronomatronOrder.get(clicks), 2, SlotActionType.CLONE);
                clicks++;
                clickDelay = -1;
            }
        }
    }

    private static void tickUltra(MinecraftClient client, ScreenHandler handler, long now) {
        ItemStack flag = handler.slots.get(49).getStack();
        DefaultedList<Slot> container = handler.slots;

        if (flag.isOf(Items.CLOCK)) {
            sequenceAdded = false;
        }

        if (!sequenceAdded && flag.isOf(Items.GLOWSTONE)) {
            if (!container.get(44).hasStack()) return;

            ultrasequencerOrder.clear();

            for (int i = 9; i <= 44; i++) {
                ItemStack stack = container.get(i).getStack();
                Item item = stack.getItem();
                if (item instanceof DyeItem || item == Items.BONE_MEAL || item == Items.INK_SAC ||
                        item == Items.LAPIS_LAZULI || item == Items.COCOA_BEANS) {
                    ultrasequencerOrder.put(stack.getCount() - 1, i);
                }
            }

            sequenceAdded = true;
            clicks = 0;
        }

        if (flag.isOf(Items.CLOCK) && ultrasequencerOrder.containsKey(clicks)) {
            if (clickDelay == -1) {
                clickDelay = now + rng.nextInt(750 - 250) + 250;
                Utils.log("Ultra Click " + (clicks + 1) + " in " + (clickDelay - now) + "ms");
            }

            if (now > clickDelay) {
                if (ultrasequencerOrder.size() > 6) {
                    client.player.closeHandledScreen();
                }

                Integer slot = ultrasequencerOrder.get(clicks);
                if (slot != null) {
                    clickSlot(client, handler, slot, 2, SlotActionType.CLONE);
                    clicks++;
                    clickDelay = -1;
                }
            }
        }
    }

    private static void clearAll() {
        currentExperiment = ExperimentType.NONE;
        chronomatronOrder.clear();
        ultrasequencerOrder.clear();
        sequenceAdded = false;
        lastAdded = 0;
        clickDelay = -1;
        endDelay = -1;
        startDelay = -1;
    }

    public static void clickSlot(MinecraftClient client, ScreenHandler handler, int slot,
                                 int button, SlotActionType actionType) {
        assert client.interactionManager != null;
        client.interactionManager.clickSlot(
                handler.syncId,
                slot,
                button,
                actionType,
                client.player
        );
    }
}