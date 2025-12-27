package com.faketils.hud

import cc.polyfrost.oneconfig.config.core.OneColor
import cc.polyfrost.oneconfig.hud.BasicHud
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack
import com.faketils.config.FaketilsConfig
import com.faketils.utils.Utils
import net.minecraft.client.Minecraft
import org.lwjgl.opengl.GL11

class MacroStatusHUD : BasicHud(
    true,
    1f,
    1f,
    1f,
    false,
    false,
    0f,
    0f,
    0f,
    OneColor(0, 0, 0, 0),
    false,
    0f,
    OneColor(0, 0, 0, 0)
) {
    @Transient
    private val mc: Minecraft? = Minecraft.getMinecraft()
    @Transient
    var isActive: Boolean = false
    @Transient
    var isPaused: Boolean = false

    override fun draw(matrices: UMatrixStack?, x: Float, y: Float, scale: Float, example: Boolean) {
        val mcLocal = mc ?: return
        val font = mcLocal.fontRendererObj ?: return

        val statusText = when {
            !isActive -> "Macro: §cInactive"
            isPaused -> "Macro: §ePaused"
            else -> "Macro: §aActive"
        }

        GL11.glPushMatrix()
        GL11.glTranslatef(x, y, 0f)
        GL11.glScalef(scale * 4.0f, scale * 4.0f, 1f)
        font.drawStringWithShadow(statusText, 0f, 0f, 0xFFFFFF)
        GL11.glPopMatrix()
    }

    override fun getWidth(scale: Float, example: Boolean): Float {
        val mcLocal = mc
        val font = mcLocal?.fontRendererObj

        if (font == null) {
            return 160f * scale
        }

        val sample = "Macro: §aActive"
        val textWidth = font.getStringWidth(sample)
        return textWidth * 4f * scale
    }

    override fun getHeight(scale: Float, example: Boolean): Float {
        val mcLocal = mc
        val font = mcLocal?.fontRendererObj

        if (font == null) {
            return 9f * 4f * scale
        }

        return font.FONT_HEIGHT * 4f * scale
    }

    override fun shouldShow(): Boolean {
        return Utils.isInGarden() && FaketilsConfig.funnyToggle && super.shouldShow()
    }
}
