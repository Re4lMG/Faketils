package com.faketils.mixin;

import com.faketils.features.Farming;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void faketils$onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Farming.onBlockBroken(pos);
    }
}