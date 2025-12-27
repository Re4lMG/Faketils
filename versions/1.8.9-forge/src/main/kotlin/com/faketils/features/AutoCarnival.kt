package com.faketils.features

import com.faketils.config.FaketilsConfig
import com.faketils.utils.Utils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraft.util.*
import net.minecraft.client.Minecraft
import net.minecraft.event.ClickEvent
import kotlin.random.Random

object AutoCarnival {

    private val mc: Minecraft get() = Minecraft.getMinecraft()

    private var awaitingOptions = false
    private var clicked = false

    private val questionTriggers = listOf(
        "wouldja like to play",
        "are you here to play",
        "would ye like to do some"
    )

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (!Utils.isInSkyblock()) return
        if (!FaketilsConfig.autoCarnival) return

        val raw = event.message.unformattedText.lowercase()

        if (questionTriggers.any { raw.contains(it) }) {
            awaitingOptions = true
            clicked = false
            return
        }

        if (awaitingOptions && !clicked) {
            val firstOption = findFirstClickable(event.message)
            if (firstOption != null) {
                clicked = true
                awaitingOptions = false

                Thread {
                    Thread.sleep(Random.nextLong(150, 750))
                    mc.addScheduledTask {
                        mc.thePlayer.sendChatMessage(firstOption.value)
                    }
                }.start()
            }
        }
    }

    private fun findFirstClickable(component: IChatComponent): ClickEvent? {
        val click = component.chatStyle?.chatClickEvent
        if (click != null && click.action == ClickEvent.Action.RUN_COMMAND) return click

        for (child in component.siblings) {
            val found = findFirstClickable(child)
            if (found != null) return found
        }

        return null
    }
}