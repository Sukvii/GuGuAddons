package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.block.entity.AbyssCatalyticChamberBlockEntity;
import com.gugucraft.guguaddons.block.entity.AbyssCatalyticChamberMiddleBlockEntity;
import com.gugucraft.guguaddons.block.entity.AbyssCatalyticChamberTopBlockEntity;
import com.gugucraft.guguaddons.block.entity.CentrifugeBlockEntity;
import com.gugucraft.guguaddons.block.entity.CentrifugeStructuralBlockEntity;
import com.gugucraft.guguaddons.block.entity.MechanicalShriekerBlockEntity;
import com.gugucraft.guguaddons.block.entity.QuestInterfaceBlockEntity;
import com.gugucraft.guguaddons.block.entity.VacuumChamberBlockEntity;
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

    public static final Supplier<BlockEntityType<com.gugucraft.guguaddons.block.entity.QuestSubmissionBlockEntity>> QUEST_SUBMISSION =
            BLOCK_ENTITIES.register("quest_submission", () ->
                    BlockEntityType.Builder.of(com.gugucraft.guguaddons.block.entity.QuestSubmissionBlockEntity::new,
                            ModBlocks.QUEST_SUBMISSION.get()).build(null));

    public static final Supplier<BlockEntityType<VacuumChamberBlockEntity>> VACUUM_CHAMBER =
            BLOCK_ENTITIES.register("vacuum_chamber", () ->
                    BlockEntityType.Builder.of(VacuumChamberBlockEntity::new,
                            ModBlocks.VACUUM_CHAMBER.get()).build(null));

    public static final Supplier<BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE =
            BLOCK_ENTITIES.register("centrifuge", () ->
                    BlockEntityType.Builder.of(CentrifugeBlockEntity::new,
                            ModBlocks.CENTRIFUGE.get()).build(null));

    public static final Supplier<BlockEntityType<CentrifugeStructuralBlockEntity>> CENTRIFUGE_STRUCTURE =
            BLOCK_ENTITIES.register("centrifuge_structure", () ->
                    BlockEntityType.Builder.of(CentrifugeStructuralBlockEntity::new,
                            ModBlocks.CENTRIFUGE_STRUCTURE.get()).build(null));

    public static final Supplier<BlockEntityType<AbyssCatalyticChamberBlockEntity>> ABYSS_CATALYTIC_CHAMBER =
            BLOCK_ENTITIES.register("abyss_catalytic_chamber", () ->
                    BlockEntityType.Builder.of(AbyssCatalyticChamberBlockEntity::new,
                            ModBlocks.ABYSS_CATALYTIC_CHAMBER.get()).build(null));

    public static final Supplier<BlockEntityType<AbyssCatalyticChamberMiddleBlockEntity>> ABYSS_CATALYTIC_CHAMBER_MIDDLE =
            BLOCK_ENTITIES.register("abyss_catalytic_chamber_middle", () ->
                    BlockEntityType.Builder.of(AbyssCatalyticChamberMiddleBlockEntity::new,
                            ModBlocks.ABYSS_CATALYTIC_CHAMBER_MIDDLE.get()).build(null));

    public static final Supplier<BlockEntityType<AbyssCatalyticChamberTopBlockEntity>> ABYSS_CATALYTIC_CHAMBER_TOP =
            BLOCK_ENTITIES.register("abyss_catalytic_chamber_top", () ->
                    BlockEntityType.Builder.of(AbyssCatalyticChamberTopBlockEntity::new,
                            ModBlocks.ABYSS_CATALYTIC_CHAMBER_TOP.get()).build(null));

    public static final Supplier<BlockEntityType<MechanicalShriekerBlockEntity>> MECHANICAL_SHRIEKER =
            BLOCK_ENTITIES.register("mechanical_shrieker", () ->
                    BlockEntityType.Builder.of(MechanicalShriekerBlockEntity::new,
                            ModBlocks.MECHANICAL_SHRIEKER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
