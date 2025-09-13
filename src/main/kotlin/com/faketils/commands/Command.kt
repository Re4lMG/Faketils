package com.faketils.commands

import com.faketils.Faketils
import com.faketils.config.Config
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class Command : CommandBase() {
    override fun getCommandName() = "ft"

    override fun getCommandAliases() = listOf("ft", "faketils", "faketil")

    override fun getCommandUsage(sender: ICommandSender?) = "/$commandName"

    override fun getRequiredPermissionLevel() = 0

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        Faketils.currentGui = Config.gui()
    }
}