package com.gugucraft.guguaddons;

import com.gugucraft.guguaddons.registry.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.foundation.block.connected.*;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = GuGuAddons.MODID, dist = Dist.CLIENT)
public class GuGuAddonsClient {

        public static final CTSpriteShiftEntry DEDUCTION_CASING_CT = CTSpriteShifter.getCT(
                        AllCTTypes.OMNIDIRECTIONAL,
                        ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "block/deduction_casing"),
                        ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "block/deduction_casing_connected"));

        public static final CTSpriteShiftEntry QUEST_INTERFACE_CT = CTSpriteShifter.getCT(
                        AllCTTypes.OMNIDIRECTIONAL,
                        ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "block/quest_interface_front"),
                        ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID,
                                        "block/quest_interface_front_connected"));

        public static final CTSpriteShiftEntry QUEST_SUBMISSION_CT = CTSpriteShifter.getCT(
                        AllCTTypes.OMNIDIRECTIONAL,
                        ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, "block/quest_submission_front"),
                        ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID,
                                        "block/quest_submission_front_connected"));

        public GuGuAddonsClient(IEventBus modEventBus, ModContainer container) {
                modEventBus.addListener(GuGuAddonsClient::onClientSetup);
                modEventBus.addListener(GuGuAddonsClient::registerRenderers);

                // Allows NeoForge to create a config screen for this mod's configs.
                // The config screen is accessed by going to the Mods screen > clicking on your
                // mod > clicking on config.
                // Do not forget to add translations for your config options to the en_us.json
                // file.
                container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }

        static void onClientSetup(FMLClientSetupEvent event) {
                // Client setup code
                GuGuAddons.LOGGER.info("HELLO FROM CLIENT SETUP");
                GuGuAddons.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

                dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer
                                .builder(ModBlockEntities.QUEST_INPUT.get())
                                .factory(OrientedRotatingVisual.of(AllPartialModels.SHAFT_HALF))
                                .skipVanillaRender(be -> true)
                                .apply();

                // Register Connected Textures
                CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(ModBlocks.DEDUCTION_CASING.getId(),
                                model -> new CTModel(model, new EncasedCTBehaviour(DEDUCTION_CASING_CT)));
                CreateClient.CASING_CONNECTIVITY.makeCasing(ModBlocks.DEDUCTION_CASING.get(), DEDUCTION_CASING_CT);

                CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(ModBlocks.QUEST_INTERFACE_BLOCK.getId(),
                                model -> new CTModel(model, new EncasedCTBehaviour(DEDUCTION_CASING_CT) {
                                        @Override
                                        public CTSpriteShiftEntry getShift(
                                                        net.minecraft.world.level.block.state.BlockState state,
                                                        net.minecraft.core.Direction direction,
                                                        @org.jetbrains.annotations.Nullable net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
                                                return direction == state.getValue(
                                                                com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.FACING)
                                                                                ? QUEST_INTERFACE_CT
                                                                                : DEDUCTION_CASING_CT;
                                        }
                                }));
                CreateClient.CASING_CONNECTIVITY.makeCasing(ModBlocks.QUEST_INTERFACE_BLOCK.get(), DEDUCTION_CASING_CT);

                CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(ModBlocks.QUEST_SUBMISSION.getId(),
                                model -> new CTModel(model, new EncasedCTBehaviour(DEDUCTION_CASING_CT) {
                                        @Override
                                        public CTSpriteShiftEntry getShift(
                                                        net.minecraft.world.level.block.state.BlockState state,
                                                        net.minecraft.core.Direction direction,
                                                        @org.jetbrains.annotations.Nullable net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
                                                return direction == state.getValue(
                                                                com.gugucraft.guguaddons.block.custom.QuestSubmissionBlock.FACING)
                                                                                ? QUEST_SUBMISSION_CT
                                                                                : DEDUCTION_CASING_CT;
                                        }
                                }));
                CreateClient.CASING_CONNECTIVITY.makeCasing(ModBlocks.QUEST_SUBMISSION.get(), DEDUCTION_CASING_CT);
        }

        public static void registerRenderers(
                        net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
                event.registerBlockEntityRenderer(ModBlockEntities.QUEST_INPUT.get(),
                                com.gugucraft.guguaddons.client.renderer.QuestInputRenderer::new);
        }
}
