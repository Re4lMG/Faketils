package com.faketils.features

import com.faketils.config.FaketilsConfig
import com.faketils.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FireFreezeTimer {

    private val mc: Minecraft = Minecraft.getMinecraft()
    private var shouldFireFreeze = false
    private var fireFreezeTimer: Long = 0L

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (!Utils.isInDungeons()) return
        val msg = event.message.unformattedText
        if (msg.startsWith("[BOSS] ") &&
            msg == "[BOSS] The Professor: Oh? You found my Guardians' one weakness?"
        ) {
            shouldFireFreeze = true
            fireFreezeTimer = System.currentTimeMillis() + 5000
            mc.thePlayer.playSound("mob.wither.spawn", 1f, 1f)
        }
    }

    fun isFireFreezeInHotbar(): Boolean {
        val inv = mc.thePlayer?.inventory ?: return false
        for (slot in 0..8) {
            val stack = inv.getStackInSlot(slot)
            if (stack != null && stack.hasDisplayName() && stack.displayName.contains("Fire Freeze Staff")) {
                return true
            }
        }
        return false
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Text) {
        if (!Utils.isInDungeons()) return
        if (!FaketilsConfig.fireFreezeTimer || !isFireFreezeInHotbar()) return
        if (!shouldFireFreeze) return

        val remaining = fireFreezeTimer - System.currentTimeMillis()
        if (remaining <= 0) {
            // timer expired → hide
            shouldFireFreeze = false
            return
        }

        val countdown = remaining / 1000.0
        val text = String.format("Fire Freeze in %.2f", countdown)
        val sr = ScaledResolution(mc)
        mc.fontRendererObj.drawStringWithShadow(
            text,
            (sr.scaledWidth / 2f) - (mc.fontRendererObj.getStringWidth(text) / 2f),
            sr.scaledHeight / 2f + 20f,
            0xFFFFFF
        )
    }
}