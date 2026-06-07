package com.faketils.mixin;

import com.faketils.Faketils;
import com.faketils.utils.Utils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyReturnValue(method = "getDimensions", at = @At("RETURN"))
    private EntityDimensions modifyDimensions(EntityDimensions original, Pose pose) {
        if (Utils.isSelf(this) && Faketils.config().oldSneak && pose == Pose.CROUCHING) {
            return original.withEyeHeight(1.54f);
        }
        return original;
    }
}