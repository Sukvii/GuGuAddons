package com.gugucraft.guguaddons.client.renderer;

import com.gugucraft.guguaddons.block.custom.QuestInputBlock;
import com.gugucraft.guguaddons.block.entity.QuestInputBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

public class QuestInputRenderer extends KineticBlockEntityRenderer<QuestInputBlockEntity> {

    public QuestInputRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(QuestInputBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state);
    }

    @Override
    protected void renderSafe(QuestInputBlockEntity be, float partialTicks, com.mojang.blaze3d.vertex.PoseStack ms, net.minecraft.client.renderer.MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        SuperByteBuffer superByteBuffer = getRotatedModel(be, be.getBlockState());
        if (superByteBuffer != null) {
            standardKineticRotationTransform(superByteBuffer, be, light).renderInto(ms, buffer.getBuffer(net.minecraft.client.renderer.RenderType.solid()));
        }
    }
}
