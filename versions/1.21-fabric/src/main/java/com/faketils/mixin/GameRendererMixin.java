package com.faketils.mixin;

import com.faketils.config.Config;
import net.minecraft.client.render.GameRenderer;
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
}