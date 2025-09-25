package com.faketils.features

import com.faketils.Faketils
import com.faketils.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemFishingRod
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object FishingTickHandler {
    private var clickTimer = 0
    private var hasClickedOnce = false
    private var scheduledClick = false
    private var delayTimer = 0
    private var fireVeilState = 0
    private var originalSlot = 0
    private var veilSlot = 0
    private var delayCounter = 0

    private val handledArmorStands = mutableSetOf<Int>()

    private val mc: Minecraft = Minecraft.getMinecraft()

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        if (!Utils.isInSkyblock() || !Faketils.config.fishingHelper) return

        if (mc.currentScreen != null) return
        val player = mc.thePlayer ?: return
        val heldItem = player.heldItem ?: return
        if (heldItem.item !is ItemFishingRod) return

        val armorStands = mc.theWorld?.loadedEntityList?.filterIsInstance<EntityArmorStand>() ?: return

        handledArmorStands.retainAll(armorStands.map { it.entityId }.toSet())

        for (armorStand in armorStands) {
            if (armorStand.isDead || !armorStand.hasCustomName()) continue
            if (armorStand.customNameTag == "§c§l!!!") {
                if (!handledArmorStands.contains(armorStand.entityId)) {
                    if (delayTimer == 0) {
                        scheduledClick = true
                        handledArmorStands.add(armorStand.entityId)
                        Utils.log("EntityID: "+ armorStand.entityId)
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

        if (scheduledClick) {
            clickTimer = (5..10).random()
            hasClickedOnce = true

            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)

            player.playSound("random.orb", 1.0f, 1.0f)
            scheduledClick = false

            if (Faketils.config.fishingHelperFireVeil) {
                val detector = FireVeilDetector(mc)
                val fireVeil = detector.findFireVeil()
                if (fireVeil != null) {
                    originalSlot = player.inventory.currentItem
                    veilSlot = fireVeil.slot
                    fireVeilState = 1
                    delayCounter = (2..5).random()
                }
            }
        }

        if (clickTimer > 0) {
            clickTimer--
        }

        if (fireVeilState > 0) {
            when (fireVeilState) {
                1 -> {
                    if (delayCounter > 0) {
                        delayCounter--
                    } else {
                        fireVeilState = 2
                    }
                }
                2 -> {
                    player.inventory.currentItem = veilSlot
                    mc.playerController.updateController()
                    delayCounter = (2..5).random()
                    fireVeilState = 3
                }
                3 -> {
                    if (delayCounter > 0) {
                        delayCounter--
                    } else {
                        fireVeilState = 4
                    }
                }
                4 -> {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
                    KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
                    delayCounter = (2..5).random()
                    fireVeilState = 5
                }
                5 -> {
                    if (delayCounter > 0) {
                        delayCounter--
                    } else {
                        fireVeilState = 6
                    }
                }
                6 -> {
                    player.inventory.currentItem = originalSlot
                    mc.playerController.updateController()
                    fireVeilState = 0
                }
            }
            return
        }

        if (clickTimer == 0 && hasClickedOnce) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
            hasClickedOnce = false
        } else if (clickTimer == 0) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
        }
    }

    class FireVeilDetector(private val mc: Minecraft = Minecraft.getMinecraft()) {

        data class FireVeilResult(val slot: Int, val keyBind: KeyBinding)

        fun findFireVeil(): FireVeilResult? {
            val player = mc.thePlayer ?: return null
            val hotbar = player.inventory.mainInventory

            for (slot in 0..8) {
                val stack: ItemStack? = hotbar[slot]
                if (stack != null && stack.hasDisplayName() && stack.displayName.contains("Fire Veil Wand")) {
                    val keyBind = mc.gameSettings.keyBindsHotbar[slot]
                    Utils.log("Veil at $slot")
                    return FireVeilResult(slot, keyBind)
                }
            }
            return null
        }
    }
}