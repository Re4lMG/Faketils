package com.faketils.features;

import com.faketils.Faketils;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Experiments {
    private enum ExperimentType {
        CHRONOMATRON,
        ULTRASEQUENCER,
        SUPERPAIRS,
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

        if (!(screen instanceof AbstractContainerScreen)) {
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

    public static void onTick(Minecraft client) {
        if (!Faketils.config().exp ||
                currentExperiment == ExperimentType.NONE ||
                client.player == null) {
            return;
        }

        if (!(client.screen instanceof AbstractContainerScreen)) {
            clearAll();
            return;
        }

        AbstractContainerMenu handler = client.player.containerMenu;
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

    private static void tickEnd(Minecraft client, long now) {
        if (endDelay == -1) {
            endDelay = now + rng.nextInt(END_DELAY_MAX - END_DELAY_MIN) + END_DELAY_MIN;
            Utils.log("End delay: " + (endDelay - now) + "ms");
        }

        if (now > endDelay) {
            client.player.closeContainer();
            clearAll();
        }
    }

    private static void tickChrono(Minecraft client, AbstractContainerMenu handler, long now) {
        ItemStack flag = handler.slots.get(49).getItem();
        List<Slot> container = handler.slots;

        if (flag.is(Items.GLOWSTONE) &&
                !container.get(lastAdded).getItem().hasFoil()) {
            sequenceAdded = false;
            if (chronomatronOrder.size() > 10) {
                client.player.closeContainer();
            }
        }

        if (!sequenceAdded && flag.is(Items.CLOCK)) {
            for (int i = 10; i <= 43; i++) {
                ItemStack stack = container.get(i).getItem();
                if (!stack.isEmpty() && stack.hasFoil()) {
                    chronomatronOrder.add(i);
                    Utils.log("Added glowing slot: " + i);
                    lastAdded = i;
                    sequenceAdded = true;
                    clicks = 0;
                    clickDelay = -1;
                    break;
                }
            }

            if (!sequenceAdded) {
                Utils.log("No glowing items found.");
                sequenceAdded = true;
            }
        }

        if (sequenceAdded && flag.is(Items.CLOCK) &&
                chronomatronOrder.size() > clicks) {

            if (clickDelay == -1) {
                clickDelay = now + rng.nextInt(1000 - 250) + 250;
                Utils.log("Chrono Click " + (clicks + 1) + " in " + (clickDelay - now) + "ms");
            }

            if (now > clickDelay) {
                clickSlot(client, handler, chronomatronOrder.get(clicks), 2, ContainerInput.CLONE);
                clicks++;
                clickDelay = -1;
            }
        }
    }

    private static void tickUltra(Minecraft client, AbstractContainerMenu handler, long now) {
        ItemStack flag = handler.slots.get(49).getItem();
        List<Slot> container = handler.slots;

        if (flag.is(Items.CLOCK)) {
            sequenceAdded = false;
        }

        if (!sequenceAdded && flag.is(Items.GLOWSTONE)) {
            if (!container.get(44).hasItem()) return;

            ultrasequencerOrder.clear();

            for (int i = 9; i <= 44; i++) {
                ItemStack stack = container.get(i).getItem();
                Item item = stack.getItem();
                if (item instanceof DyeItem || item == Items.BONE_MEAL || item == Items.INK_SAC ||
                        item == Items.LAPIS_LAZULI || item == Items.COCOA_BEANS) {
                    ultrasequencerOrder.put(stack.getCount() - 1, i);
                }
            }

            sequenceAdded = true;
            clicks = 0;
            clickDelay = -1;
        }

        if (flag.is(Items.CLOCK) && ultrasequencerOrder.containsKey(clicks)) {
            if (clickDelay == -1) {
                clickDelay = now + rng.nextInt(750 - 250) + 250;
                Utils.log("Ultra Click " + (clicks + 1) + " in " + (clickDelay - now) + "ms");
            }

            if (now > clickDelay) {
                if (ultrasequencerOrder.size() > 6) {
                    client.player.closeContainer();
                }

                Integer slot = ultrasequencerOrder.get(clicks);
                if (slot != null) {
                    clickSlot(client, handler, slot, 2, ContainerInput.CLONE);
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

    public static void clickSlot(Minecraft client, AbstractContainerMenu handler, int slot, int button, ContainerInput action) {
        assert client.gameMode != null;

        client.gameMode.handleContainerInput(
                handler.containerId,
                slot,
                button,
                action,
                client.player
        );
    }
}