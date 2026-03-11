package com.gugucraft.guguaddons.compat.emi;

import com.gugucraft.guguaddons.client.ModPartialModels;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.CustomLightingSettings;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.ILightingSettings;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

final class AnimatedCentrifugeDisplay {
    private static final ILightingSettings DEFAULT_LIGHTING = CustomLightingSettings.builder()
            .firstLightRotation(12.5f, -45.0f)
            .secondLightRotation(-20.0f, -50.0f)
            .build();

    void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack poseStack = graphics.pose();
        RenderSystem.enableDepthTest();
        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset, 200);
        poseStack.translate(2, 22, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        poseStack.mulPose(Axis.YP.rotationDegrees(112.5f));
        int scale = 25;

        blockElement(shaft(Direction.Axis.Y))
                .rotateBlock(0, getCurrentAngle(), 0)
                .scale(scale)
                .render(graphics);

        blockElement(ModBlocks.CENTRIFUGE.get().defaultBlockState())
                .scale(scale)
                .render(graphics);

        blockElement(ModPartialModels.CENTRIFUGE_BEAMS)
                .rotateBlock(0, getCurrentAngle(), 0)
                .scale(scale)
                .render(graphics);

        blockElement(ModPartialModels.CENTRIFUGE_BASIN)
                .rotateBlock(0, getCurrentAngle(), 0)
                .withRotationOffset(new Vec3(36d / 16d, 0, 0.5))
                .atLocal(-28d / 16d, 0, 0)
                .scale(scale)
                .render(graphics);

        blockElement(ModPartialModels.CENTRIFUGE_BASIN)
                .rotateBlock(0, getCurrentAngle(), 0)
                .withRotationOffset(new Vec3(-20d / 16d, 0, 0.5))
                .atLocal(28d / 16d, 0, 0)
                .scale(scale)
                .render(graphics);

        blockElement(ModPartialModels.CENTRIFUGE_BASIN)
                .rotateBlock(0, getCurrentAngle(), 0)
                .withRotationOffset(new Vec3(0.5, 0, 36d / 16d))
                .atLocal(0, 0, -28d / 16d)
                .scale(scale)
                .render(graphics);

        blockElement(ModPartialModels.CENTRIFUGE_BASIN)
                .rotateBlock(0, getCurrentAngle(), 0)
                .withRotationOffset(new Vec3(0.5, 0, -20d / 16d))
                .atLocal(0, 0, 28d / 16d)
                .scale(scale)
                .render(graphics);

        poseStack.popPose();
    }

    private static float getCurrentAngle() {
        return (AnimationTickHolder.getRenderTime() * 4f) % 360;
    }

    private static BlockState shaft(Direction.Axis axis) {
        return AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, axis);
    }

    private static GuiGameElement.GuiRenderBuilder blockElement(BlockState state) {
        return GuiGameElement.of(state).lighting(DEFAULT_LIGHTING);
    }

    private static GuiGameElement.GuiRenderBuilder blockElement(PartialModel partialModel) {
        return GuiGameElement.of(partialModel).lighting(DEFAULT_LIGHTING);
    }
}
