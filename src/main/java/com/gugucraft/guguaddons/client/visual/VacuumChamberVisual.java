package com.gugucraft.guguaddons.client.visual;

import com.gugucraft.guguaddons.block.entity.VacuumChamberBlockEntity;
import com.gugucraft.guguaddons.client.ModPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

import java.util.function.Consumer;

public class VacuumChamberVisual extends SingleAxisRotatingVisual<VacuumChamberBlockEntity> implements SimpleDynamicVisual {
    private final OrientedInstance head;
    private final VacuumChamberBlockEntity chamber;

    public VacuumChamberVisual(VisualizationContext context, VacuumChamberBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick, Models.partial(ModPartialModels.VACUUM_COG));
        this.chamber = blockEntity;

        head = instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(ModPartialModels.VACUUM_PIPE))
                .createInstance();
        animate(partialTick);
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        animate(context.partialTick());
    }

    private void animate(float partialTick) {
        float renderedHeadOffset = chamber.getRenderedHeadOffset(partialTick);

        head.position(getVisualPosition())
                .translatePosition(0, -renderedHeadOffset, 0)
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(head);
    }

    @Override
    protected void _delete() {
        super._delete();
        head.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        consumer.accept(head);
    }
}
