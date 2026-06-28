package com.gugucraft.guguaddons.event;

import java.util.List;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.compat.create.TableClothItemHandler;
import com.gugucraft.guguaddons.compat.create.TableClothUnpackingHandler;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlock;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerTableCloths(event);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.QUEST_INTERFACE.get(),
                (blockEntity, context) -> blockEntity.getItemHandler());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.QUEST_SUBMISSION.get(),
                (blockEntity, context) -> blockEntity.getItemHandler());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CENTRIFUGE.get(),
                (blockEntity, context) -> blockEntity.getItemCapability());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CENTRIFUGE.get(),
                (blockEntity, context) -> blockEntity.getFluidCapability());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CENTRIFUGE_STRUCTURE.get(),
                (blockEntity, context) -> blockEntity.getItemCapability());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CENTRIFUGE_STRUCTURE.get(),
                (blockEntity, context) -> blockEntity.getFluidCapability());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.VACUUM_CHAMBER.get(),
                (blockEntity, context) -> blockEntity.getFluidCapability());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ABYSS_CATALYTIC_CHAMBER.get(),
                (blockEntity, context) -> blockEntity.getItemCapability());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.ABYSS_CATALYTIC_CHAMBER.get(),
                (blockEntity, context) -> blockEntity.getFluidCapability());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ABYSS_CATALYTIC_CHAMBER_MIDDLE.get(),
                (blockEntity, context) -> blockEntity.getItemCapability());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.ABYSS_CATALYTIC_CHAMBER_MIDDLE.get(),
                (blockEntity, context) -> blockEntity.getFluidCapability());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ABYSS_CATALYTIC_CHAMBER_TOP.get(),
                (blockEntity, context) -> blockEntity.getItemCapability());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.ABYSS_CATALYTIC_CHAMBER_TOP.get(),
                (blockEntity, context) -> blockEntity.getFluidCapability());

        // Register table cloth unpacking handler
        registerTableClothUnpackingHandlers();
    }

    /**
     * Give every Create table cloth (create:*_table_cloth -- 16 dyed + andesite /
     * brass / copper variants) an item-handler capability, so hoppers and Create
     * logistics can stock and drain the cloth's display items. Create ships no such
     * capability, so without this nothing but manual right-click placement works.
     *
     * <p>Registered at the BLOCK level (not the block-entity level) on purpose: an
     * empty cloth has no block entity yet, and a block-level provider still answers
     * the capability query, letting {@link TableClothItemHandler} force the block
     * entity into existence on the first insert. Variants are collected from the
     * block registry so future cloth additions are covered automatically.
     */
    private static void registerTableCloths(RegisterCapabilitiesEvent event) {
        List<Block> cloths = BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof TableClothBlock)
                .toList();
        if (cloths.isEmpty()) {
            GuGuAddons.LOGGER.warn("No create:table_cloth blocks found; skipping table-cloth item capability.");
            return;
        }
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, context) -> new TableClothItemHandler(level, pos),
                cloths.toArray(new Block[0]));
    }

    /**
     * Register custom unpacking handler for all table cloth blocks to ensure
     * packages are safely rejected when they exceed the cloth's 4-item capacity.
     */
    private static void registerTableClothUnpackingHandlers() {
        List<Block> cloths = BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof TableClothBlock)
                .toList();

        if (cloths.isEmpty()) {
            GuGuAddons.LOGGER.warn("No table cloth blocks found for unpacking handler registration.");
            return;
        }

        for (Block cloth : cloths) {
            com.simibubi.create.api.packager.unpacking.UnpackingHandler.REGISTRY.register(
                    cloth,
                    TableClothUnpackingHandler.INSTANCE
            );
        }

        GuGuAddons.LOGGER.info("Registered table cloth unpacking handler for {} cloth variants.", cloths.size());
    }
}
