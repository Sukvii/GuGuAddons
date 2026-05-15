package com.gugucraft.guguaddons.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class AbyssCatalysisRecipeSerializer implements RecipeSerializer<AbyssCatalysisRecipe> {
    private static final MapCodec<AbyssCatalysisRecipe> CODEC =
            AbyssCatalysisRecipeParams.CODEC.xmap(AbyssCatalysisRecipe::new, AbyssCatalysisRecipe::params);
    private static final StreamCodec<RegistryFriendlyByteBuf, AbyssCatalysisRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> AbyssCatalysisRecipeParams.STREAM_CODEC.encode(buffer, recipe.params()),
            buffer -> new AbyssCatalysisRecipe(AbyssCatalysisRecipeParams.STREAM_CODEC.decode(buffer))
    );

    @Override
    public MapCodec<AbyssCatalysisRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, AbyssCatalysisRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
