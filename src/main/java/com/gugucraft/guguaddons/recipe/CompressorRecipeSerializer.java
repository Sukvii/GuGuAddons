package com.gugucraft.guguaddons.recipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Function;

public class CompressorRecipeSerializer<R extends CompressorRecipe> implements RecipeSerializer<R> {
    private final MapCodec<R> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

    @SuppressWarnings("unchecked")
    public CompressorRecipeSerializer(Function<CompressorRecipeParams, R> factory) {
        ProcessingRecipe.Factory<ProcessingRecipeParams, R> wrappedFactory =
                params -> factory.apply((CompressorRecipeParams) params);
        MapCodec<ProcessingRecipeParams> paramsCodec =
                (MapCodec<ProcessingRecipeParams>) (MapCodec<?>) CompressorRecipeParams.CODEC;
        StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipeParams> paramsStreamCodec =
                (StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipeParams>) (StreamCodec<?, ?>) CompressorRecipeParams.STREAM_CODEC;
        this.codec = ProcessingRecipe.codec(wrappedFactory, paramsCodec);
        this.streamCodec = ProcessingRecipe.streamCodec(wrappedFactory, paramsStreamCodec);
    }

    @Override
    public MapCodec<R> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
        return streamCodec;
    }
}
