package com.faketils.mixin;

import com.faketils.config.FaketilsConfig;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderer.class})
public class MixinEntityRenderer {
    @Inject(method = {"hurtCameraEffect"}, at = {@At("HEAD")}, cancellable = true)
    public void hurtCameraEffect(CallbackInfo ci) {
        if (FaketilsConfig.INSTANCE.getNoHurtCam()) {
            ci.cancel();
        }
    }
}