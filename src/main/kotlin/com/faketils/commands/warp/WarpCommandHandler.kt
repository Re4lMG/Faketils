package com.faketils.commands.warp

import net.minecraftforge.client.ClientCommandHandler

class WarpCommandHandler {

    init {
        registerCommands()
    }

    private fun registerCommands() {
        // warps
        registerCommand(WarpCommand("hub"))
        registerCommand(WarpCommand("deep"))
        registerCommand(WarpCommand("nether"))
        registerCommand(WarpCommand("isle"))
        registerCommand(WarpCommand("crimson"))
        registerCommand(WarpCommand("mines"))
        registerCommand(WarpCommand("forge"))
        registerCommand(WarpCommand("crystals"))
        registerCommand(WarpCommand("gold"))
        registerCommand(WarpCommand("desert"))
        registerCommand(WarpCommand("spider"))
        registerCommand(WarpCommand("barn"))
        registerCommand(WarpCommand("end"))
        registerCommand(WarpCommand("park"))
        registerCommand(WarpCommand("castle"))
        registerCommand(WarpCommand("museum"))
        registerCommand(WarpCommand("da"))
        registerCommand(WarpCommand("crypt"))
        registerCommand(WarpCommand("nest"))
        registerCommand(WarpCommand("void"))
        registerCommand(WarpCommand("drag"))
        registerCommand(WarpCommand("jungle"))
        registerCommand(WarpCommand("howl"))
        registerCommand(WarpCommand("garden"))

        registerCommand(WarpCommand("wk", "kuudra"))
        registerCommand(WarpCommand("dun", "dungeon_hub"))
    }

    private fun registerCommand(warpCommand: WarpCommand) {
        ClientCommandHandler.instance.registerCommand(warpCommand)
    }
}
