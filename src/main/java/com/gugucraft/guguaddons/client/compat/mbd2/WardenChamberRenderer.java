package com.gugucraft.guguaddons.client.compat.mbd2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.gugucraft.guguaddons.GuGuAddons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = GuGuAddons.MODID, value = Dist.CLIENT)
public final class WardenChamberRenderer {
    private static final Map<BlockEntity, DisplayWarden> WARDENS = new WeakHashMap<>();

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            WARDENS.clear();
        }
    }

    // MBD2 calls this with a pose whose origin is the containment jar's block position.
    public static void render(BlockEntity anchor, int agitation, boolean roaring, float yaw,
                              float partialTick, PoseStack poses, MultiBufferSource buffers, int packedLight) {
        DisplayWarden warden = WARDENS.computeIfAbsent(anchor, DisplayWarden::new);
        warden.agitation = agitation;
        warden.setPose(roaring ? Pose.ROARING : Pose.STANDING);
        if (!roaring) {
            warden.roarAnimationState.stop();
        }
        if (warden.lastTick != anchor.getLevel().getGameTime()) {
            warden.lastTick = anchor.getLevel().getGameTime();
            warden.tickCount++;
            warden.tick();
            warden.setPos(Vec3.atBottomCenterOf(anchor.getBlockPos()));
            warden.setDeltaMovement(Vec3.ZERO);
        }
        poses.pushPose();
        poses.translate(0.5, 0, 0.5);
        poses.mulPose(Axis.YP.rotationDegrees(yaw));
        poses.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(warden)
                .render(warden, 0, partialTick, poses, buffers, packedLight);
        poses.popPose();
    }

    private static final class DisplayWarden extends Warden {
        private int agitation;
        private long lastTick = -1;

        private DisplayWarden(BlockEntity anchor) {
            super(EntityType.WARDEN, anchor.getLevel());
            setNoAi(true);
            setNoGravity(true);
            setSilent(true);
            setPos(Vec3.atBottomCenterOf(anchor.getBlockPos()));
        }

        @Override
        public int getClientAngerLevel() {
            return agitation;
        }

        @Override
        public float getTendrilAnimation(float partialTick) {
            return getHeartAnimation(partialTick);
        }
    }
}
