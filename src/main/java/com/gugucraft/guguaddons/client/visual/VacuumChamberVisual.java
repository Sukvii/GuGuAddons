package com.gugucraft.guguaddons.client.visual;

import com.gugucraft.guguaddons.block.entity.VacuumChamberBlockEntity;
import com.gugucraft.guguaddons.client.ModPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import net.createmod.catnip.math.AngleHelper;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

import java.util.function.Consumer;

public class VacuumChamberVisual extends SingleAxisRotatingVisual<VacuumChamberBlockEntity> implements SimpleDynamicVisual {
    private final OrientedInstance head;
    private final TransformedInstance arrows;
    private final VacuumChamberBlockEntity chamber;

    public VacuumChamberVisual(VisualizationContext context, VacuumChamberBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick, Models.partial(ModPartialModels.VACUUM_COG));
        this.chamber = blockEntity;

        head = instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(ModPartialModels.VACUUM_PIPE))
                .createInstance();
        arrows = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(ModPartialModels.VACUUM_CHAMBER_ARROWS))
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

        arrows.setIdentityTransform()
                .translate(getVisualPosition());
        if (chamber.isPressurizingMode()) {
            arrows.rotateXCentered(AngleHelper.rad(180));
        }
        arrows.setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(head, arrows);
    }

    @Override
    protected void _delete() {
        super._delete();
        head.delete();
        arrows.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        consumer.accept(head);
        consumer.accept(arrows);
    }
}
