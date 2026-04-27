package com.faketils.mixin;

import com.faketils.features.Farming;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = {"onWindowFocusChanged"}, at = {@At("HEAD")}, cancellable = true)
    private void onFocusChanged(boolean focused, CallbackInfo ci) {
        if (!focused && (Farming.isActive || Farming.killingPests))
            ci.cancel();
    }
}