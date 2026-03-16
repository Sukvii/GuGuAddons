package com.gugucraft.guguaddons.client.stock;

import java.util.concurrent.atomic.AtomicReference;

import com.gugucraft.guguaddons.Config;
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
    private static ClientSession activeSession;

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
        if (!Config.isEffectiveStockEnabled()) {
            closeActiveSession(false);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        if (activeSession == null || minecraft.screen != activeSession.screen) {
            activeSession = new ClientSession(player);
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

    private static final class ClientSession {
        private final AtomicReference<StockUiSnapshot> snapshotRef = new AtomicReference<>(StockUiSnapshot.empty());
        private final StockUiFactory.StockUiView view;
        private final StockUiScreen screen;
        private boolean suppressClosePacket;

        private ClientSession(Player player) {
            this.view = StockUiFactory.create(player, this::dispatchAction);
            this.screen = new StockUiScreen(
                    view.modularUI(),
                    Component.translatable("menu.guguaddons.stock.title"),
                    this::onScreenClosed);
        }

        private void apply(StockUiSnapshot snapshot) {
            snapshotRef.set(snapshot);
            view.applySnapshot().accept(snapshot);
        }

        private void dispatchAction(StockUiAction action, int targetStock) {
            if (!Config.isEffectiveStockEnabled()) {
                closeActiveSession(false);
                return;
            }

            StockUiSnapshot snapshot = snapshotRef.get();
            PacketDistributor.sendToServer(new StockUiActionC2SPayload(
                    action.id(),
                    snapshot.page(),
                    snapshot.selectedStock(),
                    snapshot.lotIndex(),
                    snapshot.windowIndex(),
                    targetStock));
        }

        private void closeSilently(Minecraft minecraft) {
            suppressClosePacket = true;
            if (minecraft.screen == screen) {
                minecraft.setScreen(null);
                return;
            }
            if (activeSession == this) {
                activeSession = null;
            }
        }

        private void onScreenClosed() {
            if (!suppressClosePacket && Config.isEffectiveStockEnabled()) {
                dispatchAction(StockUiAction.CLOSE, -1);
            }
            suppressClosePacket = false;
            if (activeSession == this) {
                activeSession = null;
            }
        }
    }
}
