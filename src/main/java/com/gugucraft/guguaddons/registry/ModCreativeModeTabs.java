package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;
import net.minecraft.core.registries.Registries;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, GuGuAddons.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GUGUADDONS_TAB = CREATIVE_MODE_TABS
            .register("guguaddons_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guguaddons"))
                    .icon(() -> ModItems.GEM_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
