package com.faketils.features;

import com.faketils.config.Config;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;

public class PestHelper {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void initialize() {
        WorldRenderEvents.AFTER_ENTITIES.register(PestHelper::onRenderWorldLast);
    }

    private static void onRenderWorldLast(WorldRenderContext context) {
        if (!Utils.isInSkyblock() || !Config.INSTANCE.pestHelper) return;
        if (mc.currentScreen != null || mc.player == null || mc.world == null) return;

        for (ArmorStandEntity armorStand : mc.world.getEntitiesByClass(ArmorStandEntity.class, mc.player.getBoundingBox().expand(50), e -> true)) {
            Text customName = armorStand.getCustomName();
            if (customName == null) continue;
            String name = customName.getString();
            if (name.startsWith("ൠ")) {
                Utils.FtVec pos = new Utils.FtVec(armorStand.getX(), armorStand.getY(), armorStand.getZ());
                Utils.drawFilledBox(context, pos, 1, 1, 1, new float[]{0f, 1f, 1f}, 1f, true);
                Utils.drawLineFromCursor(context, pos, new float[]{0f, 0f, 1f}, 6f, true);
            }
        }
    }
}
