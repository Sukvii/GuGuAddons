package com.gugucraft.guguaddons.compat.astages;

import com.alessandro.astages.api.event.sync.ClientSynchronizeServerStagesEvent;
import com.alessandro.astages.api.event.sync.ClientSynchronizeStagesEvent;
import com.alessandro.astages.api.holder.AClientHolder;
import com.alessandro.astages.api.holder.AHolder;
import com.alessandro.astages.api.util.AStagesClientUtils;
import com.alessandro.astages.api.util.AStagesUtils;
import com.gugucraft.guguaddons.GuGuAddons;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.lang.reflect.Method;
import java.util.UUID;

public final class AStagesHelper {
    private AStagesHelper() {
    }

    public static boolean hasStage(ServerPlayer player, String stage) {
        if (player == null || isBlank(stage)) {
            return false;
        }
        return AStagesUtils.hasStage(AHolder.serverAndPlayer(player), stage);
    }

    public static boolean hasStage(UUID playerId, String stage) {
        if (playerId == null || isBlank(stage)) {
            return false;
        }
        return AStagesUtils.hasStage(AHolder.player(playerId), stage)
                || AStagesUtils.hasStage(AHolder.server(), stage);
    }

    public static boolean clientHasStage(String stage) {
        if (isBlank(stage)) {
            return false;
        }
        return AStagesClientUtils.hasStage(AClientHolder.serverAndPlayer(), stage);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onPlayerStagesSynced(ClientSynchronizeStagesEvent event) {
            notifyClientStageChanged();
        }

        @SubscribeEvent
        public static void onServerStagesSynced(ClientSynchronizeServerStagesEvent event) {
            notifyClientStageChanged();
        }

        private static void notifyClientStageChanged() {
            try {
                Class<?> hooksClass = Class.forName(
                        "com.gugucraft.guguaddons.client.stage.MachineRecipeStageClientHooks");
                Method method = hooksClass.getMethod("onAStagesChanged");
                method.invoke(null);
            } catch (Throwable t) {
                GuGuAddons.LOGGER.warn("Failed to refresh client recipe stages after AStages sync", t);
            }
        }
    }
}
