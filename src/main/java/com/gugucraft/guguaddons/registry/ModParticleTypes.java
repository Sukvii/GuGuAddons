package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.particle.MechanicalShriekerParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, GuGuAddons.MODID);

    public static final Supplier<ParticleType<MechanicalShriekerParticleOptions>> MECHANICAL_SHRIEKER =
            PARTICLES.register("mechanical_shrieker", () -> new ParticleType<>(false) {
                @Override
                public MapCodec<MechanicalShriekerParticleOptions> codec() {
                    return MechanicalShriekerParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, MechanicalShriekerParticleOptions> streamCodec() {
                    return MechanicalShriekerParticleOptions.STREAM_CODEC;
                }
            });

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
