package com.faketils.features;

import com.faketils.config.Config;
import com.faketils.events.FtEvent;
import com.faketils.events.FtEventBus;
import com.faketils.utils.RenderUtils;
import com.faketils.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class PestHelper {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void initialize() {
        FtEventBus.onEvent(FtEvent.WorldRender.class, PestHelper::onRenderWorldLast);
    }

    private static void onRenderWorldLast(FtEvent.WorldRender event) {
        if (!Utils.isInSkyblock() || !Config.INSTANCE.pestHelper) return;
        if (mc.player == null || mc.world == null) return;

        Vec3d cameraPos = event.camera.getPos();

        for (ArmorStandEntity armorStand : mc.world.getEntitiesByClass(
                ArmorStandEntity.class,
                mc.player.getBoundingBox().expand(50),
                e -> true
        )) {
            Text customName = armorStand.getCustomName();
            if (customName == null) continue;

            String name = customName.getString();
            if (!name.startsWith("ൠ")) continue;

            Vec3d target = armorStand.getLerpedPos(event.tickDelta);

            RenderUtils.renderWaypointMarker(
                    target,
                    cameraPos,
                    0xFF00FFFF,
                    name,
                    event
            );
        }
    }
}
