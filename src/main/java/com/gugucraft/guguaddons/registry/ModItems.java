package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.gugucraft.guguaddons.item.GemItem;
import com.gugucraft.guguaddons.item.GemBlankItem;

import net.minecraft.world.item.BlockItem;

public class ModItems {
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GuGuAddons.MODID);

        /*
        public static final DeferredItem<Item> GEM_ITEM = ITEMS.register("gem",
                        () -> new GemItem(new Item.Properties()));

        public static final DeferredItem<Item> GEM_BLANK_ITEM = ITEMS.register("gem_blank",
                        () -> new GemBlankItem(new Item.Properties().stacksTo(1)));
        */

        /*
        public static final DeferredItem<Item> DIMENSIONAL_TELEPORT_BLOCK_ITEM = ITEMS.register(
                        "dimensional_teleport_block",
                        () -> new BlockItem(ModBlocks.DIMENSIONAL_TELEPORT_BLOCK.get(), new Item.Properties()));
        */
        public static final DeferredItem<Item> QUEST_INTERFACE_ITEM = ITEMS.register("quest_interface",
                () -> new BlockItem(ModBlocks.QUEST_INTERFACE_BLOCK.get(), new Item.Properties()));

        public static final DeferredItem<Item> QUEST_INPUT_ITEM = ITEMS.register("quest_input",
                () -> new BlockItem(ModBlocks.QUEST_INPUT.get(), new Item.Properties()));

        public static final DeferredItem<Item> TAB_ICON = ITEMS.register("tab_icon",
                () -> new Item(new Item.Properties()));

        public static void register(IEventBus eventBus) {
                ITEMS.register(eventBus);
        }
}
