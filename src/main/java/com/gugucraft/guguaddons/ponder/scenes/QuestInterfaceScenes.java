package com.gugucraft.guguaddons.ponder.scenes;

import com.gugucraft.guguaddons.block.custom.QuestInputBlock;
import com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock;
import com.gugucraft.guguaddons.block.custom.QuestSubmissionBlock;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class QuestInterfaceScenes {

    public static void buildStructure(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("quest_interface_structure", "Building the Quest Interface");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);

        // Dimensions: 5 Wide (X), 4 High (Y), 3 Deep (Z)
        // Center X=2. Z=1 to Z=3.

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

        // Horizontal connections (East-West)
        BlockState gMid = glassBase.setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.WEST, true);

        // Front Corners (Z=1, connecting South to Casing at Z=2)
        BlockState gFrontLeft = glassBase.setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.SOUTH,
                true);
        BlockState gFrontRight = glassBase.setValue(BlockStateProperties.WEST, true)
                .setValue(BlockStateProperties.SOUTH, true);

        // Back Corners (Z=3, connecting North to Casing at Z=2)
        BlockState gBackLeft = glassBase.setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.NORTH,
                true);
        BlockState gBackRight = glassBase.setValue(BlockStateProperties.WEST, true).setValue(BlockStateProperties.NORTH,
                true);

        // --- Step 1: Base Layer (Y=1) ---
        // Range: X:0-4, Y:1, Z:1-3
        // All Deduction Casing
        scene.world().setBlocks(util.select().fromTo(0, 1, 1, 4, 1, 3), deductionCasing, false);
        scene.world().showSection(util.select().fromTo(0, 1, 1, 4, 1, 3), Direction.DOWN);

        scene.overlay().showText(60)
                .text("quest_interface_structure.text_2") // "Start with a 5x3 base..."
                .pointAt(util.vector().centerOf(2, 1, 1))
                .placeNearTarget();
        scene.idle(70);

        // --- Step 2: Internals (Middle Layer Z=2) ---
        // Y=2 Internals: X=1..3 (Shaft, Cog, Shaft)
        scene.world().setBlock(util.grid().at(1, 2, 2), shaft, false);
        scene.world().setBlock(util.grid().at(2, 2, 2), cogwheel, false);
        scene.world().setBlock(util.grid().at(3, 2, 2), shaft, false);

        // Y=3 Internals: X=1..3 (Shaft, Cog, Shaft)
        scene.world().setBlock(util.grid().at(1, 3, 2), shaft, false);
        scene.world().setBlock(util.grid().at(2, 3, 2), cogwheel, false);
        scene.world().setBlock(util.grid().at(3, 3, 2), shaft, false);

        // Show internals
        scene.world().showSection(util.select().fromTo(1, 2, 2, 3, 3, 2), Direction.DOWN);

        scene.overlay().showText(60)
                .text("quest_interface_structure.text_3") // "Build the internal kinetic components..."
                .pointAt(util.vector().centerOf(2, 2, 2))
                .placeNearTarget();
        scene.idle(70);

        // --- Step 3: Support Columns (Middle Layer Z=2 Sides) ---
        // The sides of the middle layer are Casings.
        // X=0, Z=2, Y=2..3 and X=4, Z=2, Y=2..3
        scene.world().setBlocks(util.select().fromTo(0, 2, 2, 0, 3, 2), deductionCasing, false);
        scene.world().setBlocks(util.select().fromTo(4, 2, 2, 4, 3, 2), deductionCasing, false);
        scene.world().showSection(util.select().fromTo(0, 2, 2, 0, 3, 2), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(4, 2, 2, 4, 3, 2), Direction.DOWN);
        scene.idle(10);

        // --- Step 4: Front & Back Faces (Z=1 and Z=3) ---

        // Front Wall (Z=1)
        // Y=2
        scene.world().setBlock(util.grid().at(0, 2, 1), gFrontLeft, false);
        scene.world().setBlock(util.grid().at(1, 2, 1), gMid, false);
        scene.world().setBlock(util.grid().at(2, 2, 1), questInterface, false); // Center
        scene.world().setBlock(util.grid().at(3, 2, 1), gMid, false);
        scene.world().setBlock(util.grid().at(4, 2, 1), gFrontRight, false);

        // Y=3
        scene.world().setBlock(util.grid().at(0, 3, 1), gFrontLeft, false);
        scene.world().setBlocks(util.select().fromTo(1, 3, 1, 3, 3, 1), gMid, false);
        scene.world().setBlock(util.grid().at(4, 3, 1), gFrontRight, false);

        // Back Wall (Z=3)
        // Y=2
        scene.world().setBlock(util.grid().at(0, 2, 3), gBackLeft, false);
        scene.world().setBlocks(util.select().fromTo(1, 2, 3, 3, 2, 3), gMid, false);
        scene.world().setBlock(util.grid().at(4, 2, 3), gBackRight, false);

        // Y=3
        scene.world().setBlock(util.grid().at(0, 3, 3), gBackLeft, false);
        scene.world().setBlocks(util.select().fromTo(1, 3, 3, 3, 3, 3), gMid, false);
        scene.world().setBlock(util.grid().at(4, 3, 3), gBackRight, false);

        // Show Front and Back Layers
        scene.world().showSection(util.select().fromTo(0, 2, 1, 4, 3, 1), Direction.DOWN); // Front Z=1
        scene.world().showSection(util.select().fromTo(0, 2, 3, 4, 3, 3), Direction.DOWN); // Back Z=3

        scene.idle(20);

        // Highlight Quest Interface
        scene.overlay().showText(40)
                .text("quest_interface_structure.text_1") // "The Quest Interface..."
                .pointAt(util.vector().centerOf(2, 2, 1))
                .placeNearTarget()
                .colored(PonderPalette.GREEN);
        scene.idle(50);

        // --- Step 5: Top Layer (Y=4) ---
        scene.world().setBlocks(util.select().fromTo(0, 4, 1, 4, 4, 3), deductionCasing, false);
        scene.world().showSection(util.select().fromTo(0, 4, 1, 4, 4, 3), Direction.DOWN);
        scene.idle(20);

        // --- Step 6: Final Check ---
        scene.effects().indicateSuccess(util.grid().at(2, 2, 1));
        scene.idle(20);

        // Speed requirement
        scene.overlay().showText(60)
                .text("quest_interface_structure.text_4") // "Requires... 256 RPM"
                .pointAt(util.vector().centerOf(2, 2, 1))
                .placeNearTarget()
                .colored(PonderPalette.FAST);
        scene.idle(70);

        // --- Step 7: Interface Replacements ---
        scene.overlay().showText(60)
                .text("quest_interface_structure.text_5") // "You can replace any Deduction Casing..."
                .pointAt(util.vector().centerOf(4, 2, 2)) // Point at a side casing
                .placeNearTarget()
                .colored(PonderPalette.WHITE);
        scene.idle(70);

        // Define Interface Blocks with CORRECT rotation
        // The structure is centered at X=2, Z=2.
        // Right side is X=4. Facing OUTWARDS (East) usually, but for input it needs to
        // face OUTWARDS to accept shaft from outside?
        // QuestInputBlock is a DirectionalKineticBlock. If placed at X=4 facing EAST,
        // the shaft comes from EAST.
        BlockState questInput = ModBlocks.QUEST_INPUT.get().defaultBlockState()
                .setValue(QuestInputBlock.FACING, Direction.EAST);

        // Left side is X=0. Facing WEST.
        BlockState questSubmission = ModBlocks.QUEST_SUBMISSION.get().defaultBlockState()
                .setValue(QuestSubmissionBlock.FACING, Direction.WEST);

        // 1. Show Kinetic Input (Right Side X=4)
        scene.world().hideSection(util.select().position(4, 2, 2), Direction.EAST);
        scene.idle(5);
        scene.world().setBlock(util.grid().at(4, 2, 2), questInput, false);
        scene.world().showSection(util.select().position(4, 2, 2), Direction.WEST);

        scene.overlay().showText(60)
                .text("quest_interface_structure.text_6") // "Use a Kinetic Input Interface..."
                .pointAt(util.vector().centerOf(4, 2, 2))
                .placeNearTarget()
                .colored(PonderPalette.BLUE);
        scene.idle(70);

        // Add Stress Source (Motor + Shafts) feeding into QuestInput
        BlockPos motorPos = util.grid().at(6, 2, 2);
        BlockPos shaftPos = util.grid().at(5, 2, 2);

        scene.world().setBlock(shaftPos,
                AllBlocks.SHAFT.get().defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.WEST), false);

        scene.world().showSection(util.select().fromTo(5, 2, 2, 6, 2, 2), Direction.WEST);
        scene.idle(10);

        scene.effects().rotationSpeedIndicator(motorPos);
        scene.world().setKineticSpeed(util.select().fromTo(5, 2, 2, 6, 2, 2), 256);
        scene.world().setKineticSpeed(util.select().position(4, 2, 2), 256); // Speed up the input block
        scene.world().setKineticSpeed(util.select().fromTo(1, 2, 2, 3, 2, 2), 256); // Speed up internal cogs
        scene.world().setKineticSpeed(util.select().fromTo(1, 3, 2, 3, 3, 2), -256); // Speed up internal cogs (gears
                                                                                     // swap rotation)
        scene.idle(60);

        // 2. Show Submission Interface (Left Side X=0)
        scene.world().hideSection(util.select().position(0, 2, 2), Direction.WEST);
        scene.idle(5);
        scene.world().setBlock(util.grid().at(0, 2, 2), questSubmission, false);
        scene.world().showSection(util.select().position(0, 2, 2), Direction.EAST);

        scene.overlay().showText(60)
                .text("quest_interface_structure.text_7") // "...and a Quest Submission Interface..."
                .pointAt(util.vector().centerOf(0, 2, 2))
                .placeNearTarget()
                .colored(PonderPalette.BLUE);
        scene.idle(70);

        // Add Automation (Belt + Funnel) feeding into QuestSubmission
        BlockPos beltStart = util.grid().at(-2, 1, 2);
        BlockPos beltEnd = util.grid().at(-4, 1, 2);
        BlockPos funnelPos = util.grid().at(-2, 2, 2);

        // Setup Belt
        scene.world().setBlock(beltStart, AllBlocks.DEPOT.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(beltStart), Direction.EAST);

        // Brass Funnel on the Submission Interface
        BlockState funnel = AllBlocks.BRASS_FUNNEL.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.WEST);
        scene.world().setBlock(funnelPos, funnel, false);
        scene.world().showSection(util.select().position(funnelPos), Direction.DOWN);

        // Flap the funnel
        // Flap the funnel
        scene.effects().indicateSuccess(funnelPos);

        scene.idle(20);

        // Limit warning
        scene.overlay().showText(80)
                .text("quest_interface_structure.text_8") // "Only one of each..."
                .pointAt(util.vector().centerOf(2, 2, 1))
                .placeNearTarget()
                .colored(PonderPalette.RED);
        scene.idle(60);
    }
}
