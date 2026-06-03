package com.faketils.features;

import com.faketils.Faketils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public class Harp {
    private static boolean inHarp = false;
    private static final List<Item> lastInventory = new ArrayList<>();
    private static int counter = 0;

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            onScreenOpen(screen);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            onTick(client);
        });
    }

    public static void onScreenOpen(Screen screen) {
        if (screen instanceof AbstractContainerScreen gui &&
                gui.getTitle().getString().startsWith("Harp -")) {
            lastInventory.clear();
            inHarp = true;
        }
    }

    public static void onTick(Minecraft client) {
        if (!Faketils.config().harp || client.player == null || ++counter % 2 == 0)
            return;

        if (!inHarp || !(client.screen instanceof AbstractContainerScreen gui) ||
                !gui.getTitle().getString().startsWith("Harp -")) {
            inHarp = false;
            return;
        }

        AbstractContainerMenu handler = client.player.containerMenu;
        List<Item> currentInventory = snapshotItems(handler);

        if (!lastInventory.equals(currentInventory)) {
            for (int i = 0; i < handler.slots.size(); i++) {
                if (handler.slots.get(i).getItem().is(Items.QUARTZ_BLOCK)) {
                    Experiments.clickSlot(client, handler, i, 2, ContainerInput.CLONE);
                    break;
                }
            }
        }

        lastInventory.clear();
        lastInventory.addAll(currentInventory);
    }

    public static List<Item> snapshotItems(AbstractContainerMenu handler) {
        return handler.slots.stream()
                .map(Slot::getItem)
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::getItem)
                .toList();
    }
}