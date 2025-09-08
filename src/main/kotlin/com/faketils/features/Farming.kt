package com.faketils.features

import com.faketils.Faketils
import com.faketils.commands.FarmingCommand
import com.faketils.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

class Farming {
    lateinit var toggleKey: KeyBinding
    var isActive = false
    var currentMode = "none"

    private var lastWaypoint: BlockPos? = null
    private var ticksOnWaypoint = 0

    val mc = Minecraft.getMinecraft()

    fun init() {
        toggleKey = KeyBinding("Funny Toggle", Keyboard.KEY_P, "Faketils")
        ClientRegistry.registerKeyBinding(toggleKey)
    }

    @SubscribeEvent
    fun onWorldUnLoad(event: WorldEvent.Unload) {
        if (mc.thePlayer != null && mc.theWorld != null) {
            isActive = false
            releaseAllKeys()
            currentMode = "none"
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
            if (!isActive) {
                releaseAllKeys()
                currentMode = "none"
                lastWaypoint = null
                ticksOnWaypoint = 0
            }
        }

        if (!isActive) return

        val player = mc.thePlayer ?: return
        val pos = BlockPos(player.posX.toInt(), player.posY.toInt(), player.posZ.toInt())

        val rightList = FarmingCommand.waypoints["right"] ?: emptyList()
        val leftList = FarmingCommand.waypoints["left"] ?: emptyList()

        val targetMode = when {
            rightList.any { it == pos } -> "right"
            leftList.any { it == pos } -> "left"
            else -> "none"
        }

        if (targetMode != "none") {
            if (lastWaypoint == pos) {
                ticksOnWaypoint++
                if (ticksOnWaypoint >= 20) {
                    currentMode = targetMode
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
        val text = if (isActive) "Funny: §aActive" else "Funny: §cInactive"
        val textWidth = fontRenderer.getStringWidth(text)
        val scale = 4.0f
        val scaledWidth = textWidth * scale
        val x = (resolution.scaledWidth - scaledWidth) / 2.0f
        val y = 20.0f

        GL11.glPushMatrix()
        GL11.glScalef(scale, scale, 1.0f)
        fontRenderer.drawStringWithShadow(text, x / scale, y / scale, 0xFFFFFF)
        GL11.glPopMatrix()
    }

    private fun holdKeys() {
        val settings = Minecraft.getMinecraft().gameSettings
        val forward = settings.keyBindForward
        val left = settings.keyBindLeft
        val right = settings.keyBindRight
        val attack = settings.keyBindAttack

        if (currentMode == "none") {
            KeyBinding.setKeyBindState(forward.keyCode, false)
            KeyBinding.setKeyBindState(left.keyCode, false)
            KeyBinding.setKeyBindState(right.keyCode, false)
            KeyBinding.setKeyBindState(attack.keyCode, false)
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
}
