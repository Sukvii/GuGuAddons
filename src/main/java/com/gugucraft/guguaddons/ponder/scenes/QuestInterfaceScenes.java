package com.gugucraft.guguaddons.ponder.scenes;

import com.gugucraft.guguaddons.block.custom.QuestInputBlock;
import com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock;
import com.gugucraft.guguaddons.block.custom.QuestSubmissionBlock;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class QuestInterfaceScenes {

        public static void buildStructure(SceneBuilder builder, SceneBuildingUtil util) {
                CreateSceneBuilder scene = new CreateSceneBuilder(builder);
                scene.title("quest_interface_structure", "Building the Quest Interface");
                scene.configureBasePlate(0, 0, 9); // Updated to 9 to clearly cover the 9x7 area
                scene.world().showSection(util.select().layer(0), Direction.UP);
                scene.idle(5);

                // Dimensions: 5 Wide (X), 4 High (Y), 3 Deep (Z)
                // New Base: 9 (X) x 7 (Z)
                // Center: X=4, Z=3
                // Structure Position:
                // X Range: 2 to 6 (Width 5)
                // Z Range: 2 to 4 (Depth 3)
                // Y Range: 1 to 4

                // Define BlockStates
                BlockState deductionCasing = ModBlocks.DEDUCTION_CASING.get().defaultBlockState();
                BlockState shaft = AllBlocks.SHAFT.get().defaultBlockState().setValue(BlockStateProperties.AXIS,
                                Direction.Axis.X);
                BlockState cogwheel = AllBlocks.COGWHEEL.get().defaultBlockState().setValue(BlockStateProperties.AXIS,
                                Direction.Axis.X);
                BlockState questInterface = ModBlocks.QUEST_INTERFACE_BLOCK.get().defaultBlockState()
                                .setValue(QuestInterfaceBlock.FACING, Direction.NORTH);

                // Connected Glass States
                BlockState glassBase = AllPaletteBlocks.FRAMED_GLASS_PANE.get().defaultBlockState();
                BlockState gMid = glassBase.setValue(BlockStateProperties.EAST, true)
                                .setValue(BlockStateProperties.WEST, true);
                BlockState gFrontLeft = glassBase.setValue(BlockStateProperties.EAST, true)
                                .setValue(BlockStateProperties.SOUTH, true);
                BlockState gFrontRight = glassBase.setValue(BlockStateProperties.WEST, true)
                                .setValue(BlockStateProperties.SOUTH, true);
                BlockState gBackLeft = glassBase.setValue(BlockStateProperties.EAST, true)
                                .setValue(BlockStateProperties.NORTH, true);
                BlockState gBackRight = glassBase.setValue(BlockStateProperties.WEST, true)
                                .setValue(BlockStateProperties.NORTH, true);

                // --- Step 1: Base Layer (Y=1) ---
                // Range: X:2-6, Y:1, Z:2-4
                scene.world().setBlocks(util.select().fromTo(2, 1, 2, 6, 1, 4), deductionCasing, false);
                scene.world().showSection(util.select().fromTo(2, 1, 2, 6, 1, 4), Direction.DOWN);

                scene.overlay().showText(60)
                                .text("quest_interface_structure.text_2") // "Start with a 5x3 base..."
                                .pointAt(util.vector().centerOf(4, 1, 2))
                                .placeNearTarget();
                scene.idle(70);

                // --- Step 2: Internals (Middle Layer Z=3) ---
                // Y=2 Internals: X=3,4,5 (Shaft, Cog, Shaft) @ Z=3
                scene.world().setBlock(util.grid().at(3, 2, 3), shaft, false);
                scene.world().setBlock(util.grid().at(4, 2, 3), cogwheel, false);
                scene.world().setBlock(util.grid().at(5, 2, 3), shaft, false);

                // Y=3 Internals: X=3,4,5 (Shaft, Cog, Shaft) @ Z=3
                scene.world().setBlock(util.grid().at(3, 3, 3), shaft, false);
                scene.world().setBlock(util.grid().at(4, 3, 3), cogwheel, false);
                scene.world().setBlock(util.grid().at(5, 3, 3), shaft, false);

                // Show internals
                scene.world().showSection(util.select().fromTo(3, 2, 3, 5, 3, 3), Direction.DOWN);

                scene.overlay().showText(60)
                                .text("quest_interface_structure.text_3") // "Build the internal kinetic components..."
                                .pointAt(util.vector().centerOf(4, 2, 3))
                                .placeNearTarget();
                scene.idle(70);

                // --- Step 3: Support Columns (Middle Layer Z=3 Sides) ---
                // Left Side: X=2, Z=3
                // Right Side: X=6, Z=3
                scene.world().setBlocks(util.select().fromTo(2, 2, 3, 2, 3, 3), deductionCasing, false);
                scene.world().setBlocks(util.select().fromTo(6, 2, 3, 6, 3, 3), deductionCasing, false);
                scene.world().showSection(util.select().fromTo(2, 2, 3, 2, 3, 3), Direction.DOWN);
                scene.world().showSection(util.select().fromTo(6, 2, 3, 6, 3, 3), Direction.DOWN);
                scene.idle(10);

                // --- Step 4: Front & Back Faces (Z=2 and Z=4) ---

                // Front Wall (Z=2)
                // Y=2
                scene.world().setBlock(util.grid().at(2, 2, 2), gFrontLeft, false);
                scene.world().setBlock(util.grid().at(3, 2, 2), gMid, false);
                scene.world().setBlock(util.grid().at(4, 2, 2), questInterface, false); // Center (4, 2, 2)
                scene.world().setBlock(util.grid().at(5, 2, 2), gMid, false);
                scene.world().setBlock(util.grid().at(6, 2, 2), gFrontRight, false);

                // Y=3
                scene.world().setBlock(util.grid().at(2, 3, 2), gFrontLeft, false);
                scene.world().setBlocks(util.select().fromTo(3, 3, 2, 5, 3, 2), gMid, false);
                scene.world().setBlock(util.grid().at(6, 3, 2), gFrontRight, false);

                // Back Wall (Z=4)
                // Y=2
                scene.world().setBlock(util.grid().at(2, 2, 4), gBackLeft, false);
                scene.world().setBlocks(util.select().fromTo(3, 2, 4, 5, 2, 4), gMid, false);
                scene.world().setBlock(util.grid().at(6, 2, 4), gBackRight, false);

                // Y=3
                scene.world().setBlock(util.grid().at(2, 3, 4), gBackLeft, false);
                scene.world().setBlocks(util.select().fromTo(3, 3, 4, 5, 3, 4), gMid, false);
                scene.world().setBlock(util.grid().at(6, 3, 4), gBackRight, false);

                // Show Front and Back Layers
                scene.world().showSection(util.select().fromTo(2, 2, 2, 6, 3, 2), Direction.DOWN); // Front Z=2
                scene.world().showSection(util.select().fromTo(2, 2, 4, 6, 3, 4), Direction.DOWN); // Back Z=4

                scene.idle(20);

                // Highlight Quest Interface
                scene.overlay().showText(40)
                                .text("quest_interface_structure.text_1") // "The Quest Interface..."
                                .pointAt(util.vector().centerOf(4, 2, 2))
                                .placeNearTarget()
                                .colored(PonderPalette.GREEN);
                scene.idle(50);

                // --- Step 5: Top Layer (Y=4) ---
                scene.world().setBlocks(util.select().fromTo(2, 4, 2, 6, 4, 4), deductionCasing, false);
                scene.world().showSection(util.select().fromTo(2, 4, 2, 6, 4, 4), Direction.DOWN);
                scene.idle(20);

                // --- Step 6: Final Check ---
                scene.effects().indicateSuccess(util.grid().at(4, 2, 2));
                scene.idle(20);

                // Speed requirement
                scene.overlay().showText(60)
                                .text("quest_interface_structure.text_4") // "Requires... 256 RPM"
                                .pointAt(util.vector().centerOf(4, 2, 2))
                                .placeNearTarget()
                                .colored(PonderPalette.FAST);
                scene.idle(70);

                // --- Step 7: Interface Replacements ---
                scene.overlay().showText(60)
                                .text("quest_interface_structure.text_5") // "You can replace any Deduction Casing..."
                                .pointAt(util.vector().centerOf(6, 2, 3)) // Point at Right Side Casing (6, 2, 3)
                                .placeNearTarget()
                                .colored(PonderPalette.WHITE);
                scene.idle(70);

                // Define Interface Blocks with CORRECT rotation
                BlockState questInput = ModBlocks.QUEST_INPUT.get().defaultBlockState()
                                .setValue(QuestInputBlock.FACING, Direction.EAST);

                BlockState questSubmission = ModBlocks.QUEST_SUBMISSION.get().defaultBlockState()
                                .setValue(QuestSubmissionBlock.FACING, Direction.WEST);

                // Rotate to show the Right (East) side for Kinetic Input
                scene.rotateCameraY(110);
                scene.idle(20);

                // 1. Show Kinetic Input (Right Side X=6)
                scene.world().hideSection(util.select().position(6, 2, 3), Direction.EAST);
                scene.idle(5);
                scene.world().setBlock(util.grid().at(6, 2, 3), questInput, false);
                scene.world().showSection(util.select().position(6, 2, 3), Direction.WEST);

                scene.overlay().showText(60)
                                .text("quest_interface_structure.text_6") // "Use a Kinetic Input Interface..."
                                .pointAt(util.vector().centerOf(6, 2, 3))
                                .placeNearTarget()
                                .colored(PonderPalette.BLUE);
                scene.idle(70);

                // Add Stress Source (Motor + Shafts) feeding into QuestInput (X=6)
                // Input at 6, Shaft at 7, Motor at 8. Z is 3.
                BlockPos motorPos = util.grid().at(8, 2, 3);
                BlockPos shaftPos = util.grid().at(7, 2, 3);

                scene.world().setBlock(shaftPos, AllBlocks.SHAFT.get().defaultBlockState()
                                .setValue(BlockStateProperties.AXIS, Direction.Axis.X), false);
                scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                                .setValue(BlockStateProperties.FACING, Direction.WEST), false);

                scene.world().showSection(util.select().fromTo(7, 2, 3, 8, 2, 3), Direction.WEST);
                scene.idle(10);

                scene.effects().rotationSpeedIndicator(motorPos);
                scene.world().setKineticSpeed(util.select().fromTo(7, 2, 3, 8, 2, 3), 256);
                scene.world().setKineticSpeed(util.select().position(6, 2, 3), 256); // Speed up the input block
                scene.world().setKineticSpeed(util.select().fromTo(3, 2, 3, 5, 2, 3), 256); // Speed up internal cogs
                                                                                            // i_s (X=3,4,5)
                                                                                            // i_s (X=3,4,5)
                scene.world().setKineticSpeed(util.select().fromTo(3, 3, 3, 5, 3, 3), -256); // Speed up upper internal
                                                                                             // cogs
                scene.idle(60);

                // Rotate back to original view for Submission Interface
                scene.rotateCameraY(-110);
                scene.idle(20);

                // 2. Show Submission Interface (Left Side X=2)
                // Original X=0 -> New X=2
                scene.world().hideSection(util.select().position(2, 2, 3), Direction.WEST);
                scene.idle(5);
                scene.world().setBlock(util.grid().at(2, 2, 3), questSubmission, false);
                scene.world().showSection(util.select().position(2, 2, 3), Direction.EAST);

                scene.overlay().showText(60)
                                .text("quest_interface_structure.text_7") // "...and a Quest Submission Interface..."
                                .pointAt(util.vector().centerOf(2, 2, 3))
                                .placeNearTarget()
                                .colored(PonderPalette.BLUE);
                scene.idle(70);

                // Add Automation (Belt + Funnel) feeding into QuestSubmission (X=2)
                // Move closer to ensure connection:
                // Interface at X=2. Funnel needs to be at X=1.
                BlockPos beltStart = util.grid().at(1, 1, 3);
                BlockPos funnelPos = util.grid().at(1, 2, 3);

                // Setup Depot
                scene.world().setBlock(beltStart, AllBlocks.DEPOT.get().defaultBlockState(), false);
                scene.world().showSection(util.select().position(beltStart), Direction.EAST);

                // Brass Funnel on the Submission Interface
                // Funnel at X=1, Interface at X=2. Funnel faces WEST (attached to West face of
                // Interface).
                BlockState funnel = AllBlocks.BRASS_FUNNEL.get().defaultBlockState()
                                .setValue(BlockStateProperties.FACING, Direction.WEST);
                scene.world().setBlock(funnelPos, funnel, false);
                scene.world().showSection(util.select().position(funnelPos), Direction.DOWN);

                // Simulate Item Processing
                // Spawn Item on Depot
                ElementLink<EntityElement> item = scene.world().createItemEntity(
                                util.vector().centerOf(beltStart).add(0, 0.5, 0),
                                util.vector().of(0, 0, 0),
                                new ItemStack(Items.APPLE));

                scene.idle(20);

                // "Consume" item (remove it) and flap funnel
                scene.world().modifyEntity(item, Entity::discard);
                scene.effects().indicateSuccess(funnelPos);

                scene.idle(20);

                // Limit warning
                scene.overlay().showText(80)
                                .text("quest_interface_structure.text_8") // "Only one of each..."
                                .pointAt(util.vector().centerOf(4, 2, 2)) // Center of structure front
                                .placeNearTarget()
                                .colored(PonderPalette.RED);
                scene.idle(60);
        }
}
