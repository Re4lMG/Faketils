package com.faketils.commands

import cc.polyfrost.oneconfig.utils.commands.annotations.Command
import cc.polyfrost.oneconfig.utils.commands.annotations.Main
import com.faketils.Faketils

@Command(value = "ft", aliases = ["faketils", "faketil"], description = "FarmHelper main command")
class FaketilsCommand {
    @Main
    fun mainCommand() {
        Faketils.config.openGui()
    }
}