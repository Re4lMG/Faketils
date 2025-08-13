package com.faketils

import com.faketils.config.Config
import com.faketils.config.PersistentData
import com.faketils.commands.Command
import com.faketils.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
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
    modid = "faketils",
    name = "Faketils",
    version = "0.1",
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
        persistentData = PersistentData.load()
        config = Config
        config.initialize()
    }

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        ClientCommandHandler.instance.registerCommand(Command())

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

        val player: EntityPlayerSP? = mc.thePlayer

        tickAmount++
        if (tickAmount % 20 == 0) {
            player?.let {
                Utils.checkForSkyblock()
                Utils.checkForDungeons()
            }
        }
    }

    companion object {
        val mc: Minecraft = Minecraft.getMinecraft()
        var currentGui: GuiScreen? = null

        const val MOD_ID = "faketils"
        const val NAME = "Faketils"

        lateinit var configDirectory: File
        lateinit var config: Config
        lateinit var persistentData: PersistentData

        lateinit var metadata: ModMetadata


        fun saveAll() {
            config.markDirty()
            config.writeData()
            persistentData.save()
        }
    }
}