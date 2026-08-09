package com.gugucraft.guguaddons.client.compat.astages;

import com.alessandro.astages.api.event.update.ClientRecipeUpdateEvent;
import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.client.emi.EmiClientBakeHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
public final class AStagesRecipeClientEvents {
    private AStagesRecipeClientEvents() {
    }

    @SubscribeEvent
    public static void onRecipeRestrictionsChanged(ClientRecipeUpdateEvent event) {
        EmiClientBakeHelper.requestRecipeBake("AStages recipe restriction update");
    }
}
