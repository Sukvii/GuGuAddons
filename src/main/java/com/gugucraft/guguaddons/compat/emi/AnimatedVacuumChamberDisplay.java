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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

final class AnimatedVacuumChamberDisplay {
    private static final ILightingSettings DEFAULT_LIGHTING = CustomLightingSettings.builder()
            .firstLightRotation(12.5f, -45.0f)
            .secondLightRotation(-20.0f, -50.0f)
            .build();

    void draw(GuiGraphics graphics, int xOffset, int yOffset, boolean pressurizingMode) {
        PoseStack poseStack = graphics.pose();
        RenderSystem.enableDepthTest();
        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset, 200);
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        poseStack.mulPose(Axis.YP.rotationDegrees(22.5f));

        int scale = 23;

        blockElement(ModPartialModels.VACUUM_COG)
                .rotateBlock(0, getCurrentAngle() * 2, 0)
                .atLocal(0, 0, 0)
                .scale(scale)
                .render(graphics);

        blockElement(ModBlocks.VACUUM_CHAMBER.get().defaultBlockState())
                .atLocal(0, 0, 0)
                .scale(scale)
                .render(graphics);

        float animation = ((Mth.sin(AnimationTickHolder.getRenderTime() / 32f) + 1) / 5) + .5f;
        animation = Mth.clamp(animation, 0, 11.2f / 16f);

        blockElement(ModPartialModels.VACUUM_PIPE)
                .atLocal(0, animation, 0)
                .scale(scale)
                .render(graphics);

        blockElement(AllBlocks.BASIN.getDefaultState())
                .atLocal(0, 1.65, 0)
                .scale(scale)
                .render(graphics);

        GuiGameElement.GuiRenderBuilder arrows = blockElement(ModPartialModels.VACUUM_CHAMBER_ARROWS)
                .atLocal(0, 0, 0)
                .scale(scale);
        if (pressurizingMode) {
            arrows.rotateBlock(0, 0, 180);
        }
        arrows.render(graphics);

        poseStack.popPose();
    }

    private static float getCurrentAngle() {
        return (AnimationTickHolder.getRenderTime() * 4f) % 360;
    }

    private static GuiGameElement.GuiRenderBuilder blockElement(BlockState state) {
        return GuiGameElement.of(state).lighting(DEFAULT_LIGHTING);
    }

    private static GuiGameElement.GuiRenderBuilder blockElement(PartialModel partialModel) {
        return GuiGameElement.of(partialModel).lighting(DEFAULT_LIGHTING);
    }
}
