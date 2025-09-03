package com.faketils.utils

import com.faketils.Faketils
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import net.minecraft.client.Minecraft
import net.minecraft.scoreboard.Score
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.ChatComponentText
import net.minecraft.util.StringUtils

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
        if (!Faketils.config.debug) return
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
}