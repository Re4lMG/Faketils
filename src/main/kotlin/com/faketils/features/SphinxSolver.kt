package com.faketils.features

import com.faketils.config.FaketilsConfig
import com.faketils.utils.Utils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraft.util.*
import net.minecraft.client.Minecraft
import net.minecraft.event.ClickEvent
import kotlin.random.Random

object SphinxSolver {
    data class SphinxQuestion(
        val question: String,
        val answer: String
    )
    private val mc = Minecraft.getMinecraft()
    val questions: MutableList<SphinxQuestion> = mutableListOf()
    private var pendingAnswer: String? = null

    fun init() {
        addQuestions()
    }

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (!Utils.isInSkyblock()) return
        if (!FaketilsConfig.sphinxSolver) return

        val raw = event.message.unformattedText.trim()

        if (pendingAnswer != null) {
            autoClickIfAnswer(event.message)
            return
        }

        for (sphinxQuestion in questions) {
            if (sphinxQuestion.question.equals(raw.removeFormatting(), ignoreCase = true)) {
                pendingAnswer = sphinxQuestion.answer
                Utils.log("§aCorrect answer: ${sphinxQuestion.answer}")
                return
            }
        }
    }

    private fun autoClickIfAnswer(component: IChatComponent) {
        val answer = pendingAnswer ?: return

        fun scan(comp: IChatComponent) {
            val text = comp.unformattedText.removeFormatting().trim()
            val clickEvent = comp.chatStyle?.chatClickEvent

            if (text.contains(answer, ignoreCase = true) && clickEvent != null) {
                if (clickEvent.action == ClickEvent.Action.RUN_COMMAND) {
                    val delay = Random.nextLong(1000, 2000)
                    Thread {
                        Thread.sleep(delay)
                        mc.addScheduledTask {
                            mc.thePlayer.sendChatMessage(clickEvent.value)
                            pendingAnswer = null
                        }
                    }.start()
                }
            }

            for (child in comp.siblings) scan(child)
        }

        scan(component)
    }

    private fun String.removeFormatting(): String =
        this.replace(Regex("§."), "")

    private fun addQuestions() {
        questions.add(SphinxQuestion("Which of these is NOT a pet?", "Slime"))
        questions.add(SphinxQuestion("What type of mob is exclusive to the Fishing Festival?", "Shark"))
        questions.add(SphinxQuestion("Where is Trevor the Trapper found?", "Mushroom Desert"))
        questions.add(SphinxQuestion("Who helps you apply Rod Parts?", "Roddy"))
        questions.add(SphinxQuestion("Which type of Gemstone has the lowest Breaking Power?", "Ruby"))
        questions.add(SphinxQuestion("Which item rarity comes after Mythic?", "Divine"))
        questions.add(SphinxQuestion("How do you obtain the Dark Purple Dye?", "Dark Auction"))
        questions.add(SphinxQuestion("Who runs the Chocolate Factory?", "Hoppity"))
        questions.add(SphinxQuestion("How many floors are there in The Catacombs?", "7"))
        questions.add(SphinxQuestion("What is the first type of slayer Maddox offers?", "Zombie"))
        questions.add(SphinxQuestion("What item do you use to kill Pests?", "Vacuum"))
        questions.add(SphinxQuestion("Who owns the Gold Essence Shop?", "Marigold"))
        questions.add(SphinxQuestion("Which of these is NOT a type of Gemstone?", "Prismite"))
        questions.add(SphinxQuestion("What does Junker Joel collect?", "Junk"))
        questions.add(SphinxQuestion("Where is the Titanoboa found?", "Backwater Bayou"))
    }
}
