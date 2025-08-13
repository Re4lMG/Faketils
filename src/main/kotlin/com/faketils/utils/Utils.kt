package com.faketils.utils

import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import net.minecraft.client.Minecraft
import net.minecraft.scoreboard.Score
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.StringUtils

object Utils {

    fun cleanSB(scoreboard: String): String {
        return StringUtils.stripControlCodes(scoreboard).filter { it.code in 21..126 }
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

    var inSkyblock = false

    fun checkForSkyblock() {
        val mc = Minecraft.getMinecraft()
        if (mc.theWorld != null && !mc.isSingleplayer) {
            val scoreboardObj = mc.theWorld.scoreboard.getObjectiveInDisplaySlot(1)
            if (scoreboardObj != null) {
                val scObjName = cleanSB(scoreboardObj.displayName)
                inSkyblock = scObjName.contains("SKYBLOCK")
                return
            }
        }
        inSkyblock = false
    }

    var inDungeons = false

    fun checkForDungeons() {
        if (inSkyblock) {
            val sidebarLines = getSidebarLines()
            if (sidebarLines.any { cleanSB(it).contains("The Catacombs") }) {
                inDungeons = true
                return
            }
        }
        inDungeons = false
    }
}