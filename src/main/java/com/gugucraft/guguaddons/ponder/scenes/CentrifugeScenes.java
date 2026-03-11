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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class CentrifugeScenes {
    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("centrifuge", "guguaddons.ponder.centrifuge.header");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);

        BlockPos centrifuge = util.grid().at(2, 1, 2);
        Selection centrifugeSelection = util.select().position(centrifuge);
        scene.world().setKineticSpeed(centrifugeSelection, 0);

        scene.world().showSection(util.select().position(centrifuge), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(60)
                .attachKeyFrame()
                .text("guguaddons.ponder.centrifuge.text_1")
                .pointAt(util.vector().topOf(centrifuge))
                .placeNearTarget();
        scene.idle(50);

        ItemStack basin = AllBlocks.BASIN.asStack();
        for (int i = 0; i < 4; i++) {
            scene.overlay().showControls(util.vector().blockSurface(centrifuge, Direction.NORTH), Pointing.RIGHT, 20)
                    .withItem(basin);
            scene.world().modifyBlockEntity(centrifuge, CentrifugeBlockEntity.class, be -> be.addBasin(basin.copy()));
            scene.idle(15);
        }

        scene.overlay().showText(60)
                .attachKeyFrame()
                .text("guguaddons.ponder.centrifuge.text_2")
                .pointAt(util.vector().centerOf(centrifuge))
                .placeNearTarget();
        scene.idle(50);

        scene.world().setBlock(centrifuge.above(), AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y), false);
        scene.world().showSection(util.select().position(centrifuge.above()), Direction.DOWN);
        scene.idle(5);
        scene.world().setKineticSpeed(util.select().position(centrifuge.above()), 160);
        scene.world().setKineticSpeed(centrifugeSelection, 160);
        scene.effects().indicateSuccess(centrifuge);
        scene.idle(20);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .text("guguaddons.ponder.centrifuge.text_3")
                .pointAt(util.vector().topOf(centrifuge.above()))
                .placeNearTarget();
        scene.idle(60);

        scene.world().setBlock(centrifuge.above(), AllBlocks.CLUTCH.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y)
                .setValue(BlockStateProperties.POWERED, true), true);
        scene.world().setKineticSpeed(centrifugeSelection, 0);
        scene.idle(15);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("guguaddons.ponder.centrifuge.text_4")
                .pointAt(util.vector().centerOf(centrifuge.north()))
                .placeNearTarget();
        scene.idle(45);

        ItemStack enderEye = new ItemStack(Items.ENDER_EYE);
        ElementLink<EntityElement> item = scene.world().createItemEntity(util.vector().topOf(centrifuge.north().above(3)),
                util.vector().of(0, 0.2, 0), enderEye);
        scene.idle(18);
        scene.world().modifyEntity(item, Entity::discard);
        scene.world().modifyBlockEntity(centrifuge, CentrifugeBlockEntity.class,
                be -> be.getInputInventory().setStackInSlot(0, enderEye.copy()));
        scene.idle(10);

        scene.world().setBlock(centrifuge.above(), AllBlocks.CLUTCH.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y)
                .setValue(BlockStateProperties.POWERED, false), true);
        scene.world().setKineticSpeed(centrifugeSelection, 160);
        scene.idle(90);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("guguaddons.ponder.centrifuge.text_5")
                .pointAt(util.vector().centerOf(centrifuge))
                .placeNearTarget();
        scene.idle(45);

        scene.world().setBlock(centrifuge.above(), AllBlocks.CLUTCH.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y)
                .setValue(BlockStateProperties.POWERED, true), true);
        scene.world().setKineticSpeed(centrifugeSelection, 0);
        scene.idle(10);

        scene.world().showSection(util.select().position(0, 1, 1), Direction.DOWN);
        scene.world().createItemEntity(util.vector().centerOf(util.grid().at(0, 1, 1)), util.vector().of(0, 0.2, 0),
                new ItemStack(Items.ENDER_PEARL));
        scene.world().createItemEntity(util.vector().centerOf(util.grid().at(0, 1, 1)), util.vector().of(0, 0.2, 0),
                new ItemStack(Items.BLAZE_POWDER));
        scene.idle(30);

        scene.markAsFinished();
        scene.idle(20);
        scene.world().modifyEntities(ItemEntity.class, Entity::discard);
    }
}
