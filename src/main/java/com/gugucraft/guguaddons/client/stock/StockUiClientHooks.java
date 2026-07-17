package com.gugucraft.guguaddons.client.stock;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.gugucraft.guguaddons.config.Config;
import com.gugucraft.guguaddons.stock.ui.StockUiAction;
import com.gugucraft.guguaddons.stock.ui.StockUiActionC2SPayload;
import com.gugucraft.guguaddons.stock.ui.StockUiSnapshot;
import com.gugucraft.guguaddons.stock.ui.StockUiSnapshotS2CPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public final class StockUiClientHooks {
    private static final int MAX_RECENTLY_CLOSED_NONCES = 32;
    private static ClientSession activeSession;
    private static final Set<UUID> RECENTLY_CLOSED_NONCES = new LinkedHashSet<>();

    private StockUiClientHooks() {
    }

    public static void onStockAvailabilityChanged() {
        if (Config.isEffectiveStockEnabled()) {
            return;
        }
        closeActiveSession(true);
    }

    public static void resetSession() {
        closeActiveSession(false);
    }

    public static void handleSnapshot(StockUiSnapshotS2CPayload payload) {
        UUID sessionNonce = payload.sessionNonce();
        if (RECENTLY_CLOSED_NONCES.contains(sessionNonce)) {
            return;
        }
        if (!Config.isEffectiveStockEnabled()) {
            closeActiveSession(false);
            rememberClosedNonce(sessionNonce);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        if (activeSession == null || !activeSession.nonce.equals(sessionNonce)) {
            if (activeSession != null) {
                activeSession.closeSilently(minecraft);
            }
            activeSession = new ClientSession(player, sessionNonce);
            minecraft.setScreen(activeSession.screen);
        } else if (minecraft.screen != activeSession.screen) {
            minecraft.setScreen(activeSession.screen);
        }

        activeSession.apply(payload.snapshot());
    }

    private static void closeActiveSession(boolean notify) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientSession session = activeSession;
        if (session == null) {
            return;
        }

        if (notify && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("menu.guguaddons.stock.message.disabled"), true);
        }

        session.closeSilently(minecraft);
    }

    private static void rememberClosedNonce(UUID nonce) {
        RECENTLY_CLOSED_NONCES.add(nonce);
        while (RECENTLY_CLOSED_NONCES.size() > MAX_RECENTLY_CLOSED_NONCES) {
            Iterator<UUID> iterator = RECENTLY_CLOSED_NONCES.iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private static final class ClientSession {
        private final UUID nonce;
        private final StockUiFactory.StockUiView view;
        private final StockUiScreen screen;
        private long nextRequestId = 1L;
        private boolean suppressClosePacket;

        private ClientSession(Player player, UUID nonce) {
            this.nonce = nonce;
            this.view = StockUiFactory.create(player, this::dispatchAction);
            this.screen = new StockUiScreen(
                    view.modularUI(),
                    Component.translatable("menu.guguaddons.stock.title"),
                    this::onScreenClosed);
        }

        private void apply(StockUiSnapshot snapshot) {
            view.applySnapshot().accept(snapshot);
        }

        private void dispatchAction(StockUiAction action, int targetStock) {
            if (action != StockUiAction.CLOSE && !Config.isEffectiveStockEnabled()) {
                closeActiveSession(false);
                return;
            }

            long requestId = nextRequestId++;
            if (requestId <= 0L) {
                return;
            }
            PacketDistributor.sendToServer(new StockUiActionC2SPayload(
                    nonce,
                    requestId,
                    action.id(),
                    targetStock));
        }

        private void closeSilently(Minecraft minecraft) {
            suppressClosePacket = true;
            rememberClosedNonce(nonce);
            if (activeSession == this) {
                activeSession = null;
            }
            if (minecraft.screen == screen) {
                minecraft.setScreen(null);
                return;
            }
        }

        private void onScreenClosed() {
            if (!suppressClosePacket) {
                rememberClosedNonce(nonce);
                dispatchAction(StockUiAction.CLOSE, -1);
            }
            suppressClosePacket = false;
            if (activeSession == this) {
                activeSession = null;
            }
        }
    }
}
