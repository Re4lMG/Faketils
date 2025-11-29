package com.faketils.mixin;

import com.faketils.features.ItemRarity;
import com.faketils.features.Misc;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public class MixinRenderItem {
    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V", at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/GlStateManager.scale(FFF)V", shift = At.Shift.AFTER))
    private void renderItemPre(ItemStack stack, IBakedModel model, CallbackInfo ci) {
        Misc.INSTANCE.renderItemPre(stack, model, ci);
    }
    @Inject(method = "renderItemIntoGUI(Lnet/minecraft/item/ItemStack;II)V", at = @At("HEAD"))
    private void renderRarity(ItemStack itemStack, int xPosition, int yPosition, CallbackInfo info) {
        ItemRarity.INSTANCE.renderRarityOverlay(itemStack, xPosition, yPosition);
    }
}
