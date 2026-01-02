package com.faketils.mixin;

import com.faketils.Faketils;
import com.faketils.utils.Utils;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class AbstractBlockStateMixin {
    @Inject(method = "getOutlineShape", at = @At("HEAD"), cancellable = true)
    private void faketils$fullBlockOutline(BlockView world, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = (BlockState) (Object) this;
        if (state.getBlock() instanceof PaneBlock && Faketils.config.fullBlockPanes && Utils.isInSkyblock()) {
            cir.setReturnValue(VoxelShapes.fullCube());
        }
    }
}
