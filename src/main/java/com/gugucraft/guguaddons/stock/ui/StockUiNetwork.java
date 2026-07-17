package com.gugucraft.guguaddons.stock.ui;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.config.Config;
import com.gugucraft.guguaddons.util.ReflectionCache;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public final class StockUiNetwork {
    private static final String PROTOCOL_VERSION = "2";
    private static final long SESSION_TTL_TICKS = 6_000L;
    private static final Map<UUID, ServerSession> ACTIVE_SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTION_TICKS = new HashMap<>();
    private static final ReflectionCache.MethodRef HANDLE_SNAPSHOT_METHOD = ReflectionCache.publicMethod(
            "com.gugucraft.guguaddons.client.stock.StockUiClientHooks",
            "handleSnapshot",
            StockUiSnapshotS2CPayload.class);

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
        UUID playerId = player.getUUID();
        if (!Config.isConfiguredStockEnabled()) {
            ACTIVE_SESSIONS.remove(playerId);
            notifyStockDisabled(player);
            return;
        }

        long now = player.serverLevel().getServer().getTickCount();
        ServerSession session = new ServerSession(UUID.randomUUID(), StockUiService.defaultState(),
                now + SESSION_TTL_TICKS);
        ACTIVE_SESSIONS.put(playerId, session);
        try {
            sendSnapshot(player, session);
        } catch (Throwable t) {
            ACTIVE_SESSIONS.remove(playerId, session);
            GuGuAddons.LOGGER.error("Failed to open stock UI session for player {}", playerId, t);
        }
    }

    private static void handleActionPacket(StockUiActionC2SPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        context.enqueueWork(() -> {
            UUID playerId = player.getUUID();
            ServerSession session = ACTIVE_SESSIONS.get(playerId);
            if (session == null || !session.nonce.equals(payload.sessionNonce())) {
                return;
            }

            long now = player.serverLevel().getServer().getTickCount();
            if (session.expiresAtTick <= now) {
                ACTIVE_SESSIONS.remove(playerId, session);
                return;
            }
            if (payload.requestId() <= 0L || payload.requestId() <= session.lastRequestId) {
                return;
            }
            session.lastRequestId = payload.requestId();

            StockUiAction action = payload.action();
            if (action == StockUiAction.CLOSE) {
                ACTIVE_SESSIONS.remove(playerId, session);
                return;
            }
            if (!Config.isConfiguredStockEnabled()) {
                ACTIVE_SESSIONS.remove(playerId, session);
                notifyStockDisabled(player);
                return;
            }

            Long lastActionTick = LAST_ACTION_TICKS.get(playerId);
            if (lastActionTick != null && lastActionTick == now) {
                return;
            }
            LAST_ACTION_TICKS.put(playerId, now);
            session.expiresAtTick = now + SESSION_TTL_TICKS;

            try {
                session.state = StockUiService.applyAction(player, session.state, action, payload.targetStock());
                sendSnapshot(player, session);
            } catch (Throwable t) {
                ACTIVE_SESSIONS.remove(playerId, session);
                GuGuAddons.LOGGER.error("Failed to process stock UI request {} for player {}", payload.requestId(),
                        playerId, t);
            }
        });
    }

    private static void sendSnapshot(ServerPlayer player, ServerSession session) {
        StockUiSnapshot snapshot = StockUiService.createSnapshot(player, session.state);
        PacketDistributor.sendToPlayer(player, new StockUiSnapshotS2CPayload(session.nonce, snapshot));
    }

    private static void handleSnapshotPacket(StockUiSnapshotS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) {
                return;
            }

            try {
                ReflectionCache.MethodLookup lookup = HANDLE_SNAPSHOT_METHOD.lookup();
                Method method = lookup.method();
                if (method == null) {
                    if (lookup.reportFailure()) {
                        GuGuAddons.LOGGER.error("Failed to open LDLIB stock UI", lookup.failure());
                    }
                    return;
                }
                method.invoke(null, payload);
            } catch (Throwable t) {
                GuGuAddons.LOGGER.error("Failed to open LDLIB stock UI", t);
            }
        });
    }

    private static void notifyStockDisabled(ServerPlayer player) {
        player.displayClientMessage(
                Component.translatable("menu.guguaddons.stock.message.disabled").withStyle(ChatFormatting.RED),
                true);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            ACTIVE_SESSIONS.remove(playerId);
            LAST_ACTION_TICKS.remove(playerId);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().getTickCount();
        if (now % 20L != 0L) {
            return;
        }
        ACTIVE_SESSIONS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick <= now);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE_SESSIONS.clear();
        LAST_ACTION_TICKS.clear();
    }

    private static final class ServerSession {
        private final UUID nonce;
        private StockUiSessionState state;
        private long expiresAtTick;
        private long lastRequestId;

        private ServerSession(UUID nonce, StockUiSessionState state, long expiresAtTick) {
            this.nonce = nonce;
            this.state = state;
            this.expiresAtTick = expiresAtTick;
            this.lastRequestId = 0L;
        }
    }
}
