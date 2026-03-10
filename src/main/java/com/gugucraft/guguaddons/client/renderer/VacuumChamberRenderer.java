package com.gugucraft.guguaddons.client.renderer;

import com.gugucraft.guguaddons.block.entity.VacuumChamberBlockEntity;
import com.gugucraft.guguaddons.client.ModPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class VacuumChamberRenderer extends KineticBlockEntityRenderer<VacuumChamberBlockEntity> {

    public VacuumChamberRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRenderOffScreen(VacuumChamberBlockEntity blockEntity) {
        return true;
    }

    @Override
    protected void renderSafe(VacuumChamberBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(blockEntity.getLevel())) {
            return;
        }

        BlockState blockState = blockEntity.getBlockState();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());

        SuperByteBuffer arrows = CachedBuffers.partial(ModPartialModels.VACUUM_CHAMBER_ARROWS, blockState);
        if (blockEntity.isPressurizingMode()) {
            arrows.rotateCentered(AngleHelper.rad(180), Direction.EAST);
        }
        arrows.light(light).renderInto(poseStack, vertexConsumer);

        SuperByteBuffer cog = CachedBuffers.partial(ModPartialModels.VACUUM_COG, blockState);
        standardKineticRotationTransform(cog, blockEntity, light).renderInto(poseStack, vertexConsumer);

        SuperByteBuffer head = CachedBuffers.partial(ModPartialModels.VACUUM_PIPE, blockState);
        head.translate(0, -blockEntity.getRenderedHeadOffset(partialTicks), 0)
                .light(light)
                .renderInto(poseStack, vertexConsumer);
    }
}
