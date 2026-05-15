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

import java.util.ArrayList;
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
    private static final Codec<Float> CHANCE_CODEC = Codec.FLOAT.validate(value ->
            value >= 0F && value <= 1F
                    ? DataResult.success(value)
                    : DataResult.error(() -> "Chance must be between 0 and 1"));

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
            AbyssCatalysisRecipeIngredient.CODEC.listOf().optionalFieldOf("topIngredients", List.of())
                    .forGetter(AbyssCatalysisRecipeParams::topIngredients),
            AbyssCatalysisRecipeIngredient.CODEC.listOf().optionalFieldOf("bottomIngredients", List.of())
                    .forGetter(AbyssCatalysisRecipeParams::bottomIngredients),
            AbyssCatalysisRecipeIngredient.CODEC.listOf().optionalFieldOf("catalysts", List.of())
                    .forGetter(AbyssCatalysisRecipeParams::catalysts),
            CHANCE_CODEC.optionalFieldOf("chances", 1F).forGetter(AbyssCatalysisRecipeParams::chances),
            AbyssCatalysisRecipeResult.CODEC.listOf().optionalFieldOf("results", List.of())
                    .forGetter(AbyssCatalysisRecipeParams::results),
            HEAT_REQUIREMENT_CODEC.forGetter(AbyssCatalysisRecipeParams::heatRequirement)
    ).apply(instance, AbyssCatalysisRecipeParams::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbyssCatalysisRecipeParams> STREAM_CODEC = StreamCodec.of(
            (buffer, params) -> {
                writeList(buffer, params.topIngredients(), AbyssCatalysisRecipeIngredient.STREAM_CODEC);
                writeList(buffer, params.bottomIngredients(), AbyssCatalysisRecipeIngredient.STREAM_CODEC);
                writeList(buffer, params.catalysts(), AbyssCatalysisRecipeIngredient.STREAM_CODEC);
                ByteBufCodecs.FLOAT.encode(buffer, params.chances());
                writeList(buffer, params.results(), AbyssCatalysisRecipeResult.STREAM_CODEC);
                HeatCondition.STREAM_CODEC.encode(buffer, params.heatRequirement());
            },
            buffer -> new AbyssCatalysisRecipeParams(
                    readList(buffer, AbyssCatalysisRecipeIngredient.STREAM_CODEC),
                    readList(buffer, AbyssCatalysisRecipeIngredient.STREAM_CODEC),
                    readList(buffer, AbyssCatalysisRecipeIngredient.STREAM_CODEC),
                    ByteBufCodecs.FLOAT.decode(buffer),
                    readList(buffer, AbyssCatalysisRecipeResult.STREAM_CODEC),
                    HeatCondition.STREAM_CODEC.decode(buffer))
    );

    public AbyssCatalysisRecipeParams {
        topIngredients = List.copyOf(topIngredients);
        bottomIngredients = List.copyOf(bottomIngredients);
        catalysts = List.copyOf(catalysts);
        results = List.copyOf(results);
        if (heatRequirement == null) {
            heatRequirement = HeatCondition.NONE;
        }
    }

    private static <T> void writeList(RegistryFriendlyByteBuf buffer, List<T> list,
                                      StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        ByteBufCodecs.VAR_INT.encode(buffer, list.size());
        for (T value : list) {
            codec.encode(buffer, value);
        }
    }

    private static <T> List<T> readList(RegistryFriendlyByteBuf buffer,
                                        StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        int size = ByteBufCodecs.VAR_INT.decode(buffer);
        List<T> values = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            values.add(codec.decode(buffer));
        }
        return values;
    }
}
