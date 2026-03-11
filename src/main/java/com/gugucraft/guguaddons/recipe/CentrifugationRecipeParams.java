package com.gugucraft.guguaddons.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.codec.CreateCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.stream.Stream;

public class CentrifugationRecipeParams extends ProcessingRecipeParams {
    private static final MapCodec<Integer> PROCESSING_TIME_CODEC = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString("processing_time"), ops.createString("processingTime"));
        }

        @Override
        public <T> DataResult<Integer> decode(DynamicOps<T> ops, MapLike<T> input) {
            T snakeCaseValue = input.get("processing_time");
            if (snakeCaseValue != null) {
                return Codec.INT.parse(ops, snakeCaseValue);
            }

            T camelCaseValue = input.get("processingTime");
            if (camelCaseValue != null) {
                return Codec.INT.parse(ops, camelCaseValue);
            }

            return DataResult.success(0);
        }

        @Override
        public <T> RecordBuilder<T> encode(Integer input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            if (input != 0) {
                prefix.add("processing_time", ops.createInt(input));
            }
            return prefix;
        }
    };

    public static final MapCodec<CentrifugationRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.either(CreateCodecs.SIZED_FLUID_INGREDIENT, Ingredient.CODEC).listOf().fieldOf("ingredients")
                    .forGetter(params -> params.ingredients()),
            Codec.either(FluidStack.CODEC, ProcessingOutput.CODEC).listOf().fieldOf("results")
                    .forGetter(params -> params.results()),
            PROCESSING_TIME_CODEC.forGetter(params -> params.processingDuration()),
            HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE)
                    .forGetter(params -> params.requiredHeat()),
            Codec.INT.optionalFieldOf("minimalRPM", 100)
                    .forGetter(CentrifugationRecipeParams::minimalRPM)
    ).apply(instance, (ingredients, results, processingDuration, requiredHeat, minimalRPM) -> {
        CentrifugationRecipeParams params = new CentrifugationRecipeParams();
        ingredients.forEach(either -> either
                .ifRight(params.ingredients::add)
                .ifLeft(params.fluidIngredients::add));
        results.forEach(either -> either
                .ifRight(params.results::add)
                .ifLeft(params.fluidResults::add));
        params.processingDuration = processingDuration;
        params.requiredHeat = requiredHeat;
        params.minimalRPM = minimalRPM;
        return params;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, CentrifugationRecipeParams> STREAM_CODEC = streamCodec(
            CentrifugationRecipeParams::new);

    protected int minimalRPM = 100;

    protected final int minimalRPM() {
        return minimalRPM;
    }

    @Override
    protected void encode(RegistryFriendlyByteBuf buffer) {
        super.encode(buffer);
        ByteBufCodecs.VAR_INT.encode(buffer, minimalRPM);
    }

    @Override
    protected void decode(RegistryFriendlyByteBuf buffer) {
        super.decode(buffer);
        minimalRPM = ByteBufCodecs.VAR_INT.decode(buffer);
    }
}
