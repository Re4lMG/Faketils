package com.faketils.mixin;

import com.faketils.config.FaketilsConfig;
import com.faketils.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockReed;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class MixinBlockReed {

    @Inject(method = "setBlockBoundsBasedOnState", at = @At("HEAD"), cancellable = true)
    private void modifyBoundingBox(IBlockAccess worldIn, BlockPos pos, CallbackInfo ci) {
        Block self = (Block) (Object) this;

        if (self instanceof BlockReed) {
            if (!Utils.INSTANCE.isInSkyblock()) return;

            if (FaketilsConfig.INSTANCE.getFullCane()) {
                self.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F); // set visual bounds
                ci.cancel();
            }
        }
    }
}
