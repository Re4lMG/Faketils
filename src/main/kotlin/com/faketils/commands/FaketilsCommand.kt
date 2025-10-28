package com.faketils.commands

import cc.polyfrost.oneconfig.utils.commands.annotations.Command
import cc.polyfrost.oneconfig.utils.commands.annotations.Main
import com.faketils.Faketils
import com.faketils.config.FaketilsConfig

@Command(value = "ft", aliases = ["faketils", "faketil"], description = "FarmHelper main command")
object FaketilsCommand {
    @Main
    fun mainCommand() {
        FaketilsConfig.openGui()
    }
}