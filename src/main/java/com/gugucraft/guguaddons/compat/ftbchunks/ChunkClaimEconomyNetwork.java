package com.gugucraft.guguaddons.compat.ftbchunks;

import java.lang.reflect.Method;

import com.gugucraft.guguaddons.GuGuAddons;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ChunkClaimEconomyNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                ChunkClaimPromptS2CPayload.TYPE,
                ChunkClaimPromptS2CPayload.STREAM_CODEC,
                ChunkClaimEconomyNetwork::handlePromptPacket);

        registrar.playToServer(
                ChunkClaimDecisionC2SPayload.TYPE,
                ChunkClaimDecisionC2SPayload.STREAM_CODEC,
                ChunkClaimEconomyNetwork::handleDecisionPacket);

        registrar.playToClient(
                ChunkClaimToastS2CPayload.TYPE,
                ChunkClaimToastS2CPayload.STREAM_CODEC,
                ChunkClaimEconomyNetwork::handleToastPacket);
    }

    public static void sendClaimPrompt(ServerPlayer player, int sessionId, int chunkCount, int totalCost) {
        PacketDistributor.sendToPlayer(player, new ChunkClaimPromptS2CPayload(sessionId, chunkCount, totalCost));
    }

    public static void sendToast(ServerPlayer player, String messageKey, String... args) {
        int argCount = Math.max(0, Math.min(3, args == null ? 0 : args.length));
        String arg1 = argCount > 0 ? args[0] : "";
        String arg2 = argCount > 1 ? args[1] : "";
        String arg3 = argCount > 2 ? args[2] : "";
        PacketDistributor.sendToPlayer(player, new ChunkClaimToastS2CPayload(messageKey, argCount, arg1, arg2, arg3));
    }

    private static void handlePromptPacket(ChunkClaimPromptS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }

            try {
                Class<?> hooksClass = Class
                        .forName("com.gugucraft.guguaddons.client.ftbchunks.ChunkClaimEconomyClientHooks");
                Method method = hooksClass.getMethod("openClaimPrompt", ChunkClaimPromptS2CPayload.class);
                method.invoke(null, payload);
            } catch (Throwable t) {
                GuGuAddons.LOGGER.error("Failed to open chunk claim confirmation prompt", t);
            }
        });
    }

    private static void handleDecisionPacket(ChunkClaimDecisionC2SPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        context.enqueueWork(() -> {
            if (payload.confirmed()) {
                ChunkClaimEconomyHandler.confirmSession(player, payload.sessionId());
            } else {
                ChunkClaimEconomyHandler.cancelSession(player, payload.sessionId(), false);
            }
        });
    }

    private static void handleToastPacket(ChunkClaimToastS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }

            try {
                Class<?> hooksClass = Class
                        .forName("com.gugucraft.guguaddons.client.ftbchunks.ChunkClaimEconomyClientHooks");
                Method method = hooksClass.getMethod("showToast", ChunkClaimToastS2CPayload.class);
                method.invoke(null, payload);
            } catch (Throwable t) {
                GuGuAddons.LOGGER.error("Failed to show chunk claim toast", t);
            }
        });
    }
}
