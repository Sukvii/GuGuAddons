package com.gugucraft.guguaddons.particle;

import com.gugucraft.guguaddons.registry.ModParticleTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MechanicalShriekerParticleOptions(int delay, Direction direction) implements ParticleOptions {
    public static final MapCodec<MechanicalShriekerParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.INT.fieldOf("delay").forGetter(MechanicalShriekerParticleOptions::delay),
                    Direction.CODEC.fieldOf("direction").forGetter(MechanicalShriekerParticleOptions::direction)
            ).apply(instance, MechanicalShriekerParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MechanicalShriekerParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, MechanicalShriekerParticleOptions::delay,
                    Direction.STREAM_CODEC, MechanicalShriekerParticleOptions::direction,
                    MechanicalShriekerParticleOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticleTypes.MECHANICAL_SHRIEKER.get();
    }
}
