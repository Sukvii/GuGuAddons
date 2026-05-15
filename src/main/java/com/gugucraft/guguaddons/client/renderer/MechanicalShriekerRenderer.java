package com.gugucraft.guguaddons.client.renderer;

import com.gugucraft.guguaddons.block.entity.MechanicalShriekerBlockEntity;
import com.gugucraft.guguaddons.block.custom.MechanicalShriekerBlock;
import com.gugucraft.guguaddons.client.ModPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class MechanicalShriekerRenderer extends KineticBlockEntityRenderer<MechanicalShriekerBlockEntity> {
    public MechanicalShriekerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(MechanicalShriekerBlockEntity blockEntity, BlockState state) {
        return CachedBuffers.partialFacingVertical(ModPartialModels.MECHANICAL_SHRIEKER_INNER, state,
                state.getValue(MechanicalShriekerBlock.FACING));
    }
}
