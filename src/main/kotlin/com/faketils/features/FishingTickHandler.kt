package com.faketils.features

import com.faketils.Faketils
import com.faketils.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.item.ItemFishingRod
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object FishingTickHandler {
    private var clickTimer = 0
    private var hasClickedOnce = false
    private var scheduledClickTime = 0L
    private var lastBiteTime = 0L

    private val mc: Minecraft = Minecraft.getMinecraft()

    fun onParticles(packet: S2APacketParticles) {
        if (!Utils.isInSkyblock()) return
        if (!Faketils.config.fishingHelper) return
        val type = packet.particleType
        if (type != EnumParticleTypes.WATER_WAKE && type != EnumParticleTypes.SMOKE_NORMAL) return

        val player = mc.thePlayer ?: return
        val heldItem = player.heldItem ?: return

        if (heldItem.item is ItemFishingRod) {
            val hook: EntityFishHook? = player.fishEntity
            if (hook != null) {
                val blockPos = BlockPos(hook.posX, hook.posY, hook.posZ)
                val block = hook.worldObj.getBlockState(blockPos).block

                if (block.material.isLiquid) {
                    val dist = hook.getDistanceSq(packet.xCoordinate, hook.posY, packet.zCoordinate)
                    if (dist < 1.0) {
                        val now = System.currentTimeMillis()
                        if (now - lastBiteTime >= Faketils.config.fishingHelperDelayRecast) {
                            lastBiteTime = now
                            scheduledClickTime = System.currentTimeMillis() + Faketils.config.fishingHelperDelay
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (!Faketils.config.fishingHelper) return
        if (mc.currentScreen != null) return

        val player = mc.thePlayer ?: return

        if (scheduledClickTime > 0 && System.currentTimeMillis() >= scheduledClickTime) {
            clickTimer = 10
            hasClickedOnce = true
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
            player.playSound("random.orb", 1.0f, 1.0f)
            scheduledClickTime = 0L
        }

        if (clickTimer > 0) {
            clickTimer--
            if (clickTimer == 0 && hasClickedOnce) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
                hasClickedOnce = false
            } else if (clickTimer == 0) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
            }
        }
    }
}