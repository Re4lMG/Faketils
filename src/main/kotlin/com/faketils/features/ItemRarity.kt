package com.faketils.features

import com.faketils.Faketils
import com.faketils.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.util.regex.Pattern

object ItemRarity {
    private val RARITY_TEXTURE = ResourceLocation("faketils:rarity.png")
    private val RARITY_PATTERN =
        Pattern.compile("(§[0-9a-f]§l§ka§r )?([§0-9a-fk-or]+)(?<rarity>COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|VERY SPECIAL)")
    private val PET_PATTERN = Pattern.compile("§7\\[Lvl \\d+\\] (?<color>§[0-9a-fk-or]).+")

    enum class ItemRarity(val rarityName: String, val color: EnumChatFormatting) {
        COMMON("COMMON", EnumChatFormatting.WHITE),
        UNCOMMON("UNCOMMON", EnumChatFormatting.GREEN),
        RARE("RARE", EnumChatFormatting.BLUE),
        EPIC("EPIC", EnumChatFormatting.DARK_PURPLE),
        LEGENDARY("LEGENDARY", EnumChatFormatting.GOLD),
        MYTHIC("MYTHIC", EnumChatFormatting.LIGHT_PURPLE),
        DIVINE("DIVINE", EnumChatFormatting.AQUA),
        SUPREME("SUPREME", EnumChatFormatting.DARK_RED),
        SPECIAL("SPECIAL", EnumChatFormatting.RED),
        VERY_SPECIAL("VERY SPECIAL", EnumChatFormatting.RED);

        companion object {
            fun byBaseColor(colorCode: String): ItemRarity? {
                return values().firstOrNull { it.color.toString() == colorCode }
            }
        }
    }

    fun renderRarityOverlay(stack: ItemStack?, x: Int, y: Int) {
        if (!Utils.isInSkyblock()) return
        if (stack == null) return
        val rarity = getItemRarity(stack) ?: return
        renderRarityBackground(x, y, rarity)
    }

    private fun renderRarityBackground(x: Int, y: Int, rarity: ItemRarity) {
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.enableBlend()
        GlStateManager.enableAlpha()

        Minecraft.getMinecraft().textureManager.bindTexture(RARITY_TEXTURE)
        setColorFromRarity(rarity)

        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_BLEND)
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, 16, 16, 16f, 16f)

        resetRenderStates()
    }

    private fun setColorFromRarity(rarity: ItemRarity) {
        val rgb = getColorValue(rarity.color)
        val r = (rgb shr 16 and 0xFF) / 255f
        val g = (rgb shr 8 and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        GlStateManager.color(r, g, b, Faketils.config.itemRarity)
    }

    private fun getColorValue(format: EnumChatFormatting): Int {
        return when (format) {
            EnumChatFormatting.GREEN -> 0x55FF55
            EnumChatFormatting.BLUE -> 0x5555FF
            EnumChatFormatting.DARK_PURPLE -> 0xAA00AA
            EnumChatFormatting.GOLD -> 0xFFAA00
            EnumChatFormatting.LIGHT_PURPLE -> 0xFF55FF
            EnumChatFormatting.AQUA -> 0x55FFFF
            EnumChatFormatting.DARK_RED -> 0xAA0000
            EnumChatFormatting.RED -> 0xFF5555
            else -> 0xFFFFFF
        }
    }

    private fun resetRenderStates() {
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE)
        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
        GlStateManager.disableAlpha()
    }

    fun getItemRarity(item: ItemStack?): ItemRarity? {
        if (item == null || !item.hasTagCompound()) return null

        val display = item.getSubCompound("display", false) ?: return null
        val name = getDisplayName(item)
        val lore: NBTTagList = display.getTagList("Lore", 8)

        val petMatcher = PET_PATTERN.matcher(name)
        if (petMatcher.find()) {
            return ItemRarity.byBaseColor(petMatcher.group("color"))
        }

        for (i in lore.tagCount() - 1 downTo 0) {
            val line = lore.getStringTagAt(i)
            val rarityMatcher = RARITY_PATTERN.matcher(line)
            if (rarityMatcher.find()) {
                val rarityName = rarityMatcher.group("rarity")
                return ItemRarity.values().firstOrNull { rarityName.startsWith(it.rarityName) }
            }
        }
        return null
    }

    private fun getDisplayName(item: ItemStack): String {
        if (item.hasTagCompound() && item.tagCompound!!.hasKey("display", 10)) {
            val display = item.tagCompound!!.getCompoundTag("display")
            if (display.hasKey("Name", 8)) {
                return display.getString("Name")
            }
        }
        return item.displayName
    }
}
