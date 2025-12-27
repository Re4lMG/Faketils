package com.faketils.utils

import com.faketils.config.FaketilsConfig
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.scoreboard.Score
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.*
import org.lwjgl.opengl.GL11
import java.awt.Color


object Utils {

    private val mc: Minecraft = Minecraft.getMinecraft()

    fun stripColorCodes(string: String): String {
        return string.replace("§.".toRegex(), "")
    }

    fun cleanSB(scoreboard: String): String {
        val nvString = StringUtils.stripControlCodes(scoreboard).toCharArray()
        val cleaned = StringBuilder()

        for (c in nvString) {
            if (c.code in 21..126) {
                cleaned.append(c)
            }
        }

        return cleaned.toString()
    }


    fun getSidebarLines(): List<String> {
        val lines = mutableListOf<String>()
        val mc = Minecraft.getMinecraft()
        val world = mc.theWorld ?: return lines
        val scoreboard = world.scoreboard ?: return lines

        val objective = scoreboard.getObjectiveInDisplaySlot(1) ?: return lines

        val scores: Collection<Score> = try {
            scoreboard.getSortedScores(objective)
        } catch (ex: ConcurrentModificationException) {
            ex.printStackTrace()
            return emptyList()
        }

        val list = scores.filter { it.playerName != null && !it.playerName.startsWith("#") }

        val finalScores = if (list.size > 15) {
            Lists.newArrayList(Iterables.skip(list, scores.size - 15))
        } else {
            list
        }

        for (score in finalScores) {
            val team = scoreboard.getPlayersTeam(score.playerName)
            lines.add(ScorePlayerTeam.formatPlayerName(team, score.playerName))
        }

        return lines
    }

    fun log(message: String) {
        if (!FaketilsConfig.debug) return
        val player = mc.thePlayer ?: return
        player.addChatComponentMessage(ChatComponentText("§7[§bFaketils§7] §f$message"))
    }

    fun isInSkyblock(): Boolean {
        if (mc.theWorld == null || mc.thePlayer == null) return false
        if (mc.isSingleplayer) return false
        val objective = mc.thePlayer.worldScoreboard.getObjectiveInDisplaySlot(1) ?: return false
        return stripColorCodes(objective.displayName).contains("skyblock", true)
    }

    fun isInDungeons(): Boolean {
        if (isInSkyblock()) {
            val sidebarLines = getSidebarLines()
            return sidebarLines.any { cleanSB(it).contains("The Catacombs") }
        }
        return false
    }

    fun isInGarden(): Boolean {
        if (isInSkyblock()) {
            val sidebarLines = getSidebarLines()
            return sidebarLines.any { cleanSB(it).contains("The Garden") || cleanSB(it).contains("Plot -")}
        }
        return false
    }

    fun drawFilledBoundingBoxEntity(aabb: AxisAlignedBB, alpha: Float, color: Color, partialTicks: Float) {
        val render: Entity = Minecraft.getMinecraft().renderViewEntity

        val coordX: Double = render.lastTickPosX + (render.posX - render.lastTickPosX) * partialTicks
        val coordY: Double = render.lastTickPosY + (render.posY - render.lastTickPosY) * partialTicks
        val coordZ: Double = render.lastTickPosZ + (render.posZ - render.lastTickPosZ) * partialTicks

        GlStateManager.pushMatrix()
        GlStateManager.translate(-coordX, -coordY, -coordZ)

        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GL11.glDisable(GL11.GL_LIGHTING)
        GlStateManager.disableLighting()
        GlStateManager.disableAlpha()
        GlStateManager.disableDepth()
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)

        GlStateManager.color(
            color.getRed() / 255f,
            color.getGreen() / 255f,
            color.getBlue() / 255f,
            (color.getAlpha() / 255f) * alpha
        )

        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        tessellator.draw()

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        tessellator.draw()

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        tessellator.draw()

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        tessellator.draw()

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldrenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldrenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        tessellator.draw()

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldrenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldrenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        tessellator.draw()

        GlStateManager.enableTexture2D()
        GlStateManager.enableAlpha()
        GlStateManager.disableBlend()
        GlStateManager.enableCull()
        GlStateManager.enableDepth()
        GL11.glEnable(GL11.GL_LIGHTING)
        GlStateManager.enableLighting()
        GlStateManager.popMatrix()
    }

    fun drawFilledBlockBox(pos: BlockPos, color: Color, alpha: Float, partialTicks: Float) {
        val mc = Minecraft.getMinecraft()
        val render = mc.renderViewEntity ?: return

        val coordX = render.lastTickPosX + (render.posX - render.lastTickPosX) * partialTicks
        val coordY = render.lastTickPosY + (render.posY - render.lastTickPosY) * partialTicks
        val coordZ = render.lastTickPosZ + (render.posZ - render.lastTickPosZ) * partialTicks

        val aabb = AxisAlignedBB(
            pos.x.toDouble(),
            pos.y.toDouble(),
            pos.z.toDouble(),
            pos.x + 1.0,
            pos.y + 1.0,
            pos.z + 1.0
        )

        GlStateManager.pushMatrix()
        GlStateManager.translate(-coordX, -coordY, -coordZ)

        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GL11.glDisable(GL11.GL_LIGHTING)
        GlStateManager.disableLighting()
        GlStateManager.disableAlpha()
        GlStateManager.disableDepth()
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)

        GlStateManager.color(
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            (color.alpha / 255f) * alpha
        )

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        fun drawFace(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double,
                     x3: Double, y3: Double, z3: Double, x4: Double, y4: Double, z4: Double) {
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
            worldRenderer.pos(x1, y1, z1).endVertex()
            worldRenderer.pos(x2, y2, z2).endVertex()
            worldRenderer.pos(x3, y3, z3).endVertex()
            worldRenderer.pos(x4, y4, z4).endVertex()
            tessellator.draw()
        }

        // Bottom
        drawFace(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.minY, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ, aabb.minX, aabb.minY, aabb.maxZ)
        // Top
        drawFace(aabb.minX, aabb.maxY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, aabb.minX, aabb.maxY, aabb.maxZ)
        // North
        drawFace(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.minZ, aabb.minX, aabb.maxY, aabb.minZ)
        // South
        drawFace(aabb.minX, aabb.minY, aabb.maxZ, aabb.maxX, aabb.minY, aabb.maxZ, aabb.maxX, aabb.maxY, aabb.maxZ, aabb.minX, aabb.maxY, aabb.maxZ)
        // West
        drawFace(aabb.minX, aabb.minY, aabb.minZ, aabb.minX, aabb.minY, aabb.maxZ, aabb.minX, aabb.maxY, aabb.maxZ, aabb.minX, aabb.maxY, aabb.minZ)
        // East
        drawFace(aabb.maxX, aabb.minY, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ, aabb.maxX, aabb.maxY, aabb.maxZ, aabb.maxX, aabb.maxY, aabb.minZ)

        GlStateManager.enableTexture2D()
        GlStateManager.enableAlpha()
        GlStateManager.disableBlend()
        GlStateManager.enableCull()
        GlStateManager.enableDepth()
        GL11.glEnable(GL11.GL_LIGHTING)
        GlStateManager.enableLighting()
        GlStateManager.popMatrix()
    }


    fun draw3DLine(pos: Vec3, pos2: Vec3, color: Color, partialTicks: Float) {
        val viewer = Minecraft.getMinecraft().getRenderViewEntity()
        val interp = getInterpolatedPosition(viewer, partialTicks)

        GlStateManager.pushMatrix()
        GlStateManager.translate(-interp.xCoord, -interp.yCoord, -interp.zCoord)
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GL11.glDisable(GL11.GL_LIGHTING)
        GlStateManager.disableLighting()
        GlStateManager.disableAlpha()
        GlStateManager.disableDepth()
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)

        renderLine(pos, pos2, color)

        GlStateManager.enableTexture2D()
        GlStateManager.enableAlpha()
        GlStateManager.disableBlend()
        GlStateManager.enableCull()
        GlStateManager.enableDepth()
        GL11.glEnable(GL11.GL_LIGHTING)
        GlStateManager.enableLighting()
        GlStateManager.translate(interp.xCoord, interp.yCoord, interp.zCoord)
        GlStateManager.popMatrix()
    }

    private fun getPlayerLookVec(): Vec3 {
        val mc = Minecraft.getMinecraft()
        val yaw = -mc.thePlayer.rotationYaw
        val pitch = -mc.thePlayer.rotationPitch
        return Vec3(0.0, 0.0, 1.0)
            .rotatePitch(Math.toRadians(pitch.toDouble()).toFloat())
            .rotateYaw(Math.toRadians(yaw.toDouble()).toFloat())
    }

    private fun getInterpolatedPosition(entity: Entity, partialTicks: Float): Vec3 {
        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks
        return Vec3(x, y, z)
    }

    private fun renderLine(start: Vec3, end: Vec3, color: Color) {
        val wr = Tessellator.getInstance().worldRenderer
        GlStateManager.color(
            color.red / 255f, color.green / 255f,
            color.blue / 255f, color.alpha / 255f
        )
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        wr.pos(start.xCoord, start.yCoord, start.zCoord).endVertex()
        wr.pos(end.xCoord, end.yCoord, end.zCoord).endVertex()
        Tessellator.getInstance().draw()
    }

    fun renderWaypointText(str: String, loc: BlockPos, partialTicks: Float) {
        renderWaypointText(str, loc, partialTicks, true)
    }

    fun renderWaypointText(
        str: String,
        loc: BlockPos,
        partialTicks: Float,
        showDistance: Boolean
    ) {
        GlStateManager.alphaFunc(516, 0.1f)
        GlStateManager.pushMatrix()
        GlStateManager.disableLighting()
        GL11.glDisable(GL11.GL_LIGHTING)

        val mc = Minecraft.getMinecraft()
        val viewer = mc.renderViewEntity ?: return

        val viewerX =
            viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks
        val viewerY =
            viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks
        val viewerZ =
            viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks

        var x = loc.x + 0.5 - viewerX
        var y = loc.y + 0.5 - viewerY - viewer.eyeHeight.toDouble()
        var z = loc.z + 0.5 - viewerZ

        val distSq = x * x + y * y + z * z
        val dist = Math.sqrt(distSq)

        if (distSq > 144) {
            val scale = 12 / dist
            x *= scale
            y *= scale
            z *= scale
        }

        GlStateManager.translate(x, y, z)
        GlStateManager.translate(0.0, viewer.eyeHeight.toDouble(), 0.0)

        val scale = 2.0f
        GlStateManager.scale(scale, scale, scale)

        drawNametag(str)

        val renderManager = mc.renderManager
        GlStateManager.rotate(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(renderManager.playerViewX, 1.0f, 0.0f, 0.0f)
        GlStateManager.translate(0.0f, -0.25f, 0.0f)
        GlStateManager.rotate(-renderManager.playerViewX, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(renderManager.playerViewY, 0.0f, 1.0f, 0.0f)

        if (showDistance) {
            drawNametag(EnumChatFormatting.YELLOW.toString() + Math.round(dist) + "m")
        }

        GL11.glEnable(GL11.GL_LIGHTING)
        GlStateManager.enableLighting()
        GlStateManager.popMatrix()
    }

    fun drawNametag(str: String) {
        val mc = Minecraft.getMinecraft()
        val fontRenderer = mc.fontRendererObj
        val renderManager = mc.renderManager

        val f = 1.6f
        val f1 = 0.016666668f * f

        GlStateManager.pushMatrix()
        GL11.glNormal3f(0.0f, 1.0f, 0.0f)

        GlStateManager.rotate(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(renderManager.playerViewX, 1.0f, 0.0f, 0.0f)
        GlStateManager.scale(-f1, -f1, f1)

        GlStateManager.disableLighting()
        GL11.glDisable(GL11.GL_LIGHTING)
        GlStateManager.depthMask(false)
        GlStateManager.disableDepth()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        val yOffset = 0
        val width = fontRenderer.getStringWidth(str) / 2

        GlStateManager.disableTexture2D()
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR)
        worldRenderer.pos((-width - 1).toDouble(), (-1 + yOffset).toDouble(), 0.0)
            .color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        worldRenderer.pos((-width - 1).toDouble(), (8 + yOffset).toDouble(), 0.0)
            .color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        worldRenderer.pos((width + 1).toDouble(), (8 + yOffset).toDouble(), 0.0)
            .color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        worldRenderer.pos((width + 1).toDouble(), (-1 + yOffset).toDouble(), 0.0)
            .color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        tessellator.draw()

        GlStateManager.enableTexture2D()

        fontRenderer.drawString(
            str,
            -fontRenderer.getStringWidth(str) / 2,
            yOffset,
            553648127
        )

        GlStateManager.depthMask(true)

        fontRenderer.drawString(
            str,
            -fontRenderer.getStringWidth(str) / 2,
            yOffset,
            -1
        )

        GlStateManager.enableDepth()
        GlStateManager.enableBlend()
        GL11.glEnable(GL11.GL_LIGHTING)
        GlStateManager.enableLighting()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.popMatrix()
    }

}