package com.faketils.mixin;

import com.faketils.Faketils;
import com.faketils.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class AbstractBlockStateMixin {

    @Inject(
            method = "getShape",
            at = @At("HEAD"),
            cancellable = true
    )
    private void faketils$fullBlockOutline(
            BlockGetter world,
            BlockPos pos,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        BlockState state = (BlockState) (Object) this;

        if (state.getBlock() instanceof IronBarsBlock
                && Faketils.config().fullBlockPanes
                && Utils.isInSkyblock()) {

            cir.setReturnValue(Shapes.block());
        }
    }
}