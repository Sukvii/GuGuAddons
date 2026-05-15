package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.block.custom.AbyssCatalyticChamberBlock;
import com.gugucraft.guguaddons.block.custom.AbyssCatalyticChamberMiddleBlock;
import com.gugucraft.guguaddons.block.custom.AbyssCatalyticChamberTopBlock;
import com.gugucraft.guguaddons.block.custom.CentrifugeBlock;
import com.gugucraft.guguaddons.block.custom.CentrifugeStructuralBlock;
import com.gugucraft.guguaddons.block.custom.MechanicalShriekerBlock;
import com.gugucraft.guguaddons.block.custom.VacuumChamberBlock;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

import com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock;

public class ModBlocks {
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GuGuAddons.MODID);

        public static final DeferredBlock<Block> QUEST_INTERFACE_BLOCK = BLOCKS.register("quest_interface",
                        () -> new QuestInterfaceBlock(BlockBehaviour.Properties.of().strength(3.0F)
                                        .sound(SoundType.METAL).noOcclusion()));

        public static final DeferredBlock<Block> QUEST_INPUT = BLOCKS.register("quest_input",
                        () -> new com.gugucraft.guguaddons.block.custom.QuestInputBlock(BlockBehaviour.Properties.of()
                                        .strength(3.0F).sound(SoundType.METAL).noOcclusion()));

        public static final DeferredBlock<Block> QUEST_SUBMISSION = BLOCKS.register("quest_submission",
                        () -> new com.gugucraft.guguaddons.block.custom.QuestSubmissionBlock(BlockBehaviour.Properties
                                        .of().strength(3.0F).sound(SoundType.METAL).noOcclusion()));

        public static final DeferredBlock<Block> DEDUCTION_CASING = BLOCKS.register("deduction_casing",
                        () -> new com.gugucraft.guguaddons.block.custom.DeductionCasingBlock(
                                        BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL)));

        public static final DeferredBlock<Block> VACUUM_CHAMBER = BLOCKS.register("vacuum_chamber",
                        () -> new VacuumChamberBlock(BlockBehaviour.Properties.of().strength(3.0F)
                                        .sound(SoundType.METAL).noOcclusion()));

        public static final DeferredBlock<Block> CENTRIFUGE = BLOCKS.register("centrifuge",
                        () -> new CentrifugeBlock(BlockBehaviour.Properties.of().strength(3.0F)
                                        .sound(SoundType.METAL).noOcclusion()));

        public static final DeferredBlock<Block> CENTRIFUGE_STRUCTURE = BLOCKS.register("centrifuge_structure",
                        () -> new CentrifugeStructuralBlock(BlockBehaviour.Properties.of().strength(3.0F)
                                        .sound(SoundType.METAL).noOcclusion().noLootTable()));

        public static final DeferredBlock<Block> ABYSS_CATALYTIC_CHAMBER = BLOCKS.register(
                        "abyss_catalytic_chamber",
                        () -> new AbyssCatalyticChamberBlock(BlockBehaviour.Properties.of().strength(3.0F)
                                        .sound(SoundType.METAL).noOcclusion()));

        public static final DeferredBlock<Block> ABYSS_CATALYTIC_CHAMBER_MIDDLE = BLOCKS.register(
                        "abyss_catalytic_chamber_middle",
                        () -> new AbyssCatalyticChamberMiddleBlock(BlockBehaviour.Properties.of().strength(3.0F)
                                        .sound(SoundType.METAL).noOcclusion().noLootTable()));

        public static final DeferredBlock<Block> ABYSS_CATALYTIC_CHAMBER_TOP = BLOCKS.register(
                        "abyss_catalytic_chamber_top",
                        () -> new AbyssCatalyticChamberTopBlock(BlockBehaviour.Properties.of().strength(3.0F)
                                        .sound(SoundType.METAL).noOcclusion().noLootTable()));

        public static final DeferredBlock<Block> MECHANICAL_SHRIEKER = BLOCKS.register("mechanical_shrieker",
                        () -> new MechanicalShriekerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SCULK_SHRIEKER)
                                        .noOcclusion()));

        public static void register(IEventBus eventBus) {
                BLOCKS.register(eventBus);
        }
}
