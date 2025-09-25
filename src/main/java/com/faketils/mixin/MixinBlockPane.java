package com.faketils.mixin;

import com.faketils.Faketils;
import com.faketils.utils.Utils;
import net.minecraft.block.BlockPane;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockPane.class)
public abstract class MixinBlockPane {

    @Inject(method = "setBlockBoundsBasedOnState", at = @At("HEAD"), cancellable = true)
    private void modifyPaneBoundingBox(IBlockAccess worldIn, BlockPos pos, CallbackInfo ci) {
        if (!Utils.INSTANCE.isInSkyblock()) return;
        if (Faketils.config.getFullBlockPanes()) {
            ((BlockPane)(Object)this).setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            ci.cancel();
        }
    }
}
