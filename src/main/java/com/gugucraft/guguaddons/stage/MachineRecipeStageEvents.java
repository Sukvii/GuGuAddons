package com.gugucraft.guguaddons.stage;

import com.gugucraft.guguaddons.GuGuAddons;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID)
public final class MachineRecipeStageEvents {
    private MachineRecipeStageEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MachineRecipeStageManager.reloadFromKubeJS();
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            MachineRecipeStageManager.reloadFromKubeJS();
        }
    }
}
