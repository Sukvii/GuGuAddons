package com.gugucraft.guguaddons.client.renderer;

import com.gugucraft.guguaddons.block.entity.CentrifugeBlockEntity;
import com.gugucraft.guguaddons.client.ModPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class CentrifugeRenderer extends KineticBlockEntityRenderer<CentrifugeBlockEntity> {
    private static final float[] BASIN_OFFSETS = {
            28 / 16f, 0,
            -28 / 16f, 0,
            0, 28 / 16f,
            0, -28 / 16f
    };

    public CentrifugeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRenderOffScreen(CentrifugeBlockEntity blockEntity) {
        return true;
    }

    @Override
    protected void renderSafe(CentrifugeBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(blockEntity.getLevel())) {
            return;
        }

        VertexConsumer solid = buffer.getBuffer(RenderType.solid());

        SuperByteBuffer beams = CachedBuffers.partial(ModPartialModels.CENTRIFUGE_BEAMS, blockEntity.getBlockState());
        standardKineticRotationTransform(beams, blockEntity, light).renderInto(poseStack, solid);

        for (int i = 0; i < blockEntity.getBasins(); i++) {
            int offsetIndex = i * 2;
            SuperByteBuffer basin = CachedBuffers.partial(ModPartialModels.CENTRIFUGE_BASIN, blockEntity.getBlockState());
            standardKineticRotationTransform(basin, blockEntity, light)
                    .translate(BASIN_OFFSETS[offsetIndex], 0, BASIN_OFFSETS[offsetIndex + 1])
                    .renderInto(poseStack, solid);
        }

        BlockState shaftState = shaft(Direction.Axis.Y);
        renderRotatingKineticBlock(blockEntity, shaftState, poseStack, buffer.getBuffer(getRenderType(blockEntity, shaftState)), light);
    }
}
