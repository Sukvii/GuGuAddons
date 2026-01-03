package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.world.gen.GuGuDungeonChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister
            .create(Registries.CHUNK_GENERATOR, GuGuAddons.MODID);

    public static final Supplier<MapCodec<GuGuDungeonChunkGenerator>> DUNGEON_CHUNK_GENERATOR = CHUNK_GENERATORS
            .register("dungeon_generator", () -> GuGuDungeonChunkGenerator.CODEC);

    public static void register(IEventBus eventBus) {
        CHUNK_GENERATORS.register(eventBus);
    }
}
