package com.gugucraft.guguaddons.stage;

import com.gugucraft.guguaddons.GuGuAddons;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.lang.reflect.Method;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public final class MachineRecipeStageNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private MachineRecipeStageNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                MachineRecipeStageSyncS2CPayload.TYPE,
                MachineRecipeStageSyncS2CPayload.STREAM_CODEC,
                MachineRecipeStageNetwork::handleSync);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MachineRecipeStageManager.reloadFromKubeJS(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            MachineRecipeStageManager.reloadFromKubeJS(event.getPlayerList().getServer());
            return;
        }

        event.getRelevantPlayers().forEach(MachineRecipeStageNetwork::sendSnapshot);
    }

    public static void syncAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendSnapshot(player);
        }
    }

    public static void sendSnapshot(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new MachineRecipeStageSyncS2CPayload(MachineRecipeStageManager.serverSnapshot()));
    }

    private static void handleSync(MachineRecipeStageSyncS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }

            MachineRecipeStageManager.applyClientSnapshot(payload.restrictions());
            notifyClientSnapshotChanged();
        });
    }

    private static void notifyClientSnapshotChanged() {
        try {
            Class<?> hooksClass = Class.forName(
                    "com.gugucraft.guguaddons.client.stage.MachineRecipeStageClientHooks");
            Method method = hooksClass.getMethod("onRestrictionSnapshotChanged");
            method.invoke(null);
        } catch (Throwable t) {
            GuGuAddons.LOGGER.warn("Failed to refresh client recipe stages after restriction sync", t);
        }
    }
}
