package com.gugucraft.guguaddons.client.emi;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.util.ReflectionCache;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Method;

@EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
public final class EmiClientBakeHelper {
    private static final int BAKE_DELAY_TICKS = 2;
    private static final ReflectionCache.MethodRef EMI_IS_LOADED_METHOD = ReflectionCache.publicMethod(
            "dev.emi.emi.runtime.EmiReloadManager",
            "isLoaded");
    private static final ReflectionCache.MethodRef EMI_BAKE_METHOD = ReflectionCache.publicMethod(
            "dev.emi.emi.registry.EmiRecipes",
            "bake");

    private static boolean pendingBake;
    private static boolean bakeQueued;
    private static int pendingDelayTicks;
    private static String pendingReason = "client state change";

    private EmiClientBakeHelper() {
    }

    public static void requestRecipeBake(String reason) {
        if (!ModList.get().isLoaded("emi")) {
            return;
        }

        pendingBake = true;
        pendingDelayTicks = Math.max(pendingDelayTicks, BAKE_DELAY_TICKS);
        if (reason != null && !reason.isBlank()) {
            pendingReason = reason;
        }
        scheduleIfReady();
    }

    public static void cancelPendingBake() {
        pendingBake = false;
        pendingDelayTicks = 0;
        pendingReason = "client state change";
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (pendingBake) {
            scheduleIfReady();
        }
    }

    private static void scheduleIfReady() {
        if (!pendingBake || bakeQueued || !isClientWorldReady() || !isEmiLoaded()) {
            return;
        }

        if (pendingDelayTicks > 0) {
            pendingDelayTicks--;
            return;
        }

        pendingBake = false;
        bakeQueued = true;
        Minecraft.getInstance().execute(() -> {
            bakeQueued = false;
            if (!isClientWorldReady()) {
                pendingBake = true;
                return;
            }
            bake();
        });
    }

    private static boolean isClientWorldReady() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.player != null;
    }

    private static boolean isEmiLoaded() {
        try {
            ReflectionCache.MethodLookup lookup = EMI_IS_LOADED_METHOD.lookup();
            Method isLoadedMethod = lookup.method();
            if (isLoadedMethod == null) {
                if (lookup.reportFailure()) {
                    GuGuAddons.LOGGER.warn("Failed to check EMI reload state", lookup.failure());
                }
                return true;
            }
            return Boolean.TRUE.equals(isLoadedMethod.invoke(null));
        } catch (Throwable t) {
            GuGuAddons.LOGGER.warn("Failed to check EMI reload state", t);
            return true;
        }
    }

    private static void bake() {
        try {
            ReflectionCache.MethodLookup lookup = EMI_BAKE_METHOD.lookup();
            Method bakeMethod = lookup.method();
            if (bakeMethod == null) {
                if (lookup.reportFailure()) {
                    GuGuAddons.LOGGER.warn("Failed to rebake EMI recipes after {}", pendingReason,
                            lookup.failure());
                }
                return;
            }
            bakeMethod.invoke(null);
        } catch (Throwable t) {
            GuGuAddons.LOGGER.warn("Failed to rebake EMI recipes after {}", pendingReason, t);
        }
    }
}
