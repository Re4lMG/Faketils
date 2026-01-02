package com.faketils.mixin;

import com.faketils.Faketils;
import com.faketils.utils.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockView.class)
public interface BlockViewMixin {
    @Redirect(method = "raycastBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/shape/VoxelShape;raycast(" + "Lnet/minecraft/util/math/Vec3d;" + "Lnet/minecraft/util/math/Vec3d;" + "Lnet/minecraft/util/math/BlockPos;" + ")Lnet/minecraft/util/hit/BlockHitResult;"))
    default BlockHitResult faketils$fullBlockPaneRaycast(VoxelShape shape, Vec3d start, Vec3d end, BlockPos pos) {
        if (this instanceof BlockView world) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof PaneBlock && Faketils.config.fullBlockPanes && Utils.isInSkyblock()) {
                return VoxelShapes.fullCube().raycast(start, end, pos);
            }
        }
        return shape.raycast(start, end, pos);
    }
}
