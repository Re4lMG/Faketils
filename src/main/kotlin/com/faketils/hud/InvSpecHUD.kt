package com.faketils.hud

import cc.polyfrost.oneconfig.config.annotations.Color
import cc.polyfrost.oneconfig.config.annotations.Switch
import cc.polyfrost.oneconfig.config.core.OneColor
import cc.polyfrost.oneconfig.hud.BasicHud
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.item.ItemStack

class InvSpecHUD : BasicHud(
    true,
    1f,
    1f,
    1f,
    true,
    true,
    4f,
    5f,
    5f,
    OneColor(0, 0, 0, 150),
    false,
    2f,
    OneColor(0, 0, 0, 127)
) {
    @Color(name = "Slot Background Color")
    private val slotBgColor = OneColor(0, 0, 0, 128)

    @Switch(name = "Show Slot Borders", description = "Show the highlight and shadow on slots")
    private val showSlotBorders = true

    @Transient
    private val mc: Minecraft = Minecraft.getMinecraft()
    @Transient
    private val slotSize = 18

    override fun draw(matrices: UMatrixStack?, x: Float, y: Float, scale: Float, example: Boolean) {
        if (!enabled && !example) return

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)
        GlStateManager.scale(scale, scale, 1f)
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.enableBlend()

        val invWidth = 9 * slotSize
        val invHeight = 3 * slotSize

        val centeredX = (getWidth(scale, example) / scale - invWidth) / 2f
        val centeredY = (getHeight(scale, example) / scale - invHeight) / 2f

        GlStateManager.translate(centeredX, centeredY, 0f)

        if (example) {
            drawExampleInventory()
        } else {
            drawRealInventory()
        }

        GlStateManager.popMatrix()
    }

    private fun drawExampleInventory() {
        for (row in 0..2) {
            for (col in 0..8) {
                val slotX = col * slotSize
                val slotY = row * slotSize
                drawSlotBackground(slotX, slotY)
            }
        }
    }

    private fun drawRealInventory() {
        val player = mc.thePlayer ?: return
        var index = 9
        for (row in 0..2) {
            for (col in 0..8) {
                val slotX = col * slotSize
                val slotY = row * slotSize
                drawSlotBackground(slotX, slotY)
                val stack = player.inventory.mainInventory[index]
                drawItemStack(stack, slotX, slotY)
                index++
            }
        }
    }

    private fun drawSlotBackground(x: Int, y: Int) {
        Gui.drawRect(x, y, x + 16, y + 16, slotBgColor.getRGB())
        if (showSlotBorders) {
            Gui.drawRect(x, y, x + 16, y + 1, 0x40FFFFFF) // top
            Gui.drawRect(x, y + 15, x + 16, y + 16, 0x40000000) // bottom
        }
    }

    private fun drawItemStack(stack: ItemStack?, x: Int, y: Int) {
        if (stack == null || stack.item == null) return
        GlStateManager.pushMatrix()
        RenderHelper.enableGUIStandardItemLighting()
        mc.renderItem.renderItemAndEffectIntoGUI(stack, x, y)
        mc.renderItem.renderItemOverlayIntoGUI(mc.fontRendererObj, stack, x, y, null)
        RenderHelper.disableStandardItemLighting()
        GlStateManager.popMatrix()
    }

    override fun getWidth(scale: Float, example: Boolean): Float {
        return (9 * slotSize) * scale + (paddingX * 2)
    }

    override fun getHeight(scale: Float, example: Boolean): Float {
        return (3 * slotSize) * scale + (paddingY * 2)
    }

    override fun shouldShow(): Boolean {
        return super.shouldShow() && mc.thePlayer != null
    }
}
