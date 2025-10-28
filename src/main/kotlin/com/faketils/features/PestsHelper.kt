package com.faketils.features

import com.faketils.config.FaketilsConfig
import com.faketils.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.StringUtils
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color


object PestsHelper {

    @SubscribeEvent
    fun onRenderLast(event: RenderWorldLastEvent) {
        if (!Utils.isInSkyblock()) return
        if (!FaketilsConfig.pestHelper) return
        val mc = Minecraft.getMinecraft()
        val world = mc.theWorld ?: return

        val partialTicks = event.partialTicks

        for (entity in world.loadedEntityList) {
            if (entity is EntityArmorStand) {
                val customName = entity.getCustomNameTag() ?: continue
                val unformattedName = StringUtils.stripControlCodes(customName)

                if (unformattedName.startsWith("ൠ")) {
                    val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks
                    val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks
                    val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks
                    val aabb = AxisAlignedBB(x - 0.5, y, z - 0.5, x + 0.5, y + 1.0, z + 0.5)

                    Utils.drawFilledBoundingBoxEntity(aabb, 0.8f, Color.CYAN, partialTicks)
                    Utils.draw3DLine(
                        Vec3(entity.posX, entity.posY, entity.posZ),
                        mc.thePlayer.getPositionEyes(partialTicks),
                        Color.CYAN, partialTicks
                    )
                }
            }
        }
    }
}