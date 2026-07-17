package com.gugucraft.guguaddons.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.stream.Stream;

public record AbyssCatalysisRecipeParams(
        List<AbyssCatalysisRecipeIngredient> topIngredients,
        List<AbyssCatalysisRecipeIngredient> bottomIngredients,
        List<AbyssCatalysisRecipeIngredient> catalysts,
        float chances,
        List<AbyssCatalysisRecipeResult> results,
        HeatCondition heatRequirement
) {
    private static final int MAX_LIST_SIZE = 64;

    private static final Codec<Float> CHANCE_CODEC = Codec.FLOAT.validate(value ->
            isValidChance(value)
                    ? DataResult.success(value)
                    : DataResult.error(() -> "Chance must be between 0 and 1"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<AbyssCatalysisRecipeIngredient>> INGREDIENT_LIST_STREAM_CODEC =
            AbyssCatalysisRecipeIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LIST_SIZE));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<AbyssCatalysisRecipeResult>> RESULT_LIST_STREAM_CODEC =
            AbyssCatalysisRecipeResult.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LIST_SIZE));

    private static final MapCodec<HeatCondition> HEAT_REQUIREMENT_CODEC = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString("heatRequirement"), ops.createString("heat_requirement"));
        }

        @Override
        public <T> DataResult<HeatCondition> decode(DynamicOps<T> ops, MapLike<T> input) {
            T camelCaseValue = input.get("heatRequirement");
            if (camelCaseValue != null) {
                return HeatCondition.CODEC.parse(ops, camelCaseValue);
            }

            T snakeCaseValue = input.get("heat_requirement");
            if (snakeCaseValue != null) {
                return HeatCondition.CODEC.parse(ops, snakeCaseValue);
            }

            return DataResult.success(HeatCondition.NONE);
        }

        @Override
        public <T> RecordBuilder<T> encode(HeatCondition input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            if (input != HeatCondition.NONE) {
                prefix.add("heatRequirement", ops.createString(input.getSerializedName()));
            }
            return prefix;
        }
    };

    public static final MapCodec<AbyssCatalysisRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AbyssCatalysisRecipeIngredient.CODEC.sizeLimitedListOf(MAX_LIST_SIZE).optionalFieldOf("topIngredients", List.of())
                    .forGetter(AbyssCatalysisRecipeParams::topIngredients),
            AbyssCatalysisRecipeIngredient.CODEC.sizeLimitedListOf(MAX_LIST_SIZE).optionalFieldOf("bottomIngredients", List.of())
                    .forGetter(AbyssCatalysisRecipeParams::bottomIngredients),
            AbyssCatalysisRecipeIngredient.CODEC.sizeLimitedListOf(MAX_LIST_SIZE).optionalFieldOf("catalysts", List.of())
                    .forGetter(AbyssCatalysisRecipeParams::catalysts),
            CHANCE_CODEC.optionalFieldOf("chances", 1F).forGetter(AbyssCatalysisRecipeParams::chances),
            AbyssCatalysisRecipeResult.CODEC.sizeLimitedListOf(MAX_LIST_SIZE).optionalFieldOf("results", List.of())
                    .forGetter(AbyssCatalysisRecipeParams::results),
            HEAT_REQUIREMENT_CODEC.forGetter(AbyssCatalysisRecipeParams::heatRequirement)
    ).apply(instance, AbyssCatalysisRecipeParams::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbyssCatalysisRecipeParams> STREAM_CODEC = StreamCodec.of(
            (buffer, params) -> {
                INGREDIENT_LIST_STREAM_CODEC.encode(buffer, params.topIngredients());
                INGREDIENT_LIST_STREAM_CODEC.encode(buffer, params.bottomIngredients());
                INGREDIENT_LIST_STREAM_CODEC.encode(buffer, params.catalysts());
                if (!isValidChance(params.chances())) {
                    throw new io.netty.handler.codec.EncoderException("Chance must be finite and between 0 and 1");
                }
                ByteBufCodecs.FLOAT.encode(buffer, params.chances());
                RESULT_LIST_STREAM_CODEC.encode(buffer, params.results());
                HeatCondition.STREAM_CODEC.encode(buffer, params.heatRequirement());
            },
            buffer -> new AbyssCatalysisRecipeParams(
                    INGREDIENT_LIST_STREAM_CODEC.decode(buffer),
                    INGREDIENT_LIST_STREAM_CODEC.decode(buffer),
                    INGREDIENT_LIST_STREAM_CODEC.decode(buffer),
                    decodeChance(buffer),
                    RESULT_LIST_STREAM_CODEC.decode(buffer),
                    HeatCondition.STREAM_CODEC.decode(buffer))
    );

    public AbyssCatalysisRecipeParams {
        topIngredients = List.copyOf(topIngredients);
        bottomIngredients = List.copyOf(bottomIngredients);
        catalysts = List.copyOf(catalysts);
        results = List.copyOf(results);
        if (!isValidChance(chances)) {
            throw new IllegalArgumentException("Chance must be finite and between 0 and 1");
        }
        if (heatRequirement == null) {
            heatRequirement = HeatCondition.NONE;
        }
    }

    private static float decodeChance(RegistryFriendlyByteBuf buffer) {
        float chance = ByteBufCodecs.FLOAT.decode(buffer);
        if (!isValidChance(chance)) {
            throw new io.netty.handler.codec.DecoderException("Chance must be finite and between 0 and 1");
        }
        return chance;
    }

    private static boolean isValidChance(float chance) {
        return Float.isFinite(chance) && chance >= 0F && chance <= 1F;
    }
}
