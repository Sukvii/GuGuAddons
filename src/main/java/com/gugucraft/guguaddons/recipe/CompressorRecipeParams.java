package com.gugucraft.guguaddons.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Function;

public class CompressorRecipeParams extends ProcessingRecipeParams {
    public static final MapCodec<CompressorRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            codec(CompressorRecipeParams::new).forGetter(Function.identity()),
            Codec.INT.optionalFieldOf("secondaryFluidOutput", -1)
                    .forGetter(CompressorRecipeParams::secondaryFluidOutput),
            Codec.INT.optionalFieldOf("secondaryFluidInput", -1)
                    .forGetter(CompressorRecipeParams::secondaryFluidInput)
    ).apply(instance, (params, secondaryFluidOutput, secondaryFluidInput) -> {
        params.secondaryFluidOutput = secondaryFluidOutput;
        params.secondaryFluidInput = secondaryFluidInput;
        return params;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompressorRecipeParams> STREAM_CODEC = streamCodec(
            CompressorRecipeParams::new);

    protected int secondaryFluidOutput = -1;
    protected int secondaryFluidInput = -1;

    protected final int secondaryFluidOutput() {
        return secondaryFluidOutput;
    }

    protected final int secondaryFluidInput() {
        return secondaryFluidInput;
    }

    @Override
    protected void encode(RegistryFriendlyByteBuf buffer) {
        super.encode(buffer);
        ByteBufCodecs.INT.encode(buffer, secondaryFluidOutput);
        ByteBufCodecs.INT.encode(buffer, secondaryFluidInput);
    }

    @Override
    protected void decode(RegistryFriendlyByteBuf buffer) {
        super.decode(buffer);
        secondaryFluidOutput = ByteBufCodecs.INT.decode(buffer);
        secondaryFluidInput = ByteBufCodecs.INT.decode(buffer);
    }
}
