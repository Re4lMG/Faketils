package com.faketils.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object TitleUtil {

    private data class Title(val text: String, val color: Color?, val endTime: Long)

    private val titles = CopyOnWriteArrayList<Title>()

    fun showTitle(text: String, color: Color? = Color.RED, durationMillis: Int = 1000) {
        val endTime = System.currentTimeMillis() + durationMillis
        titles.clear()
        titles.add(Title(text, color, endTime))
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return
        val currentTime = System.currentTimeMillis()

        val mc = Minecraft.getMinecraft()
        val fr: FontRenderer = mc.fontRendererObj

        titles.removeIf { it.endTime < currentTime }

        for (title in titles) {
            val displayText = if (title.color == null) title.text else net.minecraft.util.StringUtils.stripControlCodes(title.text)
            val renderColor = title.color?.rgb?.and(0xFFFFFF) ?: 0xFFFFFF
            val screenWidth = event.resolution.scaledWidth
            val screenHeight = event.resolution.scaledHeight

            GlStateManager.pushMatrix()
            GlStateManager.scale(4.0f, 4.0f, 4.0f)
            val textWidth = fr.getStringWidth(displayText)
            val x = (screenWidth / 8) - (textWidth / 2)
            val y = (screenHeight / 8) - 10
            fr.drawStringWithShadow(displayText, x.toFloat(), y.toFloat(), renderColor)
            GlStateManager.popMatrix()
        }
    }
}