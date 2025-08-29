package com.faketils.features

import com.faketils.Faketils
import net.minecraft.block.BlockPlanks
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import org.lwjgl.input.Keyboard

class Farming {

    lateinit var toggleKey: KeyBinding

    var isActive = false
    var currentMode = "none"
    var delayTimer = 0

    fun init() {
        toggleKey = KeyBinding("Funny Toggle", Keyboard.KEY_P, "Faketils")
        ClientRegistry.registerKeyBinding(toggleKey)
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return

        val mc = Minecraft.getMinecraft()
        if (mc.currentScreen != null) return

        if (toggleKey.isPressed) {
            isActive = !isActive
            if (!isActive) {
                releaseAllKeys()
                currentMode = "none"
                delayTimer = 0
            }
        }

        if (!isActive) return

        if (delayTimer > 0) {
            delayTimer--
            if (delayTimer == 0) {
                applyMode()
            } else {
                holdKeys()
                return
            }
        }

        val player = mc.thePlayer ?: return
        val pos = BlockPos(player.posX, player.posY - 1, player.posZ)
        val world = player.worldObj
        val state = world.getBlockState(pos)
        val block = state.block

        var detectedMode = ""
        if (block == Blocks.planks && state.getValue(BlockPlanks.VARIANT) == BlockPlanks.EnumType.OAK) {
            detectedMode = "left"
        } else if (block == Blocks.cobblestone) {
            detectedMode = "right"
        }

        if (detectedMode != "" && detectedMode != currentMode) {
            delayTimer = 3
            currentMode = detectedMode
            holdKeys()
            return
        }

        holdKeys()
    }

    @SubscribeEvent
    fun onRenderGameOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!Faketils.config.macroStatus) return

        val mc = Minecraft.getMinecraft()
        val fontRenderer = mc.fontRendererObj
        val resolution = ScaledResolution(mc)
        val text = if (isActive) "Macro: Active" else "Macro: Inactive"
        val textWidth = fontRenderer.getStringWidth(text)
        val x = (resolution.scaledWidth / 2) - (textWidth / 2)
        val y = 10

        fontRenderer.drawStringWithShadow(text, x.toFloat(), y.toFloat(), 0xFFFFFF)
    }

    private fun applyMode() {
        holdKeys()
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