package com.gugucraft.guguaddons.recipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Function;

public class CentrifugationRecipeSerializer<R extends CentrifugationRecipe> implements RecipeSerializer<R> {
    private final MapCodec<R> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

    @SuppressWarnings("unchecked")
    public CentrifugationRecipeSerializer(Function<CentrifugationRecipeParams, R> factory) {
        ProcessingRecipe.Factory<CentrifugationRecipeParams, R> wrappedFactory = factory::apply;
        MapCodec<CentrifugationRecipeParams> paramsCodec =
                (MapCodec<CentrifugationRecipeParams>) (MapCodec<?>) CentrifugationRecipeParams.CODEC;
        StreamCodec<RegistryFriendlyByteBuf, CentrifugationRecipeParams> paramsStreamCodec =
                (StreamCodec<RegistryFriendlyByteBuf, CentrifugationRecipeParams>) (StreamCodec<?, ?>) CentrifugationRecipeParams.STREAM_CODEC;
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
