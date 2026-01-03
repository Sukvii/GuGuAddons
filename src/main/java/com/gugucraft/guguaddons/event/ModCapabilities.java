package com.gugucraft.guguaddons.event;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = GuGuAddons.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.QUEST_INTERFACE.get(),
                (blockEntity, context) -> blockEntity.getItemHandler()
        );
    }
}
