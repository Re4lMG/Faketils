package com.faketils.commands;

import com.faketils.Faketils;
import com.faketils.config.Config;
import com.faketils.events.FlyHandler;
import com.faketils.events.RotationHandler;
import com.faketils.utils.FarmingWaypoints;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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

            dispatcher.register(literal("setrotation")
                    .then(argument("yaw", FloatArgumentType.floatArg())
                            .then(argument("pitch", FloatArgumentType.floatArg(-90.0f, 90.0f))
                                    .executes(context -> {
                                        float yaw = FloatArgumentType.getFloat(context, "yaw");
                                        float pitch = FloatArgumentType.getFloat(context, "pitch");

                                        RotationHandler.setTarget(yaw, pitch);

                                        context.getSource().sendFeedback(
                                                Text.literal("§7[§bFaketils§7] §aRotation target: yaw=§e" + yaw + "§a, pitch=§e" + pitch)
                                        );
                                        return 1;
                                    })
                            )
                    )
            );

            dispatcher.register(literal("flyto")
                    .then(argument("x", IntegerArgumentType.integer())
                            .then(argument("y", IntegerArgumentType.integer())
                                    .then(argument("z", IntegerArgumentType.integer())
                                    .executes(context -> {
                                        int x = IntegerArgumentType.getInteger(context, "x");
                                        int y = IntegerArgumentType.getInteger(context, "y");
                                        int z = IntegerArgumentType.getInteger(context, "z");

                                        FlyHandler.setTarget(new Vec3d(x, y, z));

                                        context.getSource().sendFeedback(
                                                Text.literal("§7[§bFaketils§7] §aTarget set")
                                        );
                                        return 1;
                                    })
                            )
                    )
            ));

            dispatcher.register(literal("resetrotation")
                    .executes(context -> {
                        RotationHandler.reset();
                        FlyHandler.stop();
                        context.getSource().sendFeedback(
                                Text.literal("§7[§bFaketils§7] §eRotation reset")
                        );
                        return 1;
                    })
            );
        });

        FarmingWaypoints.load();
    }

    private static int openGui(FabricClientCommandSource source) {
        net.minecraft.client.MinecraftClient.getInstance().setScreen(Config.createScreen(MinecraftClient.getInstance().currentScreen));
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