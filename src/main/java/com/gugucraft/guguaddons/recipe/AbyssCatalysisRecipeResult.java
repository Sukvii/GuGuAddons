package com.gugucraft.guguaddons.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public record AbyssCatalysisRecipeResult(Either<FluidStack, ProcessingOutput> value) {
    private static final Codec<FluidStack> LEGACY_FLUID_OUTPUT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.FLUID_NON_EMPTY_CODEC.fieldOf("fluid").forGetter(FluidStack::getFluidHolder),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidStack::getAmount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(stack -> DataComponentPatch.EMPTY)
    ).apply(instance, FluidStack::new));

    private static final Codec<FluidStack> FLUID_OUTPUT_CODEC = Codec.withAlternative(
            FluidStack.CODEC,
            LEGACY_FLUID_OUTPUT_CODEC
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

    public static final Codec<AbyssCatalysisRecipeResult> CODEC = Codec.either(
            FLUID_OUTPUT_CODEC,
            PROCESSING_OUTPUT_CODEC
    ).xmap(AbyssCatalysisRecipeResult::new, AbyssCatalysisRecipeResult::value);

    public static final StreamCodec<RegistryFriendlyByteBuf, AbyssCatalysisRecipeResult> STREAM_CODEC = StreamCodec.of(
            (buffer, result) -> {
                FluidStack fluidStack = result.fluidResult();
                buffer.writeBoolean(fluidStack != null);
                if (fluidStack != null) {
                    FluidStack.STREAM_CODEC.encode(buffer, fluidStack);
                } else {
                    ProcessingOutput.STREAM_CODEC.encode(buffer, result.itemResult());
                }
            },
            buffer -> buffer.readBoolean()
                    ? fluid(FluidStack.STREAM_CODEC.decode(buffer))
                    : item(ProcessingOutput.STREAM_CODEC.decode(buffer))
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
}
