package com.gugucraft.guguaddons.stock.ui;

import java.lang.reflect.Method;

import com.gugucraft.guguaddons.GuGuAddons;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class StockUiNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private StockUiNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToServer(
                StockUiActionC2SPayload.TYPE,
                StockUiActionC2SPayload.STREAM_CODEC,
                StockUiNetwork::handleActionPacket);

        registrar.playToClient(
                StockUiSnapshotS2CPayload.TYPE,
                StockUiSnapshotS2CPayload.STREAM_CODEC,
                StockUiNetwork::handleSnapshotPacket);
    }

    public static void openFor(ServerPlayer player) {
        sendSnapshot(player, StockUiService.defaultState());
    }

    private static void handleActionPacket(StockUiActionC2SPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        context.enqueueWork(() -> {
            StockUiAction action = payload.action();
            if (action == StockUiAction.CLOSE) {
                return;
            }

            StockUiSessionState requestState = new StockUiSessionState(
                    payload.page(),
                    payload.selectedStock(),
                    payload.lotIndex(),
                    payload.windowIndex());

            StockUiSessionState nextState = StockUiService.applyAction(player, requestState, action, payload.targetStock());
            sendSnapshot(player, nextState);
        });
    }

    private static void sendSnapshot(ServerPlayer player, StockUiSessionState state) {
        StockUiSnapshot snapshot = StockUiService.createSnapshot(player, state);
        PacketDistributor.sendToPlayer(player, new StockUiSnapshotS2CPayload(snapshot));
    }

    private static void handleSnapshotPacket(StockUiSnapshotS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }

            try {
                Class<?> hooksClass = Class.forName("com.gugucraft.guguaddons.client.stock.StockUiClientHooks");
                Method method = hooksClass.getMethod("handleSnapshot", StockUiSnapshotS2CPayload.class);
                method.invoke(null, payload);
            } catch (Throwable t) {
                GuGuAddons.LOGGER.error("Failed to open LDLIB stock UI", t);
            }
        });
    }
}
