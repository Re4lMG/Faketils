package com.faketils.mixin;

import com.faketils.config.Config;
import com.faketils.events.FtEvent;
import com.faketils.events.FtEventBus;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void disableHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (Config.INSTANCE.noHurtCam) {
            ci.cancel();
        }
    }
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", shift = At.Shift.AFTER))
    private void faketils$afterHudRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext context) {
        FtEventBus.emit(new FtEvent.HudRender(context, tickCounter.getDynamicDeltaTicks()));
    }
}