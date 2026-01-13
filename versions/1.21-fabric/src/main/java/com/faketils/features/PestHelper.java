package com.faketils.features;

import com.faketils.config.Config;
import com.faketils.events.FtEvent;
import com.faketils.events.FtEventBus;
import com.faketils.utils.RenderUtils;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class PestHelper {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void initialize() {
        WorldRenderEvents.AFTER_ENTITIES.register(PestHelper::onRenderWorldLast);
    }

    private static void onRenderWorldLast(WorldRenderContext context) {
        if (!Utils.isInSkyblock() || !Config.INSTANCE.pestHelper) return;
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = context.matrixStack();
        float tickDelta = context.tickCounter().getDynamicDeltaTicks();

        for (ArmorStandEntity armorStand : mc.world.getEntitiesByClass(
                ArmorStandEntity.class,
                mc.player.getBoundingBox().expand(50),
                e -> true
        )) {
            Text customName = armorStand.getCustomName();
            if (customName == null) continue;

            String name = customName.getString();
            if (!name.startsWith("ൠ")) continue;

            Vec3d target = armorStand.getLerpedPos(tickDelta);

            Vec3d eyePos = mc.player.getLerpedPos(tickDelta).add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);

            RenderUtils.renderWaypointMarker(matrices, target, eyePos, 0xFF00FFFF, name);

            float thickness = 4.0f;

            //RenderUtils.renderLine(matrices, eyePos, target, 0xFF00FFFF, thickness);
        }
    }
}
