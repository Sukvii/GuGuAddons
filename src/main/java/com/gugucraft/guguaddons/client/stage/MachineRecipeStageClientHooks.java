package com.gugucraft.guguaddons.client.stage;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.client.emi.EmiClientReloadHelper;
import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
public final class MachineRecipeStageClientHooks {
    private MachineRecipeStageClientHooks() {
    }

    public static void onRestrictionSnapshotChanged() {
        EmiClientReloadHelper.requestRecipeReload("machine recipe stage restriction sync");
    }

    public static void onAStagesChanged() {
        EmiClientReloadHelper.requestRecipeReload("AStages sync");
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        MachineRecipeStageManager.clearClient();
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MachineRecipeStageManager.clearClient();
        EmiClientReloadHelper.cancelPendingReload();
    }
}
