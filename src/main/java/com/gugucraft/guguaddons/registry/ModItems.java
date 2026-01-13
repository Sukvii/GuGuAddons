package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.world.item.BlockItem;

public class ModItems {
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GuGuAddons.MODID);

        public static final DeferredItem<Item> QUEST_INTERFACE_ITEM = ITEMS.register("quest_interface",
                        () -> new BlockItem(ModBlocks.QUEST_INTERFACE_BLOCK.get(), new Item.Properties()));

        public static final DeferredItem<Item> QUEST_INPUT_ITEM = ITEMS.register("quest_input",
                        () -> new BlockItem(ModBlocks.QUEST_INPUT.get(), new Item.Properties()));

        public static final DeferredItem<Item> QUEST_SUBMISSION_ITEM = ITEMS.register("quest_submission",
                        () -> new com.gugucraft.guguaddons.item.QuestSubmissionBlockItem(
                                        ModBlocks.QUEST_SUBMISSION.get(), new Item.Properties()));

        public static final DeferredItem<Item> DEDUCTION_CASING_ITEM = ITEMS.register("deduction_casing",
                        () -> new BlockItem(ModBlocks.DEDUCTION_CASING.get(), new Item.Properties()));

        public static final DeferredItem<Item> TAB_ICON = ITEMS.register("tab_icon",
                        () -> new Item(new Item.Properties()));

        public static final DeferredItem<Item> QUEST_GATE_UPGRADE_1 = ITEMS.register("quest_gate_upgrade_1",
                        () -> new Item(new Item.Properties()));

        public static final DeferredItem<Item> QUEST_GATE_UPGRADE_2 = ITEMS.register("quest_gate_upgrade_2",
                        () -> new Item(new Item.Properties()));

        public static final DeferredItem<Item> QUEST_GATE_UPGRADE_3 = ITEMS.register("quest_gate_upgrade_3",
                        () -> new Item(new Item.Properties()));

        public static void register(IEventBus eventBus) {
                ITEMS.register(eventBus);
        }
}
