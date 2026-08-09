package com.gugucraft.guguaddons.compat.emi;

import com.alessandro.astages.api.holder.AClientHolder;
import com.alessandro.astages.api.util.AStagesClientUtils;
import com.alessandro.astages.api.wrapper.RecipeWrapper;
import com.alessandro.astages.engine.AClientRestrictionManager;
import com.alessandro.astages.engine.client.restriction.recipe.AClientRecipeModRestriction;
import com.alessandro.astages.engine.client.restriction.recipe.AClientRecipeRestriction;
import dev.emi.emi.api.recipe.EmiRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * Narrow compatibility fallback for TMRV recipes, which do not expose an EMI backing recipe.
 * AStages 2.5 handles all normal item, fluid, and backed recipe visibility itself.
 */
public final class AStagesEmiVisibility {
    private static final String TMRV_RECIPE_CLASS = "dev.nolij.toomanyrecipeviewers.impl.recipe.TMRVRecipe";
    private static final String TMRV_NAMESPACE = "toomanyrecipeviewers";

    private AStagesEmiVisibility() {
    }

    public static boolean shouldHideTmrvRecipe(EmiRecipe recipe) {
        ResourceLocation originalId = tmrvOriginalId(recipe);
        if (originalId == null) {
            return false;
        }

        for (ResourceLocation recipeId : candidateRecipeIds(recipe, originalId)) {
            RecipeHolder<?> holder = findRecipe(recipeId);
            if (holder != null && isRestricted(holder)) {
                return true;
            }
            if (holder == null && isRestrictedWithoutBackingRecipe(recipeId)) {
                return true;
            }
        }
        return false;
    }

    private static RecipeHolder<?> findRecipe(ResourceLocation recipeId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        return minecraft.level.getRecipeManager().byKey(recipeId).orElse(null);
    }

    private static boolean isRestricted(RecipeHolder<?> holder) {
        RecipeWrapper wrapper = new RecipeWrapper(holder.value().getType(), holder.id());
        return AClientRestrictionManager.RECIPE_INSTANCE.getRestriction(AClientHolder.serverAndPlayer(), wrapper) != null;
    }

    private static boolean isRestrictedWithoutBackingRecipe(ResourceLocation recipeId) {
        for (AClientRecipeRestriction restriction :
                AClientRestrictionManager.RECIPE_INSTANCE.getRegistry().getRecipeRestrictions()) {
            if (!restriction.getRecipes().contains(recipeId)) {
                continue;
            }
            if (restriction.getType() == null) {
                continue;
            }
            RecipeWrapper wrapper = new RecipeWrapper(restriction.getType(), recipeId);
            if (AClientRestrictionManager.RECIPE_INSTANCE.getRestriction(
                    AClientHolder.serverAndPlayer(), wrapper) != null) {
                return true;
            }
        }

        for (AClientRecipeModRestriction restriction :
                AClientRestrictionManager.RECIPE_INSTANCE.getRegistry().getModRestrictions()) {
            if (restriction.getModId().equals(recipeId.getNamespace())
                    && !restriction.getIgnoredRecipeIds().contains(recipeId)
                    && !AStagesClientUtils.hasStage(AClientHolder.serverAndPlayer(), restriction.getStage())) {
                return true;
            }
        }
        return false;
    }

    private static List<ResourceLocation> candidateRecipeIds(EmiRecipe recipe, ResourceLocation originalId) {
        ResourceLocation emiId = recipe.getId();
        if (emiId == null || emiId.equals(originalId)) {
            return List.of(originalId);
        }
        return List.of(emiId, originalId);
    }

    private static ResourceLocation tmrvOriginalId(EmiRecipe recipe) {
        if (recipe == null || !TMRV_RECIPE_CLASS.equals(recipe.getClass().getName())) {
            return null;
        }
        return originalIdFromSyntheticTmrvId(recipe.getId());
    }

    private static ResourceLocation originalIdFromSyntheticTmrvId(ResourceLocation syntheticId) {
        if (syntheticId == null || !TMRV_NAMESPACE.equals(syntheticId.getNamespace())) {
            return null;
        }

        String syntheticPath = syntheticId.getPath();
        if (!syntheticPath.startsWith("/")) {
            return null;
        }

        int namespaceEnd = syntheticPath.indexOf('/', 1);
        if (namespaceEnd <= 1 || namespaceEnd == syntheticPath.length() - 1) {
            return null;
        }

        String namespace = syntheticPath.substring(1, namespaceEnd);
        String path = syntheticPath.substring(namespaceEnd + 1);
        return ResourceLocation.tryParse(namespace + ":" + path);
    }
}
