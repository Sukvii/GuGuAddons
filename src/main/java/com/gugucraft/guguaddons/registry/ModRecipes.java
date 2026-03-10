package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;
import com.gugucraft.guguaddons.recipe.CompressorRecipeSerializer;
import com.gugucraft.guguaddons.recipe.PressurizingRecipe;
import com.gugucraft.guguaddons.recipe.VacuumizingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister
            .create(Registries.RECIPE_SERIALIZER, GuGuAddons.MODID);
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister
            .create(Registries.RECIPE_TYPE, GuGuAddons.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<com.gugucraft.guguaddons.recipe.SlashBackSmithingRecipe>> SLASH_BACK_SMITHING_SERIALIZER = SERIALIZERS
            .register("slash_back_smithing",
                    com.gugucraft.guguaddons.recipe.SlashBackSmithingRecipe.Serializer::new);

    private static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VacuumizingRecipe>> VACUUMIZING_SERIALIZER_HOLDER =
            SERIALIZERS.register("vacuumizing", () -> new CompressorRecipeSerializer<>(VacuumizingRecipe::new));
    private static final DeferredHolder<RecipeType<?>, RecipeType<VacuumizingRecipe>> VACUUMIZING_TYPE_HOLDER =
            TYPES.register("vacuumizing", () -> RecipeType.simple(id("vacuumizing")));

    private static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PressurizingRecipe>> PRESSURIZING_SERIALIZER_HOLDER =
            SERIALIZERS.register("pressurizing", () -> new CompressorRecipeSerializer<>(PressurizingRecipe::new));
    private static final DeferredHolder<RecipeType<?>, RecipeType<PressurizingRecipe>> PRESSURIZING_TYPE_HOLDER =
            TYPES.register("pressurizing", () -> RecipeType.simple(id("pressurizing")));

    public static final ModProcessingRecipeType<VacuumizingRecipe> VACUUMIZING =
            new ModProcessingRecipeType<>(id("vacuumizing"), VACUUMIZING_SERIALIZER_HOLDER::get,
                    VACUUMIZING_TYPE_HOLDER::get);

    public static final ModProcessingRecipeType<PressurizingRecipe> PRESSURIZING =
            new ModProcessingRecipeType<>(id("pressurizing"), PRESSURIZING_SERIALIZER_HOLDER::get,
                    PRESSURIZING_TYPE_HOLDER::get);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(GuGuAddons.MODID, path);
    }

    public static final class ModProcessingRecipeType<R extends Recipe<?>> implements IRecipeTypeInfo {
        private final ResourceLocation id;
        private final java.util.function.Supplier<RecipeSerializer<?>> serializerSupplier;
        private final java.util.function.Supplier<RecipeType<?>> typeSupplier;

        private ModProcessingRecipeType(ResourceLocation id, java.util.function.Supplier<RecipeSerializer<?>> serializerSupplier,
                java.util.function.Supplier<RecipeType<?>> typeSupplier) {
            this.id = id;
            this.serializerSupplier = serializerSupplier;
            this.typeSupplier = typeSupplier;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends RecipeSerializer<?>> T getSerializer() {
            return (T) serializerSupplier.get();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends RecipeInput, T extends Recipe<I>> RecipeType<T> getType() {
            return (RecipeType<T>) typeSupplier.get();
        }
    }
}
