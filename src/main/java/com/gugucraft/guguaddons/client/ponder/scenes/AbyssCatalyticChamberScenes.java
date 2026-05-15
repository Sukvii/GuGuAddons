package com.gugucraft.guguaddons.client.ponder.scenes;

import com.gugucraft.guguaddons.block.custom.MechanicalShriekerBlock;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class AbyssCatalyticChamberScenes {
    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("abyss_catalytic_chamber", "guguaddons.ponder.abyss_catalytic_chamber.header");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);

        BlockPos burner = util.grid().at(2, 1, 2);
        BlockPos bottom = util.grid().at(2, 2, 2);
        BlockPos middle = util.grid().at(2, 3, 2);
        BlockPos top = util.grid().at(2, 4, 2);
        BlockPos shrieker = util.grid().at(2, 5, 2);
        BlockPos shaft = util.grid().at(2, 6, 2);

        scene.world().setBlock(burner, AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), false);
        scene.world().showSection(util.select().position(burner), Direction.DOWN);
        scene.idle(8);

        scene.world().setBlock(bottom, ModBlocks.ABYSS_CATALYTIC_CHAMBER.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(bottom), Direction.DOWN);
        scene.idle(8);
        scene.world().setBlock(middle, ModBlocks.ABYSS_CATALYTIC_CHAMBER_MIDDLE.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(middle), Direction.DOWN);
        scene.idle(8);
        scene.world().setBlock(top, ModBlocks.ABYSS_CATALYTIC_CHAMBER_TOP.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(top), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("guguaddons.ponder.abyss_catalytic_chamber.text_1")
                .pointAt(util.vector().centerOf(middle))
                .placeNearTarget();
        scene.idle(55);

        scene.overlay().showControls(util.vector().blockSurface(top, Direction.WEST), Pointing.RIGHT, 28)
                .withItem(new ItemStack(Items.ECHO_SHARD));
        scene.overlay().showText(60)
                .text("guguaddons.ponder.abyss_catalytic_chamber.text_2")
                .pointAt(util.vector().centerOf(top))
                .placeNearTarget()
                .colored(PonderPalette.BLUE);
        scene.idle(45);

        scene.overlay().showControls(util.vector().blockSurface(middle, Direction.WEST), Pointing.RIGHT, 28)
                .withItem(new ItemStack(Items.LAVA_BUCKET));
        scene.overlay().showText(60)
                .text("guguaddons.ponder.abyss_catalytic_chamber.text_3")
                .pointAt(util.vector().centerOf(middle))
                .placeNearTarget()
                .colored(PonderPalette.FAST);
        scene.idle(45);

        scene.overlay().showControls(util.vector().blockSurface(bottom, Direction.WEST), Pointing.RIGHT, 28)
                .withItem(new ItemStack(Items.SCULK));
        scene.overlay().showText(60)
                .text("guguaddons.ponder.abyss_catalytic_chamber.text_4")
                .pointAt(util.vector().centerOf(bottom))
                .placeNearTarget()
                .colored(PonderPalette.GREEN);
        scene.idle(45);

        scene.overlay().showText(55)
                .text("guguaddons.ponder.abyss_catalytic_chamber.text_5")
                .pointAt(util.vector().centerOf(burner))
                .placeNearTarget();
        scene.idle(45);

        scene.world().setBlock(shrieker, ModBlocks.MECHANICAL_SHRIEKER.get().defaultBlockState()
                .setValue(MechanicalShriekerBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(util.select().position(shrieker), Direction.DOWN);
        scene.idle(8);
        scene.world().setBlock(shaft, AllBlocks.SHAFT.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y), false);
        scene.world().showSection(util.select().position(shaft), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(shrieker, shaft), 128);
        scene.effects().rotationSpeedIndicator(shaft);
        scene.effects().indicateSuccess(shrieker);
        scene.idle(20);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("guguaddons.ponder.abyss_catalytic_chamber.text_6")
                .pointAt(util.vector().centerOf(shrieker))
                .placeNearTarget()
                .colored(PonderPalette.GREEN);
        scene.idle(55);

        scene.markAsFinished();
        scene.idle(20);
    }
}
