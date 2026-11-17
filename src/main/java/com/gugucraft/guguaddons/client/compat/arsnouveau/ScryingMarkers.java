package com.gugucraft.guguaddons.client.compat.arsnouveau;

import com.hollingsworth.arsnouveau.client.ClientInfo;
import com.hollingsworth.arsnouveau.setup.registry.ModPotions;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class ScryingMarkers {
    private static final RenderType MARKERS = RenderType.create("guguaddons_scrying_markers",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 16384, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    public static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER
                || minecraft.player == null || !minecraft.player.hasEffect(ModPotions.SCRYING_EFFECT)) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        var poses = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        var vertices = buffers.getBuffer(MARKERS);
        for (BlockPos pos : ClientInfo.scryingPositions) {
            double heightDifference = Math.abs(camera.y - pos.getY());
            double y = heightDifference >= 30 ? (int) (camera.y + (pos.getY() > camera.y ? 20 : -20)) : pos.getY();
            int red = heightDifference >= 30 ? 50 : 150 + (pos.hashCode() & 63);
            int green = heightDifference >= 60 ? 80 : 210;
            int blue = heightDifference >= 60 ? 255 : 100;
            poses.pushPose();
            poses.translate(pos.getX() + 0.5 - camera.x, y + 0.2 - camera.y, pos.getZ() + 0.5 - camera.z);
            poses.mulPose(event.getCamera().rotation());
            for (int layer = 0; layer < 2; layer++) {
                float size = layer == 0 ? 0.22f : 0.09f;
                int alpha = layer == 0 ? 65 : 230;
                vertices.addVertex(poses.last().pose(), -size, 0, 0).setColor(red, green, blue, alpha);
                vertices.addVertex(poses.last().pose(), 0, -size, 0).setColor(red, green, blue, alpha);
                vertices.addVertex(poses.last().pose(), size, 0, 0).setColor(red, green, blue, alpha);
                vertices.addVertex(poses.last().pose(), 0, size, 0).setColor(red, green, blue, alpha);
            }
            poses.popPose();
        }
        buffers.endBatch(MARKERS);
    }
}
