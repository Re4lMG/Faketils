package com.faketils.mixin;

import com.faketils.Faketils;
import com.faketils.events.FtEvent;
import com.faketils.events.FtEventBus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void disableHurtCam(CallbackInfo ci) {
        if (Faketils.config().noHurtCam) {
            ci.cancel();
        }
    }

    @Inject(
            method = "extractGui",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;applyCursor(Lcom/mojang/blaze3d/platform/Window;)V"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void faketils$afterHudExtract(
            DeltaTracker deltaTracker,
            boolean shouldRenderLevel,
            boolean resourcesLoaded,
            CallbackInfo ci,
            ProfilerFiller profiler,
            int xMouse,
            int yMouse,
            GuiGraphicsExtractor graphics
    ) {
        FtEventBus.emit(new FtEvent.HudRender(
                graphics,
                deltaTracker.getGameTimeDeltaPartialTick(false)
        ));
    }
}