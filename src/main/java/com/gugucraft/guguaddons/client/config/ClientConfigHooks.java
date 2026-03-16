package com.gugucraft.guguaddons.client.config;

import java.lang.reflect.Method;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.client.stock.StockUiClientHooks;
import com.gugucraft.guguaddons.config.sync.ConfigSyncState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
public final class ClientConfigHooks {
    private ClientConfigHooks() {
    }

    public static void onServerConfigSnapshotChanged() {
        StockUiClientHooks.onStockAvailabilityChanged();
        refreshEmiRecipes();
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
        refreshEmiRecipes();
    }

    private static void refreshEmiRecipes() {
        if (!ModList.get().isLoaded("emi")) {
            return;
        }

        try {
            Class<?> reloadManagerClass = Class.forName("dev.emi.emi.runtime.EmiReloadManager");
            Method reloadRecipesMethod = reloadManagerClass.getMethod("reloadRecipes");
            reloadRecipesMethod.invoke(null);
        } catch (Throwable t) {
            GuGuAddons.LOGGER.warn("Failed to refresh EMI recipes after config snapshot change", t);
        }
    }
}
