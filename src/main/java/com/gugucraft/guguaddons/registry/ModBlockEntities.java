package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.block.entity.QuestInterfaceBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, GuGuAddons.MODID);

    public static final Supplier<BlockEntityType<QuestInterfaceBlockEntity>> QUEST_INTERFACE =
            BLOCK_ENTITIES.register("quest_interface", () ->
                    BlockEntityType.Builder.of(QuestInterfaceBlockEntity::new,
                            ModBlocks.QUEST_INTERFACE_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<com.gugucraft.guguaddons.block.entity.QuestInputBlockEntity>> QUEST_INPUT =
            BLOCK_ENTITIES.register("quest_input", () ->
                    BlockEntityType.Builder.of(com.gugucraft.guguaddons.block.entity.QuestInputBlockEntity::new,
                            ModBlocks.QUEST_INPUT.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
