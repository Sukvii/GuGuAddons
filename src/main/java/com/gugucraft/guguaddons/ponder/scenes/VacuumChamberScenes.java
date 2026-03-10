package com.gugucraft.guguaddons.ponder.scenes;

import com.gugucraft.guguaddons.block.entity.VacuumChamberBlockEntity;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class VacuumChamberScenes {
    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("vacuum_chamber", "guguaddons.ponder.vacuum_chamber.header");
        scene.configureBasePlate(0, 0, 5);
        scene.world().setBlock(util.grid().at(1, 1, 2), AllBlocks.ANDESITE_CASING.getDefaultState(), false);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(1, 4, 3, 1, 1, 5), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 2), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 2, 2), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 4, 2), Direction.SOUTH);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(3, 1, 1, 1, 1, 1), Direction.SOUTH);
        scene.world().showSection(util.select().fromTo(3, 1, 5, 3, 1, 2), Direction.SOUTH);
        scene.idle(20);

        BlockPos basin = util.grid().at(1, 2, 2);
        BlockPos compressor = util.grid().at(1, 4, 2);

        ItemStack bucket = new ItemStack(Items.BUCKET);
        ItemStack snow = new ItemStack(Items.SNOW_BLOCK);
        ItemStack result = new ItemStack(Items.POWDER_SNOW_BUCKET);

        scene.overlay().showText(60)
                .pointAt(util.vector().blockSurface(basin, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame()
                .text("guguaddons.ponder.vacuum_chamber.text_1");
        scene.idle(40);

        ItemStack wrench = AllItems.WRENCH.asStack();
        scene.overlay().showControls(util.vector().topOf(compressor), Pointing.LEFT, 30).withItem(wrench);
        scene.world().modifyBlockEntity(compressor, VacuumChamberBlockEntity.class, VacuumChamberBlockEntity::changeMode);
        scene.idle(30);

        scene.overlay().showControls(util.vector().topOf(compressor), Pointing.LEFT, 30).withItem(wrench);
        scene.world().modifyBlockEntity(compressor, VacuumChamberBlockEntity.class, VacuumChamberBlockEntity::changeMode);
        scene.idle(40);

        scene.overlay().showControls(util.vector().topOf(basin), Pointing.LEFT, 30).withItem(bucket);
        scene.overlay().showControls(util.vector().topOf(basin), Pointing.RIGHT, 30).withItem(snow);
        scene.idle(20);
        scene.world().createItemOnBeltLike(basin, Direction.UP, bucket);
        scene.world().createItemOnBeltLike(basin, Direction.UP, snow);
        scene.world().modifyBlockEntity(compressor, VacuumChamberBlockEntity.class, VacuumChamberBlockEntity::startProcessingBasin);
        scene.idle(80);
        scene.effects().indicateSuccess(compressor);
        scene.idle(6);
        scene.world().createItemOnBelt(util.grid().at(1, 1, 1), Direction.UP, result);
        scene.idle(25);

        scene.rotateCameraY(-30);
        scene.idle(10);
        scene.world().setBlock(util.grid().at(1, 1, 2), AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), true);
        scene.idle(10);

        scene.overlay().showText(70)
                .pointAt(util.vector().blockSurface(basin.below(), Direction.WEST))
                .placeNearTarget()
                .text("guguaddons.ponder.vacuum_chamber.text_2");
        scene.idle(40);
    }

    public static void secondary(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("vacuum_chamber_secondary", "guguaddons.ponder.vacuum_chamber_secondary.header");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(2, 2, 2), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(2, 4, 2), Direction.SOUTH);
        scene.world().setBlock(util.grid().at(3, 3, 2), AllBlocks.SHAFT.getDefaultState(), false);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(3, 4, 2, 3, 1, 2), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(3, 1, 2, 3, 1, 5), Direction.DOWN);
        scene.idle(20);

        BlockPos basin = util.grid().at(2, 2, 2);
        BlockPos compressor = util.grid().at(2, 4, 2);

        scene.overlay().showText(50)
                .pointAt(util.vector().centerOf(compressor))
                .placeNearTarget()
                .attachKeyFrame()
                .text("guguaddons.ponder.vacuum_chamber_secondary.text_1");
        scene.idle(35);

        scene.world().showSection(util.select().fromTo(0, 1, 2, 0, 2, 2), Direction.DOWN);
        scene.idle(5);
        scene.world().setBlock(util.grid().at(3, 3, 2), AllBlocks.COGWHEEL.getDefaultState(), false);
        scene.world().showSection(util.select().fromTo(1, 3, 3, 3, 4, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(0, 3, 2, 1, 4, 2), Direction.DOWN);
        scene.idle(5);

        scene.overlay().showText(50)
                .pointAt(util.vector().centerOf(compressor))
                .placeNearTarget()
                .attachKeyFrame()
                .text("guguaddons.ponder.vacuum_chamber_secondary.text_2");
        scene.idle(40);

        scene.world().modifyBlockEntity(util.grid().at(0, 1, 2), FluidTankBlockEntity.class,
                be -> be.getTankInventory().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
        scene.idle(20);

        scene.overlay().showText(50)
                .pointAt(util.vector().centerOf(compressor))
                .placeNearTarget()
                .attachKeyFrame()
                .text("guguaddons.ponder.vacuum_chamber_secondary.text_3");
        scene.idle(35);

        scene.overlay().showText(50)
                .pointAt(util.vector().centerOf(compressor))
                .placeNearTarget()
                .attachKeyFrame()
                .text("guguaddons.ponder.vacuum_chamber_secondary.text_4");
        scene.idle(35);
    }
}
