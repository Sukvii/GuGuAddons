package com.gugucraft.guguaddons.client.emi;

import com.gugucraft.guguaddons.GuGuAddons;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Method;

@EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
public final class EmiClientReloadHelper {
    private static final int RELOAD_DELAY_TICKS = 2;

    private static boolean pendingReload;
    private static boolean reloadQueued;
    private static int pendingDelayTicks;
    private static String pendingReason = "client state change";

    private EmiClientReloadHelper() {
    }

    public static void requestRecipeReload(String reason) {
        if (!ModList.get().isLoaded("emi")) {
            return;
        }

        pendingReload = true;
        pendingDelayTicks = Math.max(pendingDelayTicks, RELOAD_DELAY_TICKS);
        if (reason != null && !reason.isBlank()) {
            pendingReason = reason;
        }
        scheduleIfReady();
    }

    public static void cancelPendingReload() {
        pendingReload = false;
        pendingDelayTicks = 0;
        pendingReason = "client state change";
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (pendingReload) {
            scheduleIfReady();
        }
    }

    private static void scheduleIfReady() {
        if (!pendingReload || reloadQueued || !isClientWorldReady() || !isEmiLoaded()) {
            return;
        }

        if (pendingDelayTicks > 0) {
            pendingDelayTicks--;
            return;
        }

        pendingReload = false;
        reloadQueued = true;
        Minecraft.getInstance().execute(() -> {
            reloadQueued = false;
            if (!isClientWorldReady()) {
                pendingReload = true;
                return;
            }
            reload();
        });
    }

    private static boolean isClientWorldReady() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.player != null;
    }

    private static boolean isEmiLoaded() {
        try {
            Class<?> reloadManagerClass = Class.forName("dev.emi.emi.runtime.EmiReloadManager");
            Method isLoadedMethod = reloadManagerClass.getMethod("isLoaded");
            return Boolean.TRUE.equals(isLoadedMethod.invoke(null));
        } catch (Throwable t) {
            GuGuAddons.LOGGER.warn("Failed to check EMI reload state", t);
            return true;
        }
    }

    private static void reload() {
        try {
            Class<?> reloadManagerClass = Class.forName("dev.emi.emi.runtime.EmiReloadManager");
            Method reloadMethod = reloadManagerClass.getMethod("reload");
            reloadMethod.invoke(null);
        } catch (Throwable t) {
            GuGuAddons.LOGGER.warn("Failed to refresh EMI recipes after {}", pendingReason, t);
        }
    }
}
