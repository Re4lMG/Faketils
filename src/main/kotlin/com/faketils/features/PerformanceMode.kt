package com.faketils.features

import com.faketils.Faketils
import com.faketils.config.FaketilsConfig
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object PerformanceMode {
    private val mc: Minecraft = Minecraft.getMinecraft()

    private var renderDistanceBefore = 0
    private var maxFpsBefore = 0
    private var fancyGraphicsBefore = false
    private var aoBefore = 0
    private var cloudsBefore = 0
    private var mipmapBefore = 0
    private var vsyncBefore = false
    private var particlesBefore = 0
    private var useVboBefore = false

    private var enabled = false

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (mc.thePlayer == null || mc.theWorld == null) return

        if (FaketilsConfig.performanceMode) {
            if (!isEnabled()) {
                start()
            }
        } else {
            if (isEnabled()) {
                stop()
            }
        }
    }


    fun start() {
        if (!FaketilsConfig.performanceMode || enabled) return
        enabled = true

        renderDistanceBefore = mc.gameSettings.renderDistanceChunks
        maxFpsBefore = mc.gameSettings.limitFramerate
        fancyGraphicsBefore = mc.gameSettings.fancyGraphics
        aoBefore = mc.gameSettings.ambientOcclusion
        cloudsBefore = mc.gameSettings.clouds
        mipmapBefore = mc.gameSettings.mipmapLevels
        vsyncBefore = mc.gameSettings.enableVsync
        particlesBefore = mc.gameSettings.particleSetting
        useVboBefore = mc.gameSettings.useVbo

        mc.gameSettings.renderDistanceChunks = 1
        mc.gameSettings.limitFramerate = -1
        mc.gameSettings.fancyGraphics = false
        mc.gameSettings.ambientOcclusion = 0
        mc.gameSettings.clouds = 0
        mc.gameSettings.mipmapLevels = 0
        mc.gameSettings.enableVsync = false
        mc.gameSettings.particleSetting = 2
        mc.gameSettings.useVbo = true

        mc.addScheduledTask { mc.renderGlobal.loadRenderers() }
    }

    fun stop() {
        if (!enabled) return
        enabled = false

        mc.gameSettings.renderDistanceChunks = renderDistanceBefore
        mc.gameSettings.limitFramerate = maxFpsBefore
        mc.gameSettings.fancyGraphics = fancyGraphicsBefore
        mc.gameSettings.ambientOcclusion = aoBefore
        mc.gameSettings.clouds = cloudsBefore
        mc.gameSettings.mipmapLevels = mipmapBefore
        mc.gameSettings.enableVsync = vsyncBefore
        mc.gameSettings.particleSetting = particlesBefore
        mc.gameSettings.useVbo = useVboBefore

        renderDistanceBefore = 0
        maxFpsBefore = 0

        mc.addScheduledTask { mc.renderGlobal.loadRenderers() }
    }

    fun isEnabled(): Boolean = enabled
}
