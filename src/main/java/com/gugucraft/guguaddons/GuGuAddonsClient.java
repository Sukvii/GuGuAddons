package com.gugucraft.guguaddons;

import com.gugucraft.guguaddons.registry.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.foundation.block.connected.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = GuGuAddons.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
public class GuGuAddonsClient {
    public GuGuAddonsClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Register QuestInputBlock connected textures
        event.enqueueWork(() -> {
            Block questInput = ModBlocks.QUEST_INPUT.get();
            // 1. Register Casing Connectivity
            CreateClient.CASING_CONNECTIVITY.make(questInput, AllSpriteShifts.ANDESITE_CASING);

            // 2. Register Connected Texture Behaviour (Model)
            ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(questInput);
            ConnectedTextureBehaviour behavior = new EncasedCTBehaviour(AllSpriteShifts.ANDESITE_CASING) {
                @Override
                public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
                    if (isBeingBlocked(state, reader, pos, otherPos, face)) return false;
                    if (state.getBlock() == other.getBlock()) return true;
                    
                    com.simibubi.create.content.decoration.encasing.CasingConnectivity.Entry otherEntry = CreateClient.CASING_CONNECTIVITY.get(other);
                    return otherEntry != null && otherEntry.getCasing() == AllSpriteShifts.ANDESITE_CASING;
                }
            };
            CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(rl, model -> new CTModel(model, behavior));

            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ModBlockEntities.QUEST_INPUT.get(), com.gugucraft.guguaddons.client.renderer.QuestInputRenderer::new);
        });

        // Some client setup code
        GuGuAddons.LOGGER.info("HELLO FROM CLIENT SETUP");
        GuGuAddons.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
