package com.gugucraft.guguaddons.recipe;

import com.gugucraft.guguaddons.Config;
import com.gugucraft.guguaddons.registry.ModItems;
import com.gugucraft.guguaddons.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

public class SlashBackSmithingRecipe implements SmithingRecipe {

    public SlashBackSmithingRecipe() {
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        // Template: Configured Template (e.g. Netherite Upgrade)
        // Base: Slash Back Terminal
        // Addition: Configured Material (e.g. Ender Pearl)

        ItemStack templateStack = input.template();
        ItemStack baseStack = input.base();
        ItemStack additionStack = input.addition();

        Item configuredTemplate = Config.getEffectiveSlashBackRefillTemplateItem();
        Item configuredMaterial = Config.getEffectiveSlashBackRefillMaterialItem();

        boolean templateMatches = !templateStack.isEmpty() && templateStack.getItem() == configuredTemplate;
        boolean baseMatches = !baseStack.isEmpty() && baseStack.getItem() == ModItems.DEATH_RECALL_ITEM.get();
        boolean additionMatches = !additionStack.isEmpty() && additionStack.getItem() == configuredMaterial;

        return templateMatches && baseMatches && additionMatches;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack baseStack = input.base();
        if (baseStack.isEmpty())
            return ItemStack.EMPTY;

        ItemStack result = baseStack.copy();
        // Fully repair
        result.setDamageValue(0);
        return result;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.DEATH_RECALL_ITEM.get()); // Generic result
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.getItem() == Config.getEffectiveSlashBackRefillTemplateItem();
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() == ModItems.DEATH_RECALL_ITEM.get();
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.getItem() == Config.getEffectiveSlashBackRefillMaterialItem();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SLASH_BACK_SMITHING_SERIALIZER.get();
    }

    // Serializer implementation
    public static class Serializer implements RecipeSerializer<SlashBackSmithingRecipe> {
        private static final SlashBackSmithingRecipe INSTANCE = new SlashBackSmithingRecipe();
        public static final MapCodec<SlashBackSmithingRecipe> CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<RegistryFriendlyByteBuf, SlashBackSmithingRecipe> STREAM_CODEC = StreamCodec
                .unit(INSTANCE);

        @Override
        public MapCodec<SlashBackSmithingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SlashBackSmithingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
