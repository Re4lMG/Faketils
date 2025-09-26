package com.faketils.features

import com.faketils.Faketils
import com.faketils.commands.FarmingCommand
import com.faketils.events.PacketEvent
import com.faketils.utils.TitleUtil
import com.faketils.utils.Utils
import kotlin.math.abs
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import net.minecraft.block.Block
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.BlockPos
import net.minecraft.util.MouseHelper
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.event.MouseEvent

class Farming {
    lateinit var toggleKey: KeyBinding
    lateinit var fakeKey: KeyBinding
    lateinit var pauseKey: KeyBinding

    var isActive = false
    var isPaused = false
    var currentMode = "none"

    private var lastWaypoint: BlockPos? = null
    private var ticksOnWaypoint = 0
    private var randomDelayTicks = (20..100).random()

    private var lastBrokenBlock = 0L
    private var blocksBroken = 0
    private var isBreaking = false
    private var startTime = 0L
    private var bps = 0.0
    private var lockedYaw = 0f
    private var lockedPitch = 0f
    private var lockedSlot = -1
    private var bpsZeroStartTime = 0L
    private val yawPitchTolerance = 1.0f
    private var lockedItemName: String? = null

    private var originalMouseSensitivity = 0.0f
    private var isMouseLocked = false

    private val waypointColors = mapOf(
        "right" to intArrayOf(0, 255, 0),    // Green
        "left" to intArrayOf(255, 0, 0),     // Red
        "warp" to intArrayOf(255, 255, 0)    // Yellow
    )

    private val farmableBlocks: Set<Block> = setOf(
        Blocks.wheat,
        Blocks.carrots,
        Blocks.potatoes,
        Blocks.pumpkin,
        Blocks.melon_block,
        Blocks.reeds,           // sugar cane
        Blocks.cactus,
        Blocks.cocoa,
        Blocks.red_mushroom,
        Blocks.brown_mushroom,
        Blocks.nether_wart
    )

    val mc = Minecraft.getMinecraft()

    fun init() {
        toggleKey = KeyBinding("Macro Toggle", Keyboard.KEY_F8, "Faketils")
        ClientRegistry.registerKeyBinding(toggleKey)

        pauseKey = KeyBinding("Pause & Unpause Macro", Keyboard.KEY_P, "Faketils")
        ClientRegistry.registerKeyBinding(pauseKey)

        fakeKey = KeyBinding("Reset fake fails", Keyboard.KEY_BACK, "Faketils")
        ClientRegistry.registerKeyBinding(fakeKey)
    }

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Send) {
        val packet = event.packet
        if (packet is C07PacketPlayerDigging &&
            packet.status == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK
        ) {
            val pos = packet.position
            val block = mc.theWorld.getBlockState(pos).block

            if (block in farmableBlocks) {
                if (startTime == 0L) startTime = System.currentTimeMillis()
                isBreaking = true
                blocksBroken++
                lastBrokenBlock = System.currentTimeMillis()
            }
        }
    }

    @SubscribeEvent
    fun onWorldUnLoad(event: WorldEvent.Unload) {
        if (mc.thePlayer != null && mc.theWorld != null) {
            isActive = false
            releaseAllKeys()
            currentMode = "none"
            unlockMouse()
        }
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (mc.currentScreen != null) return
        if (!Utils.isInSkyblock()) return
        if (!Faketils.config.funnyToggle) return

        if (toggleKey.isPressed) {
            isActive = !isActive
            isPaused = false
            if (!isActive) {
                releaseAllKeys()
                currentMode = "none"
                lastWaypoint = null
                ticksOnWaypoint = 0
                unlockMouse()
            } else {
                mc.thePlayer?.let {
                    lockedYaw = it.rotationYaw
                    lockedPitch = it.rotationPitch
                    lockedSlot = it.inventory.currentItem
                    lockedItemName = it.heldItem?.displayName
                }
                lockMouse()
            }
        }

        if (isActive && pauseKey.isPressed) {
            isPaused = !isPaused
            if (isPaused) {
                releaseAllKeys()
                unlockMouse()
                Utils.log("Macro paused")
            } else {
                lockMouse()
                Utils.log("Macro resumed")
            }
        }

        if (!isActive || isPaused) return

        if (fakeKey.isPressed) {
            mc.thePlayer?.let {
                lockedYaw = it.rotationYaw
                lockedPitch = it.rotationPitch
                lockedSlot = it.inventory.currentItem
                lockedItemName = it.heldItem?.displayName
            }
        }

        if (isBreaking) {
            val secondsElapsed = (System.currentTimeMillis() - startTime) / 1000.0
            bps = blocksBroken / secondsElapsed
            if (System.currentTimeMillis() - lastBrokenBlock > 1000) {
                bps = 0.0
                isBreaking = false
                blocksBroken = 0
                startTime = 0
                lastBrokenBlock = 0
            }
        }

        val player = mc.thePlayer ?: return
        val pos = BlockPos(player.posX.toInt(), player.posY.toInt(), player.posZ.toInt())

        val rightList = FarmingCommand.waypoints["right"] ?: emptyList()
        val leftList = FarmingCommand.waypoints["left"] ?: emptyList()
        val warpList = FarmingCommand.waypoints["warp"] ?: emptyList()

        if (isActive && player.inventory.currentItem != lockedSlot) {
            mc.thePlayer?.playSound("random.anvil_land", 1.0f, 1.0f)
            TitleUtil.showTitle("Slot changed")
            Utils.log("Slot changed")
        }

        if (isActive) {
            val currentName = player.heldItem?.displayName
            if (currentName != lockedItemName) {
                mc.thePlayer?.playSound("random.anvil_land", 1.0f, 1.0f)
                TitleUtil.showTitle("Item changed")
                Utils.log("Item changed from $lockedItemName to $currentName")
            }
        }

        if (abs(player.rotationYaw - lockedYaw) > yawPitchTolerance ||
            abs(player.rotationPitch - lockedPitch) > yawPitchTolerance) {
            mc.thePlayer?.playSound("random.anvil_land", 1.0f, 1.0f)
            TitleUtil.showTitle("Yaw/pitch changed")
            Utils.log("Yaw/pitch changed")
        }

        val targetMode = when {
            rightList.any { it == pos } -> "right"
            leftList.any { it == pos } -> "left"
            warpList.any { it == pos } -> "warp"
            else -> "none"
        }

        if (isActive) {
            if (bps == 0.0) {
                if (bpsZeroStartTime == 0L) {
                    bpsZeroStartTime = System.currentTimeMillis()
                } else {
                    val playerPos = BlockPos(player.posX.toInt(), player.posY.toInt(), player.posZ.toInt())
                    val isOnWaypoint =
                        rightList.any { Math.abs(playerPos.x - it.x) <= 2 && Math.abs(playerPos.z - it.z) <= 2 } ||
                                leftList.any { Math.abs(playerPos.x - it.x) <= 2 && Math.abs(playerPos.z - it.z) <= 2 } ||
                                warpList.any { Math.abs(playerPos.x - it.x) <= 2 && Math.abs(playerPos.z - it.z) <= 2 }

                    val delay = if (isOnWaypoint) 5000 else 3000
                    if (System.currentTimeMillis() - bpsZeroStartTime >= delay) {
                        mc.thePlayer?.playSound("random.anvil_land", 1.0f, 1.0f)
                        TitleUtil.showTitle("BPS = 0")
                        Utils.log("BPS = 0 for ${delay / 1000} seconds")
                    }
                }
            } else {
                bpsZeroStartTime = 0L
            }
        }

        if (isActive) {
            if (mc.thePlayer.posY < 63) {
                currentMode = "none"
                releaseAllKeys()
            }
        }

        if (targetMode != "none") {
            if (lastWaypoint == pos) {
                ticksOnWaypoint++
                if (ticksOnWaypoint >= randomDelayTicks) {
                    if (targetMode == "warp") {
                        mc.thePlayer.sendChatMessage("/warp garden")
                        currentMode = "none"
                        releaseAllKeys()
                        lastWaypoint = null
                        ticksOnWaypoint = 0
                    } else {
                        currentMode = targetMode
                    }
                    randomDelayTicks = (20..100).random()
                }
            } else {
                lastWaypoint = pos
                ticksOnWaypoint = 1
            }
        } else {
            lastWaypoint = null
            ticksOnWaypoint = 0
        }

        holdKeys()
    }

    @SubscribeEvent
    fun onRenderGameOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!Utils.isInSkyblock()) return
        if (!Faketils.config.funnyToggle) return

        val mc = Minecraft.getMinecraft()
        val fontRenderer = mc.fontRendererObj
        val resolution = ScaledResolution(mc)

        val statusText = when {
            !isActive -> "Macro: §cInactive"
            isPaused -> "Macro: §ePaused"
            else -> "Macro: §aActive"
        }

        val scale = 4.0f
        val statusWidth = fontRenderer.getStringWidth(statusText) * scale
        val x = (resolution.scaledWidth - statusWidth) / 2.0f
        val y = 20.0f

        GL11.glPushMatrix()
        GL11.glScalef(scale, scale, 1.0f)
        fontRenderer.drawStringWithShadow(statusText, x / scale, y / scale, 0xFFFFFF)
        GL11.glPopMatrix()
    }

    private fun holdKeys() {
        val settings = Minecraft.getMinecraft().gameSettings
        val forward = settings.keyBindForward
        val left = settings.keyBindLeft
        val right = settings.keyBindRight
        val attack = settings.keyBindAttack

        if (currentMode == "none") {
            return
        }

        KeyBinding.setKeyBindState(forward.keyCode, true)
        KeyBinding.setKeyBindState(attack.keyCode, true)

        if (currentMode == "left") {
            KeyBinding.setKeyBindState(left.keyCode, true)
            KeyBinding.setKeyBindState(right.keyCode, false)
        } else if (currentMode == "right") {
            KeyBinding.setKeyBindState(left.keyCode, false)
            KeyBinding.setKeyBindState(right.keyCode, true)
        }
    }

    private fun releaseAllKeys() {
        val settings = Minecraft.getMinecraft().gameSettings
        KeyBinding.setKeyBindState(settings.keyBindForward.keyCode, false)
        KeyBinding.setKeyBindState(settings.keyBindLeft.keyCode, false)
        KeyBinding.setKeyBindState(settings.keyBindRight.keyCode, false)
        KeyBinding.setKeyBindState(settings.keyBindAttack.keyCode, false)
    }

    private fun lockMouse() {
        if (!isMouseLocked) {
            originalMouseSensitivity = mc.gameSettings.mouseSensitivity
            mc.gameSettings.mouseSensitivity = -1f / 3f
            isMouseLocked = true
            Utils.log("Mouse locked")
        }
    }

    private fun unlockMouse() {
        if (isMouseLocked) {
            mc.gameSettings.mouseSensitivity = originalMouseSensitivity
            isMouseLocked = false
            Utils.log("Mouse unlocked")
        }
    }
}
