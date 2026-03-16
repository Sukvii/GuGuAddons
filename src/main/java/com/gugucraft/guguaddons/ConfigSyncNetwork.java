package com.gugucraft.guguaddons;

import java.lang.reflect.Method;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public final class ConfigSyncNetwork {
    private static final String PROTOCOL_VERSION = "2";

    private ConfigSyncNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                ConfigSyncS2CPayload.TYPE,
                ConfigSyncS2CPayload.STREAM_CODEC,
                ConfigSyncNetwork::handleConfigSnapshotPacket);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendSnapshot(player, Config.captureConfiguredSnapshot());
        }
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != Config.SPEC || event.getConfig().getType() != ModConfig.Type.SERVER) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        ServerConfigSnapshot snapshot = Config.captureConfiguredSnapshot();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendSnapshot(player, snapshot);
            server.getCommands().sendCommands(player);
        }
    }

    private static void sendSnapshot(ServerPlayer player, ServerConfigSnapshot snapshot) {
        PacketDistributor.sendToPlayer(player, new ConfigSyncS2CPayload(snapshot));
    }

    private static void handleConfigSnapshotPacket(ConfigSyncS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }

            ConfigSyncState.applyServerSnapshot(payload.snapshot());

            try {
                Class<?> hooksClass = Class.forName("com.gugucraft.guguaddons.client.ClientConfigHooks");
                Method method = hooksClass.getMethod("onServerConfigSnapshotChanged");
                method.invoke(null);
            } catch (Throwable t) {
                GuGuAddons.LOGGER.error("Failed to refresh client state after config sync", t);
            }
        });
    }
}
