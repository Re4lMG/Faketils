package com.faketils.commands.warp

import com.faketils.utils.Utils
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.client.Minecraft

class WarpCommand(
    private val alias: String,
    private val warpName: String = alias
) : CommandBase() {

    private val mc: Minecraft = Minecraft.getMinecraft()

    override fun getCommandName(): String = alias

    override fun getCommandUsage(sender: ICommandSender?): String = "/$alias"

    override fun getCommandAliases(): MutableList<String> = mutableListOf()

    override fun getRequiredPermissionLevel(): Int = 0
    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean = true

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (Utils.isInSkyblock()) {
            mc.thePlayer.sendChatMessage("/warp $warpName")
        }
    }
}
