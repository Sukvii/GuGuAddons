package com.gugucraft.guguaddons.ponder.scenes;

import com.gugucraft.guguaddons.block.entity.CentrifugeBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Ponder scene for the Centrifuge. Text order matters: Ponder keys every {@code text(...)} call as
 * {@code text_1, text_2, ...} in call order, so the lang files list the texts in the same order as below.
 */
public class CentrifugeScenes {
    private static final float SPEED = 100;

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("centrifuge", "Separating Materials with the Centrifuge");
        scene.configureBasePlate(0, 0, 5);

        BlockPos centrifuge = util.grid().at(2, 1, 2);
        BlockPos clutch = util.grid().at(2, 2, 2);
        BlockPos lever = util.grid().at(2, 2, 1);
        BlockPos funnel = util.grid().at(0, 1, 1);
        Selection footprint = util.select().fromTo(1, 1, 1, 3, 1, 3);
        Selection machine = util.select().position(centrifuge);
        Selection power = util.select().fromTo(2, 2, 1, 2, 3, 2)
                .add(util.select().fromTo(3, 3, 2, 5, 3, 2))
                .add(util.select().fromTo(5, 1, 2, 5, 2, 2));
        Selection pump = util.select().fromTo(1, 2, 2, 1, 4, 3)
                .add(util.select().fromTo(2, 4, 2, 2, 4, 3));
        Selection clutchAndLever = util.select().position(clutch).add(util.select().position(lever));

        scene.world().setKineticSpeed(machine, 0);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);
        scene.world().showSection(footprint, Direction.DOWN);
        scene.idle(15);

        // 1
        scene.overlay().showOutline(PonderPalette.GREEN, "footprint", footprint, 70);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("The Centrifuge occupies a 3x3 area. The eight surrounding blocks must be free when it is placed")
                .pointAt(util.vector().topOf(centrifuge))
                .placeNearTarget();
        scene.idle(80);

        // 2
        ItemStack basin = AllBlocks.BASIN.asStack();
        for (int i = 0; i < CentrifugeBlockEntity.MAX_BASINS; i++) {
            scene.overlay().showControls(util.vector().topOf(centrifuge), Pointing.DOWN, 12)
                    .rightClick()
                    .withItem(basin);
            scene.idle(4);
            scene.world().modifyBlockEntity(centrifuge, CentrifugeBlockEntity.class, be -> be.addBasin(basin.copy()));
            scene.idle(12);
        }
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Right-click it with Basins to install them. It only operates once all four Basins are installed")
                .pointAt(util.vector().topOf(centrifuge))
                .placeNearTarget();
        scene.idle(90);

        // 3
        scene.world().showSection(power, Direction.DOWN);
        scene.idle(10);
        scene.world().setKineticSpeed(machine, SPEED);
        scene.effects().rotationSpeedIndicator(clutch);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Power it with a vertical shaft from above. Each recipe sets a minimum speed, and higher speeds process faster")
                .pointAt(util.vector().blockSurface(clutch, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);

        // 4
        scene.world().toggleRedstonePower(clutchAndLever);
        scene.effects().indicateRedstone(lever);
        scene.world().setKineticSpeed(machine, 0);
        scene.idle(10);
        scene.overlay().showText(100)
                .attachKeyFrame()
                .text("Its inventory is locked while it spins. Items and fluids can only be inserted or extracted while it is stopped, e.g. by a Clutch")
                .pointAt(util.vector().blockSurface(lever, Direction.NORTH))
                .placeNearTarget()
                .colored(PonderPalette.RED);
        scene.idle(110);

        // 5
        scene.world().showSection(pump, Direction.DOWN);
        scene.idle(10);
        ItemStack enderEye = new ItemStack(Items.ENDER_EYE);
        ElementLink<EntityElement> thrown = scene.world().createItemEntity(
                util.vector().topOf(centrifuge.north().above(2)), util.vector().of(0, 0.1, 0), enderEye);
        scene.idle(16);
        scene.world().modifyEntity(thrown, Entity::discard);
        scene.world().modifyBlockEntity(centrifuge, CentrifugeBlockEntity.class,
                be -> be.getInputInventory().setStackInSlot(0, enderEye.copy()));
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("While stopped, ingredients can be thrown on top, inserted by hand, or supplied through Funnels and pipes on any of its nine blocks")
                .pointAt(util.vector().topOf(centrifuge.north()))
                .placeNearTarget();
        scene.idle(100);

        // 6
        scene.world().toggleRedstonePower(clutchAndLever);
        scene.effects().indicateRedstone(lever);
        scene.world().setKineticSpeed(machine, SPEED);
        scene.idle(10);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Processing starts once it spins again. Stopping it midway only pauses the progress")
                .pointAt(util.vector().topOf(centrifuge))
                .placeNearTarget();
        scene.idle(100);
        scene.world().modifyBlockEntity(centrifuge, CentrifugeBlockEntity.class, be -> {
            be.getInputInventory().setStackInSlot(0, ItemStack.EMPTY);
            be.getOutputInventory().setStackInSlot(0, new ItemStack(Items.ENDER_PEARL));
            be.getOutputInventory().setStackInSlot(1, new ItemStack(Items.BLAZE_POWDER));
        });
        scene.effects().indicateSuccess(centrifuge);
        scene.idle(10);

        // 7
        scene.world().toggleRedstonePower(clutchAndLever);
        scene.effects().indicateRedstone(lever);
        scene.world().setKineticSpeed(machine, 0);
        scene.idle(10);
        scene.world().showSection(util.select().position(funnel), Direction.EAST);
        scene.idle(10);
        scene.world().modifyBlockEntity(centrifuge, CentrifugeBlockEntity.class, be -> {
            be.getOutputInventory().setStackInSlot(0, ItemStack.EMPTY);
            be.getOutputInventory().setStackInSlot(1, ItemStack.EMPTY);
        });
        scene.world().flapFunnel(funnel, true);
        scene.world().createItemEntity(util.vector().blockSurface(funnel, Direction.WEST), util.vector().of(-0.1, 0.1, 0),
                new ItemStack(Items.ENDER_PEARL));
        scene.world().createItemEntity(util.vector().blockSurface(funnel, Direction.WEST), util.vector().of(-0.1, 0.15, 0.05),
                new ItemStack(Items.BLAZE_POWDER));
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Once finished, stop it again to extract the products from any of its nine blocks")
                .pointAt(util.vector().blockSurface(funnel, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);

        scene.markAsFinished();
        scene.idle(20);
        scene.world().modifyEntities(ItemEntity.class, Entity::discard);
    }
}
