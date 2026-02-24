package com.faketils.features;

import com.faketils.events.FtEvent;
import com.faketils.events.FtEventBus;
import com.faketils.events.PacketEvent;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class DanceRoomSolver {

    private static final double TARGET_X = -264.0;
    private static final double TARGET_Y = 33.0;
    private static final double TARGET_Z = -107.0;
    private static final double TOLERANCE = 4.0;

    private static final double[] PITCH_LIST = {
            0.523809552192688,   // F#
            1.047619104385376,   // C
            0.6984127163887024,  // G
            0.8888888955116272   // A
    };

    private static int jumpDelay = 500;
    private static int punchDelay = 800;
    private static boolean isActive = false;
    private static int beats = -1;
    private static boolean pendingStart = false;
    private static float lastMasterVolume = 1.0f;

    private static KeyBinding currentDirectionKey = null;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());
        FtEventBus.onEvent(FtEvent.HudRender.class, hud -> onHudRender(hud.context));

        registerCommands();

        PacketEvent.registerReceive(DanceRoomSolver::onPacketReceive);
    }

    private static void onPacketReceive(Packet<?> packet, net.minecraft.network.ClientConnection connection) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (packet instanceof TitleS2CPacket titlePacket) {
            Text subtitle = titlePacket.text();

            if (subtitle != null) {
                String cleanSubtitle = subtitle.getString().replaceAll("§.", "");
                client.execute(() -> {
                    if (inDanceRoom() && cleanSubtitle.equals("Move!") && !isActive) {
                        chat("Dance Room Solver activated!");
                        setActive();
                    }
                });
            }
        }

        if (packet instanceof PlaySoundS2CPacket soundPacket) {
            String soundName = soundPacket.getSound().getIdAsString();
            float volume = soundPacket.getVolume();
            float pitch = soundPacket.getPitch();

            client.execute(() -> {
                if (!isActive || !inDanceRoom()) return;

                if (soundName.contains("entity.player.burp")) {
                    chat("Turning off module, try again.");
                    setInactive();
                } else if (soundName.contains("block.note_block.bass") && volume == 1.0f) {
                    boolean pitchMatches = false;
                    for (double p : PITCH_LIST) {
                        if (Math.abs(p - pitch) < 0.0001) {
                            pitchMatches = true;
                            break;
                        }
                    }
                    if (pitchMatches) {
                        beats++;
                        doMove(beats);
                    } else if (Math.abs(pitch - 0.7460317611694336) < 0.0001) {
                        chat("Completed! Toggling off.");
                        setInactive();
                    }
                }
            });
        }
    }

    private static void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (!inStartArea(mc.player)) {
            pendingStart = false;
            return;
        }

        if (!isActive && inStartArea(mc.player) && !pendingStart) {
            pendingStart = true;
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                //releaseAllMovementKeys();
                chat("Hold still!");
                setActive();
            }).start();
        }

        if (isActive && !inDanceRoom(mc.player)) {
            chat("Left dance room! Deactivating.");
            setInactive();
        }
    }

    private static void onHudRender(DrawContext context) {
        if (!isActive || !inDanceRoom()) return;

        String text = "Dance Room!";
        MinecraftClient mc = MinecraftClient.getInstance();
        int x = context.getScaledWindowWidth() / 2 - mc.textRenderer.getWidth(text) / 2;
        int y = context.getScaledWindowHeight() / 2 + 30;
        context.drawText(mc.textRenderer, text, x, y, 0xFFFFFF, false);
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("drs")
                    .executes(context -> openHelp(context.getSource()))
                    .then(literal("start")
                            .executes(context -> handleStart(context.getSource()))
                    )
                    .then(literal("stop")
                            .executes(context -> handleStop(context.getSource()))
                    )
                    .then(literal("jumpdelay")
                            .then(argument("ms", IntegerArgumentType.integer(0, 5000))
                                    .executes(context -> handleJumpDelay(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "ms")
                                    ))
                            )
                    )
                    .then(literal("punchdelay")
                            .then(argument("ms", IntegerArgumentType.integer(0, 5000))
                                    .executes(context -> handlePunchDelay(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "ms")
                                    ))
                            )
                    )
            );

            dispatcher.register(literal("danceroomsolver").executes(context -> openHelp(context.getSource())));
        });
    }

    private static int openHelp(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("§6§lDance Room Solver §7- Commands:"));
        source.sendFeedback(Text.literal("§7/drs start §8- §aActivate the solver"));
        source.sendFeedback(Text.literal("§7/drs stop §8- §cDeactivate the solver"));
        source.sendFeedback(Text.literal("§7/drs jumpdelay <ms> §8- §7Set jump delay (current: §e" + jumpDelay + "ms§7)"));
        source.sendFeedback(Text.literal("§7/drs punchdelay <ms> §8- §7Set punch delay (current: §e" + punchDelay + "ms§7)"));
        source.sendFeedback(Text.literal("§7Status: " + (isActive ? "§aActive" : "§cInactive")));
        return 1;
    }

    private static int handleStart(FabricClientCommandSource source) {
        setActive();
        source.sendFeedback(Text.literal("§7[§6DRS§7] §aDance Room Solver activated."));
        return 1;
    }

    private static int handleStop(FabricClientCommandSource source) {
        setInactive();
        source.sendFeedback(Text.literal("§7[§6DRS§7] §cDance Room Solver deactivated."));
        return 1;
    }

    private static int handleJumpDelay(FabricClientCommandSource source, int ms) {
        jumpDelay = ms;
        source.sendFeedback(Text.literal("§7[§6DRS§7] §aJump delay set to §e" + jumpDelay + "ms"));
        return 1;
    }

    private static int handlePunchDelay(FabricClientCommandSource source, int ms) {
        punchDelay = ms;
        source.sendFeedback(Text.literal("§7[§6DRS§7] §aPunch delay set to §e" + punchDelay + "ms"));
        return 1;
    }

    private static boolean inDanceRoom() {
        return inDanceRoom(MinecraftClient.getInstance().player);
    }

    private static boolean inDanceRoom(net.minecraft.entity.player.PlayerEntity player) {
        if (player == null) return false;
        return Math.abs(player.getX() - TARGET_X) < TOLERANCE &&
                Math.abs(player.getY() - TARGET_Y) < TOLERANCE &&
                Math.abs(player.getZ() - TARGET_Z) < TOLERANCE;
    }

    private static boolean inStartArea(net.minecraft.entity.player.PlayerEntity player) {
        if (player == null) return false;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        return x >= -264.999 && x <= -264.0 &&
                y >= 32.9 && y <= 33.1 &&
                z >= -107.999 && z <= -107.0;
    }

    private static void chat(String message) {
        if (MinecraftClient.getInstance().player == null) return;
        MinecraftClient.getInstance().player.sendMessage(Text.literal("§6[DRS] §7" + message), false);
    }

    private static void setKeyState(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
    }

    private static void setDirection(KeyBinding newKey) {
        if (currentDirectionKey != null) {
            setKeyState(currentDirectionKey, false);
        }
        if (newKey != null) {
            setKeyState(newKey, true);
        }
        currentDirectionKey = newKey;
    }

    private static void setRotation() {
        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.setYaw(90f);
            player.setPitch(90f);
        }
    }

    private static void toggleGuardBlocks(boolean active) {
        var world = MinecraftClient.getInstance().world;
        if (world == null) return;
        var block = active ? Blocks.BARRIER : Blocks.AIR;

        for (int x = -266; x <= -261; x++) {
            for (int z = -109; z <= -104; z++) {
                for (int y = 33; y <= 34; y++) {
                    if (x == -266 || x == -262 || z == -109 || z == -105) {
                        BlockPos pos = new BlockPos(x, y, z);
                        world.setBlockState(pos, block.getDefaultState());
                        world.updateNeighbors(pos, block);
                    }
                }
            }
        }
    }

    private static void setInactive() {
        isActive = false;
        beats = -1;
        setDirection(null);
        setKeyState(MinecraftClient.getInstance().options.sneakKey, false);
        setKeyState(MinecraftClient.getInstance().options.jumpKey, false);
        toggleGuardBlocks(false);
        MinecraftClient.getInstance().options.getSoundVolumeOption(SoundCategory.MASTER).setValue((double) lastMasterVolume);
    }

    private static void setActive() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var masterOption = mc.options.getSoundVolumeOption(SoundCategory.MASTER);
        lastMasterVolume = masterOption.getValue().floatValue();

        if (lastMasterVolume == 0f) {
            chat("Turning volume on because it was off!");
            masterOption.setValue(0.01);
            lastMasterVolume = 0.01f;
        }

        isActive = true;
        setRotation();
        //toggleGuardBlocks(true);
        beats = -1;
        chat("Enabled! Follow the moves.");
    }

    private static void doMove(int beat) {
        if (beat == 0 || beat % 2 == 1) {
            int index = (int) (Math.ceil(beat / 2.0) % 4);
            KeyBinding[] cycle = { MinecraftClient.getInstance().options.leftKey, MinecraftClient.getInstance().options.backKey, MinecraftClient.getInstance().options.rightKey, MinecraftClient.getInstance().options.forwardKey };
            //setDirection(cycle[index]);
        }

        if (beat >= 8) {
            switch (beat % 4) {
                case 0:
                    setKeyState(MinecraftClient.getInstance().options.sneakKey, true);
                    break;
                case 1:
                    setKeyState(MinecraftClient.getInstance().options.sneakKey, false);
                    break;
            }
        }

        if (beat >= 24 && (beat % 8 == 0 || beat % 8 == 2)) {
            executor.submit(() -> {
                try {
                    Thread.sleep(jumpDelay);
                    MinecraftClient.getInstance().execute(() -> setKeyState(MinecraftClient.getInstance().options.jumpKey, true));
                    Thread.sleep(100);
                    MinecraftClient.getInstance().execute(() -> setKeyState(MinecraftClient.getInstance().options.jumpKey, false));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        if (beat >= 64 && beat % 2 == 0) {
            executor.submit(() -> {
                try {
                    Thread.sleep(punchDelay);
                    //MinecraftClient.getInstance().execute(() -> setKeyState(MinecraftClient.getInstance().options.attackKey, true));
                    //Thread.sleep(40);
                    //MinecraftClient.getInstance().execute(() -> setKeyState(MinecraftClient.getInstance().options.attackKey, false));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    private static void releaseAllMovementKeys() {
        setKeyState(MinecraftClient.getInstance().options.forwardKey, false);
        setKeyState(MinecraftClient.getInstance().options.backKey, false);
        setKeyState(MinecraftClient.getInstance().options.leftKey, false);
        setKeyState(MinecraftClient.getInstance().options.rightKey, false);
        setKeyState(MinecraftClient.getInstance().options.sneakKey, false);
        setKeyState(MinecraftClient.getInstance().options.jumpKey, false);
    }
}