package com.faketils.features;

import com.faketils.Faketils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

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
        if (screen instanceof GenericContainerScreen gui &&
                gui.getTitle().getString().startsWith("Harp -")) {
            lastInventory.clear();
            inHarp = true;
        }

    }

    public static void onTick(MinecraftClient client) {
        if (!Faketils.config().harp || client.player == null || ++counter % 2 == 0)
            return;

        if (!inHarp || !(client.currentScreen instanceof GenericContainerScreen gui) ||
                !gui.getTitle().getString().startsWith("Harp -")) {
            inHarp = false;
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        List<Item> currentInventory = snapshotItems(handler);

        if (!lastInventory.equals(currentInventory)) {
            for (int i = 0; i < handler.slots.size(); i++) {
                if (handler.slots.get(i).getStack().isOf(Items.QUARTZ_BLOCK)) {
                    Experiments.clickSlot(client, handler, i, 2, SlotActionType.CLONE);
                    break;
                }
            }
        }

        lastInventory.clear();
        lastInventory.addAll(currentInventory);
    }

    public static List<Item> snapshotItems(ScreenHandler handler) {
        return handler.slots.stream()
                .map(Slot::getStack)
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::getItem)
                .toList();
    }
}
