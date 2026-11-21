package com.gugucraft.guguaddons.client.ponder.scenes;

import com.gugucraft.guguaddons.block.custom.AbyssCatalyticChamberBlock;
import com.gugucraft.guguaddons.block.custom.MechanicalShriekerBlock;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;

/**
 * Ponder scene for the Abyss Catalytic Chamber. The structure file is an empty 5x5 plate whose bounds reach
 * y = 7 (an air block in the top corner); every block is placed here, since Ponder only shows blocks inside the
 * structure bounds. Text order matters: Ponder keys every {@code text(...)} call as {@code text_1, text_2, ...}
 * in call order, so the lang files list the texts in the same order as below.
 */
public class AbyssCatalyticChamberScenes {
    private static final float SPEED = 64;

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("abyss_catalytic_chamber", "Running Abyss Catalysis");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.85f);
        scene.setSceneOffsetY(-1.5f);

        BlockPos burner = util.grid().at(2, 1, 2);
        BlockPos bottom = util.grid().at(2, 2, 2);
        BlockPos middle = util.grid().at(2, 3, 2);
        BlockPos top = util.grid().at(2, 4, 2);
        BlockPos shrieker = util.grid().at(2, 5, 2);
        BlockPos depot = util.grid().at(2, 1, 1);
        BlockPos sensor = util.grid().at(0, 1, 2);
        Selection chamber = util.select().fromTo(bottom, top);
        Selection shafts = util.select().fromTo(2, 6, 2, 2, 7, 2);
        Selection drive = util.select().position(shrieker).add(util.select().fromTo(2, 6, 2, 2, 7, 2));

        // Andesite casing first; it is swapped for a Blaze Burner when heat is explained
        scene.world().setBlock(burner, AllBlocks.ANDESITE_CASING.getDefaultState(), false);
        scene.world().setBlock(bottom, ModBlocks.ABYSS_CATALYTIC_CHAMBER.get().defaultBlockState(), false);
        scene.world().setBlock(middle, ModBlocks.ABYSS_CATALYTIC_CHAMBER_MIDDLE.get().defaultBlockState(), false);
        scene.world().setBlock(top, ModBlocks.ABYSS_CATALYTIC_CHAMBER_TOP.get().defaultBlockState(), false);
        scene.world().setBlock(shrieker, ModBlocks.MECHANICAL_SHRIEKER.get().defaultBlockState()
                .setValue(MechanicalShriekerBlock.FACING, Direction.DOWN), false);
        scene.world().setBlocks(shafts, AllBlocks.SHAFT.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y), false);

        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().position(burner), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(chamber, Direction.DOWN);
        scene.idle(15);

        // 1
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("The Abyss Catalytic Chamber consists of a bottom, a middle and a top tier. Placing it builds all three")
                .pointAt(util.vector().blockSurface(middle, Direction.WEST))
                .placeNearTarget();
        scene.idle(90);

        // 2
        scene.world().showSection(drive, Direction.DOWN);
        scene.idle(10);
        scene.world().setKineticSpeed(drive, SPEED);
        scene.effects().rotationSpeedIndicator(util.grid().at(2, 6, 2));
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("A Mechanical Shrieker facing down above the chamber runs the process. It may sit directly on the top tier or one block higher")
                .pointAt(util.vector().blockSurface(shrieker, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);

        // 3
        scene.overlay().showText(80)
                .text("Power the Shrieker through a shaft from above. Higher speeds shorten each step")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 6, 2), Direction.WEST))
                .placeNearTarget();
        scene.idle(90);

        // 4
        scene.world().setBlock(burner, AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), true);
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Heated recipes draw heat from a Blaze Burner below the bottom tier")
                .pointAt(util.vector().blockSurface(burner, Direction.WEST))
                .placeNearTarget();
        scene.idle(80);

        // 5: step I
        scene.overlay().showOutline(PonderPalette.GREEN, "tier", util.select().position(bottom), 90);
        scene.overlay().showControls(util.vector().blockSurface(bottom, Direction.WEST), Pointing.RIGHT, 30)
                .rightClick()
                .withItem(new ItemStack(Items.SCULK));
        scene.idle(15);
        setShrieking(scene, shrieker, true);
        vibrate(scene, util, bottom);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Step I, Settling: starts once the bottom ingredients, heat and power are ready. It consumes the bottom ingredients and emits a vibration of frequency 4")
                .pointAt(util.vector().blockSurface(bottom, Direction.WEST))
                .placeNearTarget()
                .colored(PonderPalette.GREEN);
        scene.idle(100);

        // 6: step II
        scene.overlay().showOutline(PonderPalette.GREEN, "tier", util.select().position(middle), 100);
        scene.overlay().showControls(util.vector().blockSurface(middle, Direction.WEST), Pointing.RIGHT, 30)
                .rightClick()
                .withItem(new ItemStack(Items.SCULK_CATALYST));
        scene.idle(15);
        vibrate(scene, util, bottom);
        scene.overlay().showText(100)
                .attachKeyFrame()
                .text("Step II, Activation: requires a catalyst in the middle tier and pauses without one. It emits a vibration of frequency 8, and the catalyst may be consumed when it ends")
                .pointAt(util.vector().blockSurface(middle, Direction.WEST))
                .placeNearTarget()
                .colored(PonderPalette.GREEN);
        scene.idle(110);

        // 7: step III
        scene.overlay().showOutline(PonderPalette.GREEN, "tier", util.select().position(top), 100);
        vibrate(scene, util, bottom);
        scene.idle(15);
        scene.overlay().showControls(util.vector().blockSurface(top, Direction.WEST), Pointing.RIGHT, 30)
                .rightClick()
                .withItem(new ItemStack(Items.ECHO_SHARD));
        scene.overlay().showText(100)
                .attachKeyFrame()
                .text("Step III, Condensation: emits a vibration of frequency 14 and waits. Only now add the top ingredients; once they suffice, they are consumed and the products are made")
                .pointAt(util.vector().blockSurface(top, Direction.WEST))
                .placeNearTarget()
                .colored(PonderPalette.GREEN);
        scene.idle(110);
        setShrieking(scene, shrieker, false);

        // 8: failure rule
        scene.overlay().showOutline(PonderPalette.RED, "tier", util.select().position(top), 110);
        scene.overlay().showText(110)
                .attachKeyFrame()
                .text("The top tier must stay empty during Settling and Activation, or the batch turns into Sculk Dregs. The next batch may be preloaded in the bottom tier, and the middle tier may keep its catalyst")
                .pointAt(util.vector().blockSurface(top, Direction.WEST))
                .placeNearTarget()
                .colored(PonderPalette.RED);
        scene.idle(120);

        // 9: outputs
        scene.world().setBlock(depot, AllBlocks.DEPOT.getDefaultState(), false);
        scene.world().modifyBlock(bottom, state -> state.setValue(AbyssCatalyticChamberBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(util.select().position(depot), Direction.DOWN);
        scene.idle(10);
        scene.world().createItemOnBeltLike(depot, Direction.SOUTH, new ItemStack(Items.ECHO_SHARD));
        scene.overlay().showText(100)
                .attachKeyFrame()
                .text("Products are stored in the bottom tier and can be extracted with Funnels or pipes. A Depot, Belt or container diagonally below receives them directly; use a Wrench to choose the side")
                .pointAt(util.vector().topOf(depot))
                .placeNearTarget();
        scene.idle(110);

        // 10: reading the steps
        scene.world().setBlock(sensor, Blocks.CALIBRATED_SCULK_SENSOR.defaultBlockState()
                .setValue(CalibratedSculkSensorBlock.FACING, Direction.WEST), false);
        scene.world().showSection(util.select().position(sensor), Direction.DOWN);
        scene.idle(10);
        vibrate(scene, util, bottom);
        scene.world().modifyBlock(sensor, state -> state
                .setValue(SculkSensorBlock.PHASE, SculkSensorPhase.ACTIVE)
                .setValue(SculkSensorBlock.POWER, 15), false);
        scene.effects().indicateRedstone(sensor);
        scene.overlay().showText(110)
                .attachKeyFrame()
                .text("The chamber has no comparator output. Calibrated Sculk Sensors tuned to frequencies 4, 8 and 14 tell which step is running, which allows timed automatic feeding")
                .pointAt(util.vector().blockSurface(sensor, Direction.WEST))
                .placeNearTarget();
        scene.idle(30);
        scene.world().modifyBlock(sensor, state -> state
                .setValue(SculkSensorBlock.PHASE, SculkSensorPhase.INACTIVE)
                .setValue(SculkSensorBlock.POWER, 0), false);
        scene.idle(90);

        scene.markAsFinished();
    }

    private static void setShrieking(CreateSceneBuilder scene, BlockPos shrieker, boolean shrieking) {
        scene.world().modifyBlock(shrieker, state -> state.setValue(MechanicalShriekerBlock.SHRIEKING, shrieking), false);
    }

    /** Sculk particles around the bottom tier, standing in for the vibration it emits at each step. */
    private static void vibrate(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos bottom) {
        scene.effects().emitParticles(util.vector().centerOf(bottom),
                scene.effects().simpleParticleEmitter(ParticleTypes.SCULK_SOUL, util.vector().of(0, 0.05, 0)), 2, 3);
    }
}
