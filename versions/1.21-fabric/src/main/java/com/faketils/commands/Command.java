package com.faketils.commands;

import com.faketils.Faketils;
import com.faketils.utils.FarmingWaypoints;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class Command {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("ft")
                    .executes(context -> openGui(context.getSource()))
                    .then(literal("set")
                            .then(argument("action", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        builder.suggest("right");
                                        builder.suggest("left");
                                        builder.suggest("warp");
                                        builder.suggest("reset");
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> handleSet(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "action")
                                    ))
                            )
                    )
            );

            dispatcher.register(literal("faketils").executes(context -> openGui(context.getSource())));
            dispatcher.register(literal("faketil").executes(context -> openGui(context.getSource())));
        });

        FarmingWaypoints.load();
    }

    private static int openGui(FabricClientCommandSource source) {
        Faketils.currentGui = Faketils.config.gui();
        return 1;
    }

    private static int handleSet(FabricClientCommandSource source, String action) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return 0;

        action = action.toLowerCase();

        if (action.equals("reset")) {
            FarmingWaypoints.WAYPOINTS.clear();
            FarmingWaypoints.save();
            source.sendFeedback(Text.literal("§7[§bFaketils§7] §eAll waypoints cleared!"));
            return 1;
        }

        double yOffset = 0.5;
        BlockPos pos = BlockPos.ofFloored(
                mc.player.getX(),
                mc.player.getY() + yOffset,
                mc.player.getZ()
        );

        switch (action) {
            case "right" -> {
                FarmingWaypoints.WAYPOINTS.computeIfAbsent("right", k -> new java.util.ArrayList<>()).add(pos);
                source.sendFeedback(Text.literal("§7[§bFaketils§7] §aRight waypoint added!"));
            }
            case "left" -> {
                FarmingWaypoints.WAYPOINTS.computeIfAbsent("left", k -> new java.util.ArrayList<>()).add(pos);
                source.sendFeedback(Text.literal("§7[§bFaketils§7] §cLeft waypoint added!"));
            }
            case "warp" -> {
                FarmingWaypoints.WAYPOINTS.computeIfAbsent("warp", k -> new java.util.ArrayList<>()).add(pos);
                source.sendFeedback(Text.literal("§7[§bFaketils§7] §eWarp waypoint added!"));
            }
            default -> {
                source.sendFeedback(Text.literal("§7[§bFaketils§7] §cInvalid argument. Use right/left/warp/reset"));
                return 0;
            }
        }

        FarmingWaypoints.save();
        return 1;
    }
}