package com.faketils

import cc.polyfrost.oneconfig.utils.commands.CommandManager
import com.faketils.commands.FaketilsCommand
import com.faketils.commands.FarmingCommand
import com.faketils.commands.warp.WarpCommandHandler
import com.faketils.config.FaketilsConfig
import com.faketils.features.*
import com.faketils.utils.TitleUtil
import com.faketils.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.ModMetadata
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.io.File

@Mod(
    modid = Faketils.MOD_ID,
    name = Faketils.NAME,
    useMetadata = true,
    clientSideOnly = true
)
class Faketils {

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        metadata = event.modMetadata
        val directory = File(event.modConfigurationDirectory, event.modMetadata.modId)
        directory.mkdirs()
        configDirectory = directory
        config = FaketilsConfig()

        WarpCommandHandler()
    }

    private val farming = Farming()

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        CommandManager.register(FaketilsCommand())
        ClientCommandHandler.instance.registerCommand(FarmingCommand())

        MinecraftForge.EVENT_BUS.register(FireFreezeTimer())
        MinecraftForge.EVENT_BUS.register(FishingTickHandler)
        MinecraftForge.EVENT_BUS.register(Misc())
        MinecraftForge.EVENT_BUS.register(PestsHelper())
        FarmingCommand.loadWaypoints()

        farming.init()
        MinecraftForge.EVENT_BUS.register(farming)
        MinecraftForge.EVENT_BUS.register(PerformanceMode)
        MinecraftForge.EVENT_BUS.register(TitleUtil)

        listOf(
            this
        ).forEach(MinecraftForge.EVENT_BUS::register)
    }

    private var tickAmount = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || currentGui == null) return
        mc.displayGuiScreen(currentGui)
        currentGui = null

        tickAmount++
        if (tickAmount % 20 == 0) {
            if (mc.thePlayer != null) {
                Utils.isInSkyblock()
                Utils.isInDungeons()
            }
        }
    }

    companion object {
        val mc: Minecraft = Minecraft.getMinecraft()
        var currentGui: GuiScreen? = null

        const val MOD_ID = "faketils"
        const val NAME = "Faketils"

        lateinit var configDirectory: File
        lateinit var config: FaketilsConfig

        lateinit var metadata: ModMetadata
    }
}