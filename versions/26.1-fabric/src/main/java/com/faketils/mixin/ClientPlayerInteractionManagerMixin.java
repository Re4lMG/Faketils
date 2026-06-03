package com.faketils.mixin;

import com.faketils.features.Farming;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(
            method = "destroyBlock",
            at = @At("HEAD")
    )
    private void faketils$onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Farming.onBlockBroken(pos);
    }
}