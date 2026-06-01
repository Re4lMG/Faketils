package com.faketils.mixin;

import com.faketils.Faketils;
import com.faketils.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockGetter.class)
public interface BlockViewMixin {

    @Redirect(
            method = "clipWithInteractionOverride",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/shapes/VoxelShape;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/BlockHitResult;"
            )
    )
    default BlockHitResult faketils$fullBlockPaneRaycast(
            VoxelShape shape,
            Vec3 start,
            Vec3 end,
            BlockPos pos
    ) {
        if ((Object)this instanceof BlockGetter world) {

            BlockState state = world.getBlockState(pos);

            if (state.getBlock() instanceof IronBarsBlock
                    && Faketils.config().fullBlockPanes
                    && Utils.isInSkyblock()) {

                return Shapes.block().clip(start, end, pos);
            }
        }

        return shape.clip(start, end, pos);
    }
}