package com.gugucraft.guguaddons.event;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.block.entity.QuestInterfaceBlockEntity;
import com.gugucraft.guguaddons.block.entity.QuestSubmissionBlockEntity;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
// EventBusSubscriber removed, registered manually in GuGuAddons.java
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.QUEST_INTERFACE.get(),
                (blockEntity, context) -> blockEntity.getItemHandler()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.QUEST_SUBMISSION.get(),
                (blockEntity, context) -> blockEntity.getItemHandler()
        );
    }
}
