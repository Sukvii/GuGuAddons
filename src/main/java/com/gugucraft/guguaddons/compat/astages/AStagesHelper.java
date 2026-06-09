package com.gugucraft.guguaddons.compat.astages;

import com.alessandro.astages.api.event.sync.ClientSynchronizeServerStagesEvent;
import com.alessandro.astages.api.event.sync.ClientSynchronizeStagesEvent;
import com.alessandro.astages.api.holder.AClientHolder;
import com.alessandro.astages.api.holder.AHolder;
import com.alessandro.astages.api.util.AStagesClientUtils;
import com.alessandro.astages.api.util.AStagesUtils;
import com.alessandro.astages.engine.ARestrictionManager;
import com.alessandro.astages.engine.server.restriction.item.ABaseItemRestriction;
import com.alessandro.astages.engine.store.Attributes;
import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.util.ReflectionCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AStagesHelper {
    private AStagesHelper() {
    }

    public static boolean hasStage(ServerPlayer player, String stage) {
        if (player == null || isBlank(stage)) {
            return false;
        }
        return AStagesUtils.hasStage(AHolder.serverAndPlayer(player), stage);
    }

    public static boolean hasStage(UUID playerId, String stage) {
        if (playerId == null || isBlank(stage)) {
            return false;
        }
        return AStagesUtils.hasStage(AHolder.player(playerId), stage)
                || AStagesUtils.hasStage(AHolder.server(), stage);
    }

    public static boolean hasServerStage(String stage) {
        if (isBlank(stage)) {
            return false;
        }
        return AStagesUtils.hasStage(AHolder.server(), stage);
    }

    public static boolean clientHasStage(String stage) {
        if (isBlank(stage)) {
            return false;
        }
        return AStagesClientUtils.hasStage(AClientHolder.serverAndPlayer(), stage);
    }

    public static boolean isUnknownInventoryItem(ServerPlayer player, ItemStack stack) {
        ABaseItemRestriction<?, ?> restriction = getInventoryRestriction(player, stack);
        return restriction != null && restriction.isDisabled(Attributes.STORING_IN_INVENTORY);
    }

    public static boolean isUnknownEquipmentItem(ServerPlayer player, ItemStack stack) {
        ABaseItemRestriction<?, ?> restriction = getEquipmentRestriction(player, stack);
        return restriction != null && restriction.isDisabled(Attributes.EQUIPPING);
    }

    public static boolean isStillUnknownItem(ServerPlayer player, ItemStack stack) {
        return getItemRestriction(player, stack) != null;
    }

    private static ABaseItemRestriction<?, ?> getInventoryRestriction(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return null;
        }
        return ARestrictionManager.ITEM_INSTANCE.getInventoryRestriction(AHolder.serverAndPlayer(player), stack);
    }

    private static ABaseItemRestriction<?, ?> getEquipmentRestriction(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return null;
        }
        return ARestrictionManager.ITEM_INSTANCE.getEquipmentRestriction(AHolder.serverAndPlayer(player), stack);
    }

    private static ABaseItemRestriction<?, ?> getItemRestriction(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return null;
        }
        return ARestrictionManager.ITEM_INSTANCE.getRestriction(AHolder.serverAndPlayer(player), stack);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private static final ReflectionCache.MethodRef ASTAGES_CHANGED_METHOD = ReflectionCache.publicMethod(
                "com.gugucraft.guguaddons.client.stage.MachineRecipeStageClientHooks",
                "onAStagesChanged");
        // jeiFinishedReload() fires AStages JEI recipe updates; TMRV's JEI stub rejects those after it locks.
        private static final String ASTAGES_RELOAD_STATE_CLASS =
                "com.alessandro.astages.engine.client.ClientRestrictionReloadState";
        private static final String DID_JEI_FINISH_RELOADING_FIELD = "didJeiFinishReloading";
        private static final AtomicBoolean TMRV_BRIDGE_FAILURE_REPORTED = new AtomicBoolean();
        private static final ThreadLocal<Boolean> TMRV_TOOLTIP_BRIDGE_OPEN = ThreadLocal.withInitial(() -> false);
        private static volatile Field didJeiFinishReloadingField;

        private ClientEvents() {
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void beforePlayerStagesSynced(ClientSynchronizeStagesEvent event) {
            closeTmrvTooltipBridge();
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void beforeServerStagesSynced(ClientSynchronizeServerStagesEvent event) {
            closeTmrvTooltipBridge();
        }

        @SubscribeEvent
        public static void onPlayerStagesSynced(ClientSynchronizeStagesEvent event) {
            notifyClientStageChanged();
        }

        @SubscribeEvent
        public static void onServerStagesSynced(ClientSynchronizeServerStagesEvent event) {
            notifyClientStageChanged();
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void beforeItemTooltip(ItemTooltipEvent event) {
            openTmrvTooltipBridge(event);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void afterItemTooltip(ItemTooltipEvent event) {
            closeTmrvTooltipBridge();
        }

        private static void notifyClientStageChanged() {
            try {
                ReflectionCache.MethodLookup lookup = ASTAGES_CHANGED_METHOD.lookup();
                Method method = lookup.method();
                if (method == null) {
                    if (lookup.reportFailure()) {
                        GuGuAddons.LOGGER.warn("Failed to refresh client recipe stages after AStages sync",
                                lookup.failure());
                    }
                    return;
                }
                method.invoke(null);
            } catch (Throwable t) {
                GuGuAddons.LOGGER.warn("Failed to refresh client recipe stages after AStages sync", t);
            }
        }

        private static void openTmrvTooltipBridge(ItemTooltipEvent event) {
            if (event.getEntity() == null || !ModList.get().isLoaded("toomanyrecipeviewers")) {
                return;
            }

            if (setTmrvJeiReloadFinished(true)) {
                TMRV_TOOLTIP_BRIDGE_OPEN.set(true);
            }
        }

        private static void closeTmrvTooltipBridge() {
            if (!ModList.get().isLoaded("toomanyrecipeviewers")) {
                TMRV_TOOLTIP_BRIDGE_OPEN.remove();
                return;
            }

            if (Boolean.TRUE.equals(TMRV_TOOLTIP_BRIDGE_OPEN.get())) {
                TMRV_TOOLTIP_BRIDGE_OPEN.remove();
            }
            setTmrvJeiReloadFinished(false);
        }

        private static boolean setTmrvJeiReloadFinished(boolean value) {
            try {
                getDidJeiFinishReloadingField().setBoolean(null, value);
                return true;
            } catch (Throwable t) {
                reportTmrvBridgeFailure(t);
                return false;
            }
        }

        private static Field getDidJeiFinishReloadingField() throws ReflectiveOperationException {
            Field field = didJeiFinishReloadingField;
            if (field != null) {
                return field;
            }

            synchronized (ClientEvents.class) {
                field = didJeiFinishReloadingField;
                if (field == null) {
                    field = Class.forName(ASTAGES_RELOAD_STATE_CLASS).getDeclaredField(DID_JEI_FINISH_RELOADING_FIELD);
                    field.setAccessible(true);
                    didJeiFinishReloadingField = field;
                }
                return field;
            }
        }

        private static void reportTmrvBridgeFailure(Throwable t) {
            if (TMRV_BRIDGE_FAILURE_REPORTED.compareAndSet(false, true)) {
                GuGuAddons.LOGGER.warn("Failed to bridge AStages JEI reload state for TMRV", t);
            }
        }
    }
}
