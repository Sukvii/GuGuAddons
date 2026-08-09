package com.gugucraft.guguaddons.client.config;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.client.emi.EmiClientBakeHelper;
import com.gugucraft.guguaddons.client.stock.StockUiClientHooks;
import com.gugucraft.guguaddons.config.sync.ConfigSyncState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
public final class ClientConfigHooks {
    private ClientConfigHooks() {
    }

    public static void onServerConfigSnapshotChanged() {
        StockUiClientHooks.onStockAvailabilityChanged();
        EmiClientBakeHelper.requestRecipeBake("config snapshot change");
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ConfigSyncState.clearServerSnapshot();
        StockUiClientHooks.resetSession();
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        StockUiClientHooks.resetSession();
        ConfigSyncState.clearServerSnapshot();
        EmiClientBakeHelper.cancelPendingBake();
    }
}
