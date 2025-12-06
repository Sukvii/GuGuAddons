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

        public static final DeferredItem<Item> GEM_ITEM = ITEMS.register("gem",
                        () -> new GemItem(new Item.Properties()));

        public static final DeferredItem<Item> GEM_BLANK_ITEM = ITEMS.register("gem_blank",
                        () -> new GemBlankItem(new Item.Properties().stacksTo(1)));

        public static final DeferredItem<Item> TEST_PORTAL_FRAME_ITEM = ITEMS.register("test_portal_frame",
                        () -> new BlockItem(ModBlocks.TEST_PORTAL_FRAME.get(), new Item.Properties()));

        public static void register(IEventBus eventBus) {
                ITEMS.register(eventBus);
        }
}
