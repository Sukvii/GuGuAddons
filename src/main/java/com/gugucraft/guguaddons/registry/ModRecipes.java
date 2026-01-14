package com.gugucraft.guguaddons.registry;

import com.gugucraft.guguaddons.GuGuAddons;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister
            .create(Registries.RECIPE_SERIALIZER, GuGuAddons.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<com.gugucraft.guguaddons.recipe.SlashBackSmithingRecipe>> SLASH_BACK_SMITHING_SERIALIZER = SERIALIZERS
            .register("slash_back_smithing",
                    com.gugucraft.guguaddons.recipe.SlashBackSmithingRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
