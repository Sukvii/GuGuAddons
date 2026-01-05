package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;

import com.gugucraft.guguaddons.block.custom.DimensionalTeleportBlock;
import com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GuGuAddons.MODID);

    public static final DeferredBlock<Block> DIMENSIONAL_TELEPORT_BLOCK = BLOCKS.register("dimensional_teleport_block",
            () -> new DimensionalTeleportBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL)));

    public static final DeferredBlock<Block> QUEST_INTERFACE_BLOCK = BLOCKS.register("quest_interface",
            () -> new QuestInterfaceBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<Block> QUEST_INPUT = BLOCKS.register("quest_input",
            () -> new com.gugucraft.guguaddons.block.custom.QuestInputBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<Block> QUEST_SUBMISSION = BLOCKS.register("quest_submission",
            () -> new com.gugucraft.guguaddons.block.custom.QuestSubmissionBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<Block> DEDUCTION_CASING = BLOCKS.register("deduction_casing",
            () -> new com.gugucraft.guguaddons.block.custom.DeductionCasingBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
