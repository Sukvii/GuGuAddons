package com.gugucraft.guguaddons.client.compat.emi;

import com.gugucraft.guguaddons.block.custom.MechanicalShriekerBlock;
import com.gugucraft.guguaddons.client.ModPartialModels;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.gui.CustomLightingSettings;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.ILightingSettings;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class AnimatedAbyssCatalyticChamberDisplay {
    private static final ILightingSettings DEFAULT_LIGHTING = CustomLightingSettings.builder()
            .firstLightRotation(12.5f, -45.0f)
            .secondLightRotation(-20.0f, -50.0f)
            .build();

    void drawMachine(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack poseStack = graphics.pose();
        RenderSystem.enableDepthTest();
        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset, 300);
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        poseStack.mulPose(Axis.YP.rotationDegrees(22.5f));

        int scale = 23;
        blockElement(ModPartialModels.MECHANICAL_SHRIEKER_INNER)
                .rotateBlock(180, getCurrentAngle() * 2, 0)
                .atLocal(0, 0, 0)
                .scale(scale)
                .render(graphics);
        blockElement(ModBlocks.MECHANICAL_SHRIEKER.get().defaultBlockState()
                .setValue(MechanicalShriekerBlock.FACING, Direction.DOWN)
                .setValue(MechanicalShriekerBlock.SHRIEKING, true))
                .atLocal(0, 0, 0)
                .scale(scale)
                .render(graphics);
        blockElement(ModBlocks.ABYSS_CATALYTIC_CHAMBER_TOP.get().defaultBlockState())
                .atLocal(0, 2, 0)
                .scale(scale)
                .render(graphics);
        blockElement(ModBlocks.ABYSS_CATALYTIC_CHAMBER_MIDDLE.get().defaultBlockState())
                .atLocal(0, 3, 0)
                .scale(scale)
                .render(graphics);
        blockElement(ModBlocks.ABYSS_CATALYTIC_CHAMBER.get().defaultBlockState())
                .atLocal(0, 4, 0)
                .scale(scale)
                .render(graphics);

        poseStack.popPose();
    }

    void drawHeater(GuiGraphics graphics, int xOffset, int yOffset, BlazeBurnerBlock.HeatLevel heatLevel) {
        PoseStack poseStack = graphics.pose();
        RenderSystem.enableDepthTest();
        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset, 200);
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        poseStack.mulPose(Axis.YP.rotationDegrees(22.5f));

        int scale = 23;
        float offset = (Mth.sin(AnimationTickHolder.getRenderTime() / 16f) + 0.5f) / 16f;

        blockElement(AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, heatLevel))
                .atLocal(0, 1.65, 0)
                .scale(scale)
                .render(graphics);

        PartialModel blaze = heatLevel == BlazeBurnerBlock.HeatLevel.SEETHING
                ? AllPartialModels.BLAZE_SUPER
                : AllPartialModels.BLAZE_ACTIVE;
        PartialModel rods = heatLevel == BlazeBurnerBlock.HeatLevel.SEETHING
                ? AllPartialModels.BLAZE_BURNER_SUPER_RODS_2
                : AllPartialModels.BLAZE_BURNER_RODS_2;

        blockElement(blaze)
                .atLocal(1, 1.8, 1)
                .rotate(0, 180, 0)
                .scale(scale)
                .render(graphics);
        blockElement(rods)
                .atLocal(1, 1.7 + offset, 1)
                .rotate(0, 180, 0)
                .scale(scale)
                .render(graphics);

        poseStack.scale(scale, -scale, scale);
        poseStack.translate(0, -1.8, 0);

        SpriteShiftEntry flame = heatLevel == BlazeBurnerBlock.HeatLevel.SEETHING
                ? AllSpriteShifts.SUPER_BURNER_FLAME
                : AllSpriteShifts.BURNER_FLAME;
        float spriteWidth = flame.getTarget().getU1() - flame.getTarget().getU0();
        float spriteHeight = flame.getTarget().getV1() - flame.getTarget().getV0();
        float time = AnimationTickHolder.getRenderTime();
        float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();

        double vScroll = speed * time;
        vScroll -= Math.floor(vScroll);
        vScroll = vScroll * spriteHeight / 2;

        double uScroll = speed * time / 2;
        uScroll -= Math.floor(uScroll);
        uScroll = uScroll * spriteWidth / 2;

        CachedBuffers.partial(AllPartialModels.BLAZE_BURNER_FLAME, Blocks.AIR.defaultBlockState())
                .shiftUVScrolling(flame, (float) uScroll, (float) vScroll)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(poseStack, graphics.bufferSource().getBuffer(RenderType.cutoutMipped()));

        poseStack.popPose();
    }

    private static GuiGameElement.GuiRenderBuilder blockElement(BlockState state) {
        return GuiGameElement.of(state).lighting(DEFAULT_LIGHTING);
    }

    private static GuiGameElement.GuiRenderBuilder blockElement(PartialModel partialModel) {
        return GuiGameElement.of(partialModel).lighting(DEFAULT_LIGHTING);
    }

    private static float getCurrentAngle() {
        return (AnimationTickHolder.getRenderTime() * 4f) % 360;
    }
}
