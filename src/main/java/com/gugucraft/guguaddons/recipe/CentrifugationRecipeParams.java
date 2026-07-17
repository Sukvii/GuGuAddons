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
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.NonNullList;
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
    private static final int MAX_LIST_SIZE = 64;
    private static final int MIN_FLUID_AMOUNT = 1;
    private static final int MAX_FLUID_AMOUNT = 1_000_000;
    private static final int MIN_MINIMAL_RPM = 1;
    private static final int MAX_MINIMAL_RPM = 1_000_000;

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
            ExtraCodecs.intRange(MIN_FLUID_AMOUNT, MAX_FLUID_AMOUNT).fieldOf("amount").forGetter(ingredient -> null)
    ).apply(instance, (type, fluid, components, amount) ->
            new SizedFluidIngredient(DataComponentFluidIngredient.of(false, components.split().added(), fluid), amount)));

    private static final Codec<SizedFluidIngredient> LEGACY_FLUID_TAG_INGREDIENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            validatedFluidIngredientType("fluid_tag"),
            TagKey.codec(Registries.FLUID).fieldOf("fluid_tag").forGetter(ingredient -> null),
            ExtraCodecs.intRange(MIN_FLUID_AMOUNT, MAX_FLUID_AMOUNT).fieldOf("amount").forGetter(ingredient -> null)
    ).apply(instance, (type, tag, amount) -> new SizedFluidIngredient(TagFluidIngredient.tag(tag), amount)));

    private static final Codec<SizedFluidIngredient> SIZED_FLUID_INGREDIENT_CODEC = Codec.withAlternative(
            CreateCodecs.FLAT_SIZED_FLUID_INGREDIENT_WITH_TYPE,
            Codec.withAlternative(LEGACY_FLUID_STACK_INGREDIENT_CODEC, LEGACY_FLUID_TAG_INGREDIENT_CODEC)
    ).validate(CentrifugationRecipeParams::validateFluidIngredientAmount);

    private static final Codec<FluidStack> FLUID_RESULT_CODEC = FluidStack.CODEC
            .validate(CentrifugationRecipeParams::validateFluidStackAmount);

    private static final StreamCodec<RegistryFriendlyByteBuf, SizedFluidIngredient> SIZED_FLUID_INGREDIENT_STREAM_CODEC =
            boundedStreamCodec(SizedFluidIngredient.STREAM_CODEC,
                    CentrifugationRecipeParams::validateFluidIngredientAmount,
                    "fluid ingredient");

    private static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> FLUID_RESULT_STREAM_CODEC =
            boundedStreamCodec(FluidStack.STREAM_CODEC, CentrifugationRecipeParams::validateFluidStackAmount,
                    "fluid result");

    private static final StreamCodec<RegistryFriendlyByteBuf, ProcessingOutput> PROCESSING_OUTPUT_STREAM_CODEC =
            boundedStreamCodec(ProcessingOutput.STREAM_CODEC, CentrifugationRecipeParams::validateProcessingOutput,
                    "processing output");

    private static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<Ingredient>> INGREDIENTS_STREAM_CODEC =
            CatnipStreamCodecBuilders.nonNullList(Ingredient.CONTENTS_STREAM_CODEC, MAX_LIST_SIZE);

    private static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<SizedFluidIngredient>> FLUID_INGREDIENTS_STREAM_CODEC =
            CatnipStreamCodecBuilders.nonNullList(SIZED_FLUID_INGREDIENT_STREAM_CODEC, MAX_LIST_SIZE);

    private static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<ProcessingOutput>> RESULTS_STREAM_CODEC =
            CatnipStreamCodecBuilders.nonNullList(PROCESSING_OUTPUT_STREAM_CODEC, MAX_LIST_SIZE);

    private static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<FluidStack>> FLUID_RESULTS_STREAM_CODEC =
            CatnipStreamCodecBuilders.nonNullList(FLUID_RESULT_STREAM_CODEC, MAX_LIST_SIZE);

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
    ).validate(CentrifugationRecipeParams::validateProcessingOutput);

    public static final MapCodec<CentrifugationRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.either(SIZED_FLUID_INGREDIENT_CODEC, Ingredient.CODEC).sizeLimitedListOf(MAX_LIST_SIZE).fieldOf("ingredients")
                    .forGetter(params -> params.ingredients()),
            Codec.either(FLUID_RESULT_CODEC, PROCESSING_OUTPUT_CODEC).sizeLimitedListOf(MAX_LIST_SIZE).fieldOf("results")
                    .forGetter(params -> params.results()),
            PROCESSING_TIME_CODEC.forGetter(params -> params.processingDuration()),
            HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE)
                    .forGetter(params -> params.requiredHeat()),
            ExtraCodecs.intRange(MIN_MINIMAL_RPM, MAX_MINIMAL_RPM).optionalFieldOf("minimalRPM", 100)
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
        if (ingredients.size() + fluidIngredients.size() > MAX_LIST_SIZE
                || results.size() + fluidResults.size() > MAX_LIST_SIZE) {
            throw new EncoderException("Centrifugation recipe has too many ingredients or results");
        }
        if (!isMinimalRPMInRange(minimalRPM)) {
            throw new EncoderException("minimalRPM must be between " + MIN_MINIMAL_RPM + " and " + MAX_MINIMAL_RPM);
        }

        INGREDIENTS_STREAM_CODEC.encode(buffer, ingredients);
        FLUID_INGREDIENTS_STREAM_CODEC.encode(buffer, fluidIngredients);
        RESULTS_STREAM_CODEC.encode(buffer, results);
        FLUID_RESULTS_STREAM_CODEC.encode(buffer, fluidResults);
        ByteBufCodecs.VAR_INT.encode(buffer, processingDuration);
        HeatCondition.STREAM_CODEC.encode(buffer, requiredHeat);
        ByteBufCodecs.VAR_INT.encode(buffer, minimalRPM);
    }

    @Override
    protected void decode(RegistryFriendlyByteBuf buffer) {
        ingredients = INGREDIENTS_STREAM_CODEC.decode(buffer);
        fluidIngredients = FLUID_INGREDIENTS_STREAM_CODEC.decode(buffer);
        results = RESULTS_STREAM_CODEC.decode(buffer);
        fluidResults = FLUID_RESULTS_STREAM_CODEC.decode(buffer);
        if (ingredients.size() + fluidIngredients.size() > MAX_LIST_SIZE
                || results.size() + fluidResults.size() > MAX_LIST_SIZE) {
            throw new DecoderException("Centrifugation recipe has too many ingredients or results");
        }

        processingDuration = ByteBufCodecs.VAR_INT.decode(buffer);
        requiredHeat = HeatCondition.STREAM_CODEC.decode(buffer);
        minimalRPM = ByteBufCodecs.VAR_INT.decode(buffer);
        if (!isMinimalRPMInRange(minimalRPM)) {
            throw new DecoderException("minimalRPM must be between " + MIN_MINIMAL_RPM + " and " + MAX_MINIMAL_RPM);
        }
    }

    private static <T> RecordCodecBuilder<T, String> validatedFluidIngredientType(String requiredType) {
        return Codec.STRING
                .validate(type -> type.equals(requiredType)
                        ? DataResult.success(type)
                        : DataResult.error(() -> "Invalid Type: " + type))
                .fieldOf("type")
                .forGetter(params -> requiredType);
    }

    private static DataResult<SizedFluidIngredient> validateFluidIngredientAmount(SizedFluidIngredient ingredient) {
        return isFluidAmountInRange(ingredient.amount())
                ? DataResult.success(ingredient)
                : DataResult.error(() -> "Fluid ingredient amount must be between " + MIN_FLUID_AMOUNT + " and "
                        + MAX_FLUID_AMOUNT);
    }

    private static DataResult<FluidStack> validateFluidStackAmount(FluidStack stack) {
        return isFluidAmountInRange(stack.getAmount())
                ? DataResult.success(stack)
                : DataResult.error(() -> "Fluid result amount must be between " + MIN_FLUID_AMOUNT + " and "
                        + MAX_FLUID_AMOUNT);
    }

    private static DataResult<ProcessingOutput> validateProcessingOutput(ProcessingOutput output) {
        ItemStack stack = output.getStack();
        if (stack.getCount() < 1 || stack.getCount() > 99) {
            return DataResult.error(() -> "Processing output count must be between 1 and 99");
        }
        if (!Float.isFinite(output.getChance()) || output.getChance() <= 0F || output.getChance() > 1F) {
            return DataResult.error(() -> "Processing output chance must be finite and between 0 and 1");
        }
        return DataResult.success(output);
    }

    private static <T> StreamCodec<RegistryFriendlyByteBuf, T> boundedStreamCodec(
            StreamCodec<RegistryFriendlyByteBuf, T> delegate,
            java.util.function.Function<T, DataResult<T>> validator, String description) {
        return StreamCodec.of(
                (buffer, value) -> {
                    if (validator.apply(value).error().isPresent()) {
                        throw new EncoderException("Invalid " + description);
                    }
                    delegate.encode(buffer, value);
                },
                buffer -> {
                    T value;
                    try {
                        value = delegate.decode(buffer);
                    } catch (IllegalArgumentException exception) {
                        throw new DecoderException("Invalid " + description, exception);
                    }
                    if (validator.apply(value).error().isPresent()) {
                        throw new DecoderException("Invalid " + description);
                    }
                    return value;
                });
    }

    private static boolean isFluidAmountInRange(int amount) {
        return amount >= MIN_FLUID_AMOUNT && amount <= MAX_FLUID_AMOUNT;
    }

    private static boolean isMinimalRPMInRange(int rpm) {
        return rpm >= MIN_MINIMAL_RPM && rpm <= MAX_MINIMAL_RPM;
    }
}
