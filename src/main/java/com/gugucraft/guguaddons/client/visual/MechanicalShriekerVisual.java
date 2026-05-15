package com.gugucraft.guguaddons.client.visual;

import com.gugucraft.guguaddons.block.custom.MechanicalShriekerBlock;
import com.gugucraft.guguaddons.block.entity.MechanicalShriekerBlockEntity;
import com.gugucraft.guguaddons.client.ModPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;

import java.util.function.Consumer;

public class MechanicalShriekerVisual extends KineticBlockEntityVisual<MechanicalShriekerBlockEntity>
        implements SimpleTickableVisual {
    private final RotatingInstance rotatingModel;

    public MechanicalShriekerVisual(VisualizationContext context, MechanicalShriekerBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);

        Model model = Models.partial(ModPartialModels.MECHANICAL_SHRIEKER_INNER);
        rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, model)
                .createInstance()
                .setup(blockEntity)
                .setPosition(getVisualPosition());
        rotateToFacing(blockEntity.getBlockState().getValue(MechanicalShriekerBlock.FACING));
        rotatingModel.setChanged();
    }

    @Override
    public void update(float partialTick) {
        rotatingModel.setup(blockEntity)
                .setChanged();
    }

    @Override
    public void tick(Context context) {
        applyOverstressEffect(blockEntity, rotatingModel);
    }

    @Override
    public void updateLight(float partialTick) {
        relight(rotatingModel);
    }

    @Override
    protected void _delete() {
        rotatingModel.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(rotatingModel);
    }

    private void rotateToFacing(Direction facing) {
        rotatingModel.rotation.identity()
                .rotateY(AngleHelper.rad(AngleHelper.horizontalAngle(facing)))
                .rotateX(AngleHelper.rad(AngleHelper.verticalAngle(facing) + 90));
    }
}
