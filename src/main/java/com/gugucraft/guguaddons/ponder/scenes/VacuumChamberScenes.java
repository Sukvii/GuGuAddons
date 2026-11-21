package com.gugucraft.guguaddons.ponder.scenes;

import com.gugucraft.guguaddons.block.entity.VacuumChamberBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Ponder scenes for the Compressor (vacuum chamber). Text order matters: Ponder keys every
 * {@code showText(...).text(...)} call as {@code text_1, text_2, ...} in call order, so the lang files list the
 * texts in the same order as they appear below.
 */
public class VacuumChamberScenes {
    private static final int RUN_TICKS = 60;

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("vacuum_chamber", "Processing Items with the Compressor");
        scene.configureBasePlate(0, 0, 5);

        BlockPos casing = util.grid().at(1, 1, 2);
        BlockPos basin = util.grid().at(1, 2, 2);
        BlockPos compressor = util.grid().at(1, 4, 2);
        BlockPos cog = util.grid().at(1, 4, 3);
        BlockPos belt = util.grid().at(1, 1, 1);
        Selection power = util.select().fromTo(1, 1, 3, 3, 1, 5)
                .add(util.select().fromTo(1, 2, 3, 1, 4, 3))
                .add(util.select().position(3, 1, 2));

        scene.world().setBlock(casing, AllBlocks.ANDESITE_CASING.getDefaultState(), false);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);
        scene.world().showSection(util.select().position(casing), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(basin), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(compressor), Direction.DOWN);
        scene.idle(15);

        // 1
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("The Compressor processes the contents of a Basin placed two blocks below it")
                .pointAt(util.vector().blockSurface(compressor, Direction.WEST))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(power, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 1, 1), Direction.SOUTH);
        scene.idle(15);

        // 2
        scene.overlay().showText(70)
                .text("It acts as a cogwheel itself and requires at least medium speed")
                .pointAt(util.vector().centerOf(cog))
                .placeNearTarget();
        scene.idle(80);

        // 3: vacuumizing
        ItemStack bucket = new ItemStack(Items.BUCKET);
        ItemStack snow = new ItemStack(Items.SNOW_BLOCK);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("In Vacuumizing mode, it runs vacuumizing recipes on the items and fluids in the Basin")
                .pointAt(util.vector().blockSurface(basin, Direction.WEST))
                .placeNearTarget();
        scene.idle(20);
        process(scene, util, basin, compressor, belt, new ItemStack(Items.POWDER_SNOW_BUCKET), bucket, snow);
        scene.idle(20);

        // 4: mode switch
        ItemStack wrench = AllItems.WRENCH.asStack();
        scene.overlay().showControls(util.vector().topOf(compressor), Pointing.DOWN, 30)
                .rightClick()
                .withItem(wrench);
        scene.idle(10);
        scene.world().modifyBlockEntity(compressor, VacuumChamberBlockEntity.class, VacuumChamberBlockEntity::changeMode);
        scene.effects().indicateSuccess(compressor);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Right-click it with a Wrench to switch between Vacuumizing and Pressurizing mode. Each mode only runs recipes of its own type")
                .pointAt(util.vector().topOf(compressor))
                .placeNearTarget();
        scene.idle(100);

        // 5
        scene.overlay().showText(70)
                .text("The mode cannot be changed while it is running. Engineer's Goggles show the current mode")
                .pointAt(util.vector().topOf(compressor))
                .placeNearTarget();
        scene.idle(80);

        // 6: pressurizing
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("In Pressurizing mode, it runs pressurizing recipes instead")
                .pointAt(util.vector().blockSurface(basin, Direction.WEST))
                .placeNearTarget();
        scene.idle(20);
        process(scene, util, basin, compressor, belt, new ItemStack(Items.BLUE_ICE),
                new ItemStack(Items.ICE), new ItemStack(Items.PACKED_ICE));
        scene.idle(20);

        // 7: heat
        scene.world().setBlock(casing, AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), true);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Some recipes require heat from a Blaze Burner below the Basin")
                .pointAt(util.vector().blockSurface(casing, Direction.WEST))
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void secondary(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("vacuum_chamber_secondary", "Internal Tanks of the Compressor");
        scene.configureBasePlate(0, 0, 5);

        BlockPos tank = util.grid().at(0, 1, 2);
        BlockPos burner = util.grid().at(2, 1, 2);
        BlockPos basin = util.grid().at(2, 2, 2);
        BlockPos pump = util.grid().at(1, 4, 2);
        BlockPos compressor = util.grid().at(2, 4, 2);
        Selection power = util.select().fromTo(3, 1, 2, 3, 1, 5)
                .add(util.select().fromTo(3, 2, 2, 3, 4, 3))
                .add(util.select().fromTo(1, 4, 3, 2, 4, 3));
        Selection fluids = util.select().fromTo(0, 1, 2, 0, 4, 2)
                .add(util.select().position(pump));

        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);
        scene.world().showSection(util.select().position(burner), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(basin), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(compressor), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(power, Direction.DOWN);
        scene.idle(15);

        // 1
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("The Compressor has its own input tank and output tank, which pipes can access from any side")
                .pointAt(util.vector().centerOf(compressor))
                .placeNearTarget();
        scene.idle(90);

        // Input example: the pump feeds the compressor's input tank from the fluid tank
        scene.world().modifyBlock(pump, state -> state.setValue(BlockStateProperties.FACING, Direction.EAST), false);
        scene.world().modifyBlockEntity(tank, FluidTankBlockEntity.class,
                be -> be.getTankInventory().fill(new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE));
        scene.world().showSection(fluids, Direction.DOWN);
        scene.idle(15);

        // 2
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Some recipes take a specific fluid ingredient from the input tank instead of the Basin")
                .pointAt(util.vector().centerOf(pump))
                .placeNearTarget()
                .colored(PonderPalette.INPUT);
        scene.idle(30);
        setMode(scene, util, compressor, true);
        process(scene, basin, compressor, new ItemStack(Items.MUD), new ItemStack(Items.DIRT));
        scene.world().modifyBlockEntity(tank, FluidTankBlockEntity.class,
                be -> be.getTankInventory().drain(250, FluidAction.EXECUTE));
        scene.idle(30);

        // 3
        scene.overlay().showText(80)
                .text("Here, the Dirt in the Basin and the Water from the input tank are pressurized into Mud")
                .pointAt(util.vector().blockSurface(basin, Direction.WEST))
                .placeNearTarget();
        scene.idle(90);
        clearBasin(scene, basin);

        // Output example: the pump drains the compressor's output tank into the fluid tank
        scene.world().modifyBlock(pump, state -> state.setValue(BlockStateProperties.FACING, Direction.WEST), false);
        scene.idle(10);

        // 4
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Likewise, some recipes store a fluid product in the output tank, where it can be pumped out")
                .pointAt(util.vector().centerOf(pump))
                .placeNearTarget()
                .colored(PonderPalette.OUTPUT);
        scene.idle(30);
        setMode(scene, util, compressor, false);
        process(scene, basin, compressor, new ItemStack(Items.SPONGE), new ItemStack(Items.WET_SPONGE));
        scene.world().modifyBlockEntity(tank, FluidTankBlockEntity.class,
                be -> be.getTankInventory().fill(new FluidStack(Fluids.WATER, 250), FluidAction.EXECUTE));
        scene.idle(30);

        // 5
        scene.overlay().showText(80)
                .text("Here, a Wet Sponge is vacuumized into a Sponge, and the extracted Water goes into the output tank")
                .pointAt(util.vector().blockSurface(basin, Direction.WEST))
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    /** Puts the inputs into the Basin, runs the Compressor and sends the result out onto the belt. */
    private static void process(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos basin, BlockPos compressor,
            BlockPos belt, ItemStack result, ItemStack... inputs) {
        Pointing[] sides = {Pointing.LEFT, Pointing.RIGHT};
        for (int i = 0; i < inputs.length; i++) {
            scene.overlay().showControls(util.vector().topOf(basin), sides[i % sides.length], 30).withItem(inputs[i]);
        }
        scene.idle(10);
        for (ItemStack input : inputs) {
            scene.world().createItemOnBeltLike(basin, Direction.UP, input.copy());
        }
        scene.world().modifyBlockEntity(compressor, VacuumChamberBlockEntity.class,
                VacuumChamberBlockEntity::startProcessingBasin);
        scene.idle(RUN_TICKS);
        clearBasin(scene, basin);
        scene.effects().indicateSuccess(compressor);
        scene.world().createItemOnBelt(belt, Direction.UP, result);
        scene.idle(10);
    }

    /** Same as above without an output belt: the result stays in the Basin. */
    private static void process(CreateSceneBuilder scene, BlockPos basin, BlockPos compressor, ItemStack result,
            ItemStack... inputs) {
        for (ItemStack input : inputs) {
            scene.world().createItemOnBeltLike(basin, Direction.UP, input.copy());
        }
        scene.idle(10);
        scene.world().modifyBlockEntity(compressor, VacuumChamberBlockEntity.class,
                VacuumChamberBlockEntity::startProcessingBasin);
        scene.idle(RUN_TICKS);
        clearBasin(scene, basin);
        scene.world().createItemOnBeltLike(basin, Direction.UP, result);
        scene.effects().indicateSuccess(compressor);
    }

    private static void setMode(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos compressor, boolean pressurizing) {
        scene.overlay().showControls(util.vector().topOf(compressor), Pointing.DOWN, 25)
                .rightClick()
                .withItem(AllItems.WRENCH.asStack());
        scene.idle(5);
        scene.world().modifyBlockEntity(compressor, VacuumChamberBlockEntity.class, be -> {
            if (be.isPressurizingMode() != pressurizing) {
                be.changeMode();
            }
        });
        scene.idle(20);
    }

    private static void clearBasin(CreateSceneBuilder scene, BlockPos basin) {
        scene.world().modifyBlockEntity(basin, BasinBlockEntity.class, be -> {
            clear(be.getInputInventory());
            clear(be.getOutputInventory());
        });
    }

    private static void clear(SmartInventory inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }
}
