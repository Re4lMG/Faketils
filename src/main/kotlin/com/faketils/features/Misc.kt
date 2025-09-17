package com.faketils.features

import com.faketils.Faketils
import com.faketils.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.model.IBakedModel
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

class Misc {

    companion object {
        @JvmStatic
        fun renderItemPre(stack: ItemStack, model: IBakedModel, ci: CallbackInfo) {
            if (!Utils.isInSkyblock()) return

            val mc = Minecraft.getMinecraft()
            val screen = mc.currentScreen

            val isContainerSlot = if (screen is net.minecraft.client.gui.inventory.GuiContainer) {
                screen.inventorySlots.inventorySlots.any { slot ->
                    slot.hasStack && slot.stack == stack
                }
            } else {
                false
            }

            val isHotbarSlot = mc.thePlayer.inventory.mainInventory.any { it == stack }

            if (!isContainerSlot && !isHotbarSlot) return

            if (stack.item === Items.skull) {
                val scale = Faketils.config.largerHeadScale.toDouble()
                GlStateManager.scale(scale, scale, scale)
            }
        }

        @JvmStatic
        fun scaleItemDrop(
            entity: EntityItem,
            x: Double,
            y: Double,
            z: Double,
            entityYaw: Float,
            partialTicks: Float,
            ci: CallbackInfo
        ) {
            if (!Utils.isInSkyblock()) return
            val scale = Faketils.config.itemDropScale.toDouble()
            GlStateManager.scale(scale, scale, scale)
        }
    }

    @SubscribeEvent
    fun onRenderLiving(event: RenderLivingEvent.Pre<EntityLivingBase>) {
        val entity = event.entity

        if (!Utils.isInSkyblock()) return
        if (!Faketils.config.hideDyingMobs) return

        if (entity.deathTime > 0) {
            event.setCanceled(true);
        }
    }
}