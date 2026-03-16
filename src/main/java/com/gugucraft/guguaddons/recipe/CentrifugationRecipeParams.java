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
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.TagFluidIngredient;

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

    // Accept Create's legacy recipe JSON without depending on deprecated codecs.
    private static final Codec<SizedFluidIngredient> LEGACY_FLUID_STACK_INGREDIENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            validatedFluidIngredientType("fluid_stack"),
            FluidStack.FLUID_NON_EMPTY_CODEC.fieldOf("fluid").forGetter(ingredient -> null),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ingredient -> null),
            Codec.INT.fieldOf("amount").forGetter(ingredient -> null)
    ).apply(instance, (type, fluid, components, amount) ->
            new SizedFluidIngredient(DataComponentFluidIngredient.of(false, components.split().added(), fluid), amount)));

    private static final Codec<SizedFluidIngredient> LEGACY_FLUID_TAG_INGREDIENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            validatedFluidIngredientType("fluid_tag"),
            TagKey.codec(Registries.FLUID).fieldOf("fluid_tag").forGetter(ingredient -> null),
            Codec.INT.fieldOf("amount").forGetter(ingredient -> null)
    ).apply(instance, (type, tag, amount) -> new SizedFluidIngredient(TagFluidIngredient.tag(tag), amount)));

    private static final Codec<SizedFluidIngredient> SIZED_FLUID_INGREDIENT_CODEC = Codec.withAlternative(
            CreateCodecs.FLAT_SIZED_FLUID_INGREDIENT_WITH_TYPE,
            Codec.withAlternative(LEGACY_FLUID_STACK_INGREDIENT_CODEC, LEGACY_FLUID_TAG_INGREDIENT_CODEC)
    );

    private static final Codec<ProcessingOutput> LEGACY_PROCESSING_OUTPUT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(output -> {
                ItemStack stack = output.getStack();
                stack.setCount(1);
                return stack;
            }),
            ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(output -> output.getStack().getCount()),
            ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("chance", 1F).forGetter(ProcessingOutput::getChance)
    ).apply(instance, (stack, count, chance) ->
            new ProcessingOutput(stack.getItem(), count, stack.getComponentsPatch(), chance)));

    private static final Codec<ProcessingOutput> PROCESSING_OUTPUT_CODEC = Codec.withAlternative(
            ProcessingOutput.CODEC_NEW,
            LEGACY_PROCESSING_OUTPUT_CODEC
    );

    public static final MapCodec<CentrifugationRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.either(SIZED_FLUID_INGREDIENT_CODEC, Ingredient.CODEC).listOf().fieldOf("ingredients")
                    .forGetter(params -> params.ingredients()),
            Codec.either(FluidStack.CODEC, PROCESSING_OUTPUT_CODEC).listOf().fieldOf("results")
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

    private static <T> RecordCodecBuilder<T, String> validatedFluidIngredientType(String requiredType) {
        return Codec.STRING
                .validate(type -> type.equals(requiredType)
                        ? DataResult.success(type)
                        : DataResult.error(() -> "Invalid Type: " + type))
                .fieldOf("type")
                .forGetter(params -> requiredType);
    }
}
