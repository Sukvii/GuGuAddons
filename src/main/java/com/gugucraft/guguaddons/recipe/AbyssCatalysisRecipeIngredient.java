package com.gugucraft.guguaddons.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.codec.CreateCodecs;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.TagFluidIngredient;

public record AbyssCatalysisRecipeIngredient(Either<SizedFluidIngredient, Ingredient> value) {
    private static final int MIN_FLUID_AMOUNT = 1;
    private static final int MAX_FLUID_AMOUNT = 1_000_000;

    private static final Codec<SizedFluidIngredient> FLUID_STACK_INGREDIENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.FLUID_NON_EMPTY_CODEC.fieldOf("fluid").forGetter(ingredient -> null),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ingredient -> null),
            ExtraCodecs.intRange(MIN_FLUID_AMOUNT, MAX_FLUID_AMOUNT).fieldOf("amount").forGetter(ingredient -> null)
    ).apply(instance, (fluid, components, amount) ->
            new SizedFluidIngredient(DataComponentFluidIngredient.of(false, components.split().added(), fluid), amount)));

    private static final Codec<SizedFluidIngredient> FLUID_TAG_INGREDIENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.codec(Registries.FLUID).fieldOf("tag").forGetter(ingredient -> null),
            ExtraCodecs.intRange(MIN_FLUID_AMOUNT, MAX_FLUID_AMOUNT).fieldOf("amount").forGetter(ingredient -> null)
    ).apply(instance, AbyssCatalysisRecipeIngredient::fluidTagIngredient));

    private static final Codec<SizedFluidIngredient> LEGACY_FLUID_TAG_INGREDIENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.codec(Registries.FLUID).fieldOf("fluid_tag").forGetter(ingredient -> null),
            ExtraCodecs.intRange(MIN_FLUID_AMOUNT, MAX_FLUID_AMOUNT).fieldOf("amount").forGetter(ingredient -> null)
    ).apply(instance, AbyssCatalysisRecipeIngredient::fluidTagIngredient));

    private static final Codec<SizedFluidIngredient> SIZED_FLUID_INGREDIENT_CODEC = Codec.withAlternative(
            CreateCodecs.FLAT_SIZED_FLUID_INGREDIENT_WITH_TYPE,
            Codec.withAlternative(FLUID_STACK_INGREDIENT_CODEC,
                    Codec.withAlternative(FLUID_TAG_INGREDIENT_CODEC, LEGACY_FLUID_TAG_INGREDIENT_CODEC))
    ).validate(AbyssCatalysisRecipeIngredient::validateFluidIngredientAmount);

    private static final StreamCodec<RegistryFriendlyByteBuf, SizedFluidIngredient> SIZED_FLUID_INGREDIENT_STREAM_CODEC =
            StreamCodec.of(
                    (buffer, ingredient) -> {
                        if (!isFluidAmountInRange(ingredient.amount())) {
                            throw new EncoderException("Fluid ingredient amount must be between " + MIN_FLUID_AMOUNT
                                    + " and " + MAX_FLUID_AMOUNT);
                        }
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, ingredient);
                    },
                    buffer -> {
                        SizedFluidIngredient ingredient;
                        try {
                            ingredient = SizedFluidIngredient.STREAM_CODEC.decode(buffer);
                        } catch (IllegalArgumentException exception) {
                            throw new DecoderException("Invalid fluid ingredient amount", exception);
                        }
                        if (!isFluidAmountInRange(ingredient.amount())) {
                            throw new DecoderException("Fluid ingredient amount must be between " + MIN_FLUID_AMOUNT
                                    + " and " + MAX_FLUID_AMOUNT);
                        }
                        return ingredient;
                    });

    public static final Codec<AbyssCatalysisRecipeIngredient> CODEC = Codec.either(
            SIZED_FLUID_INGREDIENT_CODEC,
            Ingredient.CODEC
    ).xmap(AbyssCatalysisRecipeIngredient::new, AbyssCatalysisRecipeIngredient::value);

    public static final StreamCodec<RegistryFriendlyByteBuf, AbyssCatalysisRecipeIngredient> STREAM_CODEC = StreamCodec.of(
            (buffer, ingredient) -> {
                SizedFluidIngredient fluidIngredient = ingredient.fluidIngredient();
                buffer.writeBoolean(fluidIngredient != null);
                if (fluidIngredient != null) {
                    SIZED_FLUID_INGREDIENT_STREAM_CODEC.encode(buffer, fluidIngredient);
                } else {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.itemIngredient());
                }
            },
            buffer -> buffer.readBoolean()
                    ? fluid(SIZED_FLUID_INGREDIENT_STREAM_CODEC.decode(buffer))
                    : item(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer))
    );

    public static AbyssCatalysisRecipeIngredient fluid(SizedFluidIngredient ingredient) {
        return new AbyssCatalysisRecipeIngredient(Either.left(ingredient));
    }

    public static AbyssCatalysisRecipeIngredient item(Ingredient ingredient) {
        return new AbyssCatalysisRecipeIngredient(Either.right(ingredient));
    }

    public boolean isFluid() {
        return value.left().isPresent();
    }

    public SizedFluidIngredient fluidIngredient() {
        return value.left().orElse(null);
    }

    public Ingredient itemIngredient() {
        return value.right().orElse(Ingredient.EMPTY);
    }

    private static SizedFluidIngredient fluidTagIngredient(TagKey<Fluid> tag, int amount) {
        return new SizedFluidIngredient(TagFluidIngredient.tag(tag), amount);
    }

    private static DataResult<SizedFluidIngredient> validateFluidIngredientAmount(SizedFluidIngredient ingredient) {
        return isFluidAmountInRange(ingredient.amount())
                ? DataResult.success(ingredient)
                : DataResult.error(() -> "Fluid ingredient amount must be between " + MIN_FLUID_AMOUNT + " and "
                        + MAX_FLUID_AMOUNT);
    }

    private static boolean isFluidAmountInRange(int amount) {
        return amount >= MIN_FLUID_AMOUNT && amount <= MAX_FLUID_AMOUNT;
    }
}
