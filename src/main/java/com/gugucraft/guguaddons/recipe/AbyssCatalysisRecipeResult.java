package com.gugucraft.guguaddons.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public record AbyssCatalysisRecipeResult(Either<FluidStack, ProcessingOutput> value) {
    private static final int MIN_FLUID_AMOUNT = 1;
    private static final int MAX_FLUID_AMOUNT = 1_000_000;

    private static final Codec<FluidStack> LEGACY_FLUID_OUTPUT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.FLUID_NON_EMPTY_CODEC.fieldOf("fluid").forGetter(FluidStack::getFluidHolder),
            ExtraCodecs.intRange(MIN_FLUID_AMOUNT, MAX_FLUID_AMOUNT).fieldOf("amount").forGetter(FluidStack::getAmount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(stack -> DataComponentPatch.EMPTY)
    ).apply(instance, FluidStack::new));

    private static final Codec<FluidStack> FLUID_OUTPUT_CODEC = Codec.withAlternative(
            FluidStack.CODEC,
            LEGACY_FLUID_OUTPUT_CODEC
    ).validate(AbyssCatalysisRecipeResult::validateFluidStackAmount);

    private static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> FLUID_OUTPUT_STREAM_CODEC = StreamCodec.of(
            (buffer, stack) -> {
                if (!isFluidAmountInRange(stack.getAmount())) {
                    throw new EncoderException("Fluid output amount must be between " + MIN_FLUID_AMOUNT + " and "
                            + MAX_FLUID_AMOUNT);
                }
                FluidStack.STREAM_CODEC.encode(buffer, stack);
            },
            buffer -> {
                FluidStack stack;
                try {
                    stack = FluidStack.STREAM_CODEC.decode(buffer);
                } catch (IllegalArgumentException exception) {
                    throw new DecoderException("Invalid fluid output amount", exception);
                }
                if (!isFluidAmountInRange(stack.getAmount())) {
                    throw new DecoderException("Fluid output amount must be between " + MIN_FLUID_AMOUNT + " and "
                            + MAX_FLUID_AMOUNT);
                }
                return stack;
            });

    private static final StreamCodec<RegistryFriendlyByteBuf, ProcessingOutput> PROCESSING_OUTPUT_STREAM_CODEC =
            StreamCodec.of(
                    (buffer, output) -> {
                        if (validateProcessingOutput(output).error().isPresent()) {
                            throw new EncoderException("Invalid processing output");
                        }
                        ProcessingOutput.STREAM_CODEC.encode(buffer, output);
                    },
                    buffer -> {
                        ProcessingOutput output;
                        try {
                            output = ProcessingOutput.STREAM_CODEC.decode(buffer);
                        } catch (IllegalArgumentException exception) {
                            throw new DecoderException("Invalid processing output", exception);
                        }
                        if (validateProcessingOutput(output).error().isPresent()) {
                            throw new DecoderException("Invalid processing output");
                        }
                        return output;
                    });

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
    ).validate(AbyssCatalysisRecipeResult::validateProcessingOutput);

    public static final Codec<AbyssCatalysisRecipeResult> CODEC = Codec.either(
            FLUID_OUTPUT_CODEC,
            PROCESSING_OUTPUT_CODEC
    ).xmap(AbyssCatalysisRecipeResult::new, AbyssCatalysisRecipeResult::value);

    public static final StreamCodec<RegistryFriendlyByteBuf, AbyssCatalysisRecipeResult> STREAM_CODEC = StreamCodec.of(
            (buffer, result) -> {
                FluidStack fluidStack = result.fluidResult();
                buffer.writeBoolean(fluidStack != null);
                if (fluidStack != null) {
                    FLUID_OUTPUT_STREAM_CODEC.encode(buffer, fluidStack);
                } else {
                    PROCESSING_OUTPUT_STREAM_CODEC.encode(buffer, result.itemResult());
                }
            },
            buffer -> buffer.readBoolean()
                    ? fluid(FLUID_OUTPUT_STREAM_CODEC.decode(buffer))
                    : item(PROCESSING_OUTPUT_STREAM_CODEC.decode(buffer))
    );

    public static AbyssCatalysisRecipeResult fluid(FluidStack result) {
        return new AbyssCatalysisRecipeResult(Either.left(result));
    }

    public static AbyssCatalysisRecipeResult item(ProcessingOutput result) {
        return new AbyssCatalysisRecipeResult(Either.right(result));
    }

    public boolean isFluid() {
        return value.left().isPresent();
    }

    public FluidStack fluidResult() {
        return value.left().orElse(null);
    }

    public ProcessingOutput itemResult() {
        return value.right().orElse(ProcessingOutput.EMPTY);
    }

    private static DataResult<FluidStack> validateFluidStackAmount(FluidStack stack) {
        return isFluidAmountInRange(stack.getAmount())
                ? DataResult.success(stack)
                : DataResult.error(() -> "Fluid output amount must be between " + MIN_FLUID_AMOUNT + " and "
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

    private static boolean isFluidAmountInRange(int amount) {
        return amount >= MIN_FLUID_AMOUNT && amount <= MAX_FLUID_AMOUNT;
    }
}
