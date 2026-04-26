package com.gugucraft.guguaddons.compat.emi;

import com.alessandro.astages.api.wrapper.RecipeWrapper;
import com.alessandro.astages.engine.AClientRestrictionManager;
import com.alessandro.astages.engine.client.restriction.recipe.AClientRecipeModRestriction;
import com.alessandro.astages.engine.client.restriction.recipe.AClientRecipeRestriction;
import com.gugucraft.guguaddons.compat.astages.AStagesHelper;
import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public final class AStagesEmiVisibility {
    private static final String TMRV_RECIPE_CLASS = "dev.nolij.toomanyrecipeviewers.impl.recipe.TMRVRecipe";

    private AStagesEmiVisibility() {
    }

    public static boolean shouldHideMachineRecipe(EmiRecipe recipe) {
        if (recipe == null) {
            return false;
        }

        if (MachineRecipeStageManager.clientShouldHide(recipe.getBackingRecipe())) {
            return true;
        }

        for (ResourceLocation recipeId : candidateRecipeIds(recipe)) {
            if (MachineRecipeStageManager.clientShouldHide(recipeId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldHideRecipe(EmiRecipe recipe) {
        if (recipe == null) {
            return false;
        }

        RecipeHolder<?> holder = recipe.getBackingRecipe();
        if (holder != null && shouldHideRecipe(holder.value().getType(), holder.id())) {
            return true;
        }

        for (ResourceLocation recipeId : candidateRecipeIds(recipe)) {
            if (shouldHideRecipe(null, recipeId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldHideStack(EmiStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ItemStack itemStack = stack.getItemStack();
        if (!itemStack.isEmpty()) {
            return missesAnyStage(AClientRestrictionManager.ITEM_INSTANCE.getStagesForStack(itemStack));
        }

        ResourceLocation id = stack.getId();
        return id != null && missesAnyStage(
                AClientRestrictionManager.ITEM_INSTANCE.getStagesForResourceLocation(id));
    }

    private static boolean shouldHideRecipe(RecipeType<?> recipeType, ResourceLocation recipeId) {
        RecipeWrapper wrapper = recipeType == null ? null : new RecipeWrapper(recipeType, recipeId);

        for (AClientRecipeModRestriction restriction :
                AClientRestrictionManager.RECIPE_INSTANCE.getRegistry().getModRestrictions()) {
            if (isRestrictedByMod(restriction, wrapper, recipeId) && missingStage(restriction.getStage())) {
                return true;
            }
        }

        for (AClientRecipeRestriction restriction :
                AClientRestrictionManager.RECIPE_INSTANCE.getRegistry().getRecipeRestrictions()) {
            if (isRestrictedRecipe(restriction, wrapper, recipeType, recipeId) && missingStage(restriction.getStage())) {
                return true;
            }
        }

        return false;
    }

    private static List<ResourceLocation> candidateRecipeIds(EmiRecipe recipe) {
        ResourceLocation id = recipe.getId();
        ResourceLocation tmrvOriginalId = tmrvOriginalId(recipe);

        if (id == null) {
            return tmrvOriginalId == null ? List.of() : List.of(tmrvOriginalId);
        }
        if (tmrvOriginalId == null || tmrvOriginalId.equals(id)) {
            return List.of(id);
        }
        return List.of(id, tmrvOriginalId);
    }

    private static ResourceLocation tmrvOriginalId(EmiRecipe recipe) {
        if (!TMRV_RECIPE_CLASS.equals(recipe.getClass().getName())) {
            return null;
        }

        try {
            Field field = recipe.getClass().getField("originalId");
            Object value = field.get(recipe);
            return value instanceof ResourceLocation id ? id : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean isRestrictedByMod(AClientRecipeModRestriction restriction, RecipeWrapper wrapper,
                                             ResourceLocation recipeId) {
        if (wrapper != null) {
            return restriction.isRestricted(wrapper);
        }
        return restriction.getModId().equals(recipeId.getNamespace())
                && !restriction.getIgnoredRecipeIds().contains(recipeId);
    }

    private static boolean isRestrictedRecipe(AClientRecipeRestriction restriction, RecipeWrapper wrapper,
                                              RecipeType<?> recipeType, ResourceLocation recipeId) {
        if (wrapper != null) {
            return restriction.isRestricted(wrapper);
        }
        return (recipeType == null || restriction.getType() == recipeType)
                && restriction.getRecipes().contains(recipeId);
    }

    private static boolean missesAnyStage(Set<String> stages) {
        for (String stage : stages) {
            if (missingStage(stage)) {
                return true;
            }
        }
        return false;
    }

    private static boolean missingStage(String stage) {
        return stage != null && !stage.isBlank() && !AStagesHelper.clientHasStage(stage);
    }
}
