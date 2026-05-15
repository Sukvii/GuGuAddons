package com.gugucraft.guguaddons.client.particle;

import com.gugucraft.guguaddons.particle.MechanicalShriekerParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public class MechanicalShriekerParticle extends TextureSheetParticle {
    private int delay;
    private final Vec3i normal;

    protected MechanicalShriekerParticle(ClientLevel level, double x, double y, double z, double xSpeed,
            double ySpeed, double zSpeed, int delay, Direction direction, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.delay = delay;
        this.normal = direction.getNormal();
        lifetime = 90;
        quadSize = .85f;
        gravity = 0;
        hasPhysics = false;
        alpha = 1;
        xd = normal.getX() * .1d;
        yd = normal.getY() * .1d;
        zd = normal.getZ() * .1d;
        pickSprite(sprites);
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        return quadSize * Mth.clamp(((age + scaleFactor) * 2) / 45, 0, 1);
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        if (delay > 0) {
            return;
        }

        alpha = 1 - Mth.clamp((age + partialTicks) / lifetime, 0, 1);

        Quaternionf first = new Quaternionf().rotateXYZ(
                (float) (-Math.PI / 2) * (normal.getX() - 1),
                0,
                (float) (-Math.PI / 2) * normal.getZ());
        renderRotatedQuad(buffer, renderInfo, first, partialTicks);

        Quaternionf second = new Quaternionf().rotateXYZ(
                (float) (Math.PI / 2) * (normal.getX() + 1),
                (float) -Math.PI,
                (float) (Math.PI / 2) * normal.getZ());
        renderRotatedQuad(buffer, renderInfo, second, partialTicks);
    }

    @Override
    public void tick() {
        if (delay > 0) {
            delay--;
        } else {
            super.tick();
        }
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 240;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<MechanicalShriekerParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(MechanicalShriekerParticleOptions type, ClientLevel level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new MechanicalShriekerParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, type.delay(),
                    type.direction(), sprites);
        }
    }
}
