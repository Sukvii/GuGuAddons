package com.gugucraft.guguaddons;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

import net.neoforged.neoforge.event.server.ServerStartingEvent;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.gugucraft.guguaddons.registry.ModCreativeModeTabs;
import com.gugucraft.guguaddons.registry.ModItems;
import com.gugucraft.guguaddons.registry.ModDimensions;

import com.gugucraft.guguaddons.registry.ModChunkGenerators;

@Mod(GuGuAddons.MODID)
public class GuGuAddons {

    public static final String MODID = "guguaddons";

    public static final Logger LOGGER = LogUtils.getLogger();

    public GuGuAddons(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        // ModCreativeModeTabs.register(modEventBus); // Postponed to next version

        // ModDimensions.register(); // Postponed to next version

        // ModChunkGenerators.register(modEventBus); // Postponed to next version

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {


        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

        LOGGER.info("HELLO from server starting");
    }
}
