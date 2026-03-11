package com.gugucraft.guguaddons.client.visual;

import com.gugucraft.guguaddons.block.entity.CentrifugeBlockEntity;
import com.gugucraft.guguaddons.client.ModPartialModels;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

import java.util.function.Consumer;

public class CentrifugeVisual extends SingleAxisRotatingVisual<CentrifugeBlockEntity> implements SimpleDynamicVisual {
    private static final float[] BASIN_OFFSETS = {
            28 / 16f, 0,
            -28 / 16f, 0,
            0, 28 / 16f,
            0, -28 / 16f
    };

    private final RotatingInstance beams;
    private final TransformedInstance[] basins;

    public CentrifugeVisual(VisualizationContext context, CentrifugeBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick, Models.partial(AllPartialModels.SHAFT));

        beams = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(ModPartialModels.CENTRIFUGE_BEAMS))
                .createInstance();
        beams.setup(blockEntity)
                .setPosition(getVisualPosition())
                .setChanged();

        basins = new TransformedInstance[4];
        var basinInstancer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(ModPartialModels.CENTRIFUGE_BASIN));
        for (int i = 0; i < basins.length; i++) {
            basins[i] = basinInstancer.createInstance();
        }

        animate();
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        animate();
    }

    private void animate() {
        float angle = KineticBlockEntityRenderer.getAngleForBe(blockEntity, blockEntity.getBlockPos(), rotationAxis());
        for (int i = 0; i < basins.length; i++) {
            int offsetIndex = i * 2;
            basins[i].setVisible(i < blockEntity.getBasins());
            basins[i].setIdentityTransform()
                    .translate(getVisualPosition())
                    .center()
                    .rotateY(angle)
                    .uncenter()
                    .translate(BASIN_OFFSETS[offsetIndex], 0, BASIN_OFFSETS[offsetIndex + 1])
                    .setChanged();
        }
    }

    @Override
    public void update(float partialTick) {
        super.update(partialTick);
        beams.setup(blockEntity)
                .setPosition(getVisualPosition())
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(beams);
        relight(basins);
    }

    @Override
    protected void _delete() {
        super._delete();
        beams.delete();
        for (TransformedInstance basin : basins) {
            basin.delete();
        }
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        consumer.accept(beams);
        for (TransformedInstance basin : basins) {
            consumer.accept(basin);
        }
    }
}
