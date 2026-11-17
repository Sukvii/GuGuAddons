package com.gugucraft.guguaddons.compat.emi;

import com.alessandro.astages.api.holder.AClientHolder;
import com.alessandro.astages.api.util.AStagesClientUtils;
import com.alessandro.astages.engine.AClientRestrictionManager;
import com.alessandro.astages.engine.client.restriction.recipe.AClientRecipeModRestriction;
import com.alessandro.astages.engine.client.restriction.recipe.AClientRecipeRestriction;
import dev.emi.emi.api.recipe.EmiRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Uses a per-bake snapshot because AStages 2.5.3 skips recipe locks without a mod-wide lock.
 */
public final class AStagesEmiVisibility {
    private static final String TMRV_RECIPE_CLASS = "dev.nolij.toomanyrecipeviewers.impl.recipe.TMRVRecipe";
    private static final String TMRV_NAMESPACE = "toomanyrecipeviewers";
    private static Map<ResourceLocation, List<AClientRecipeRestriction>> recipeRestrictions = Map.of();
    private static List<AClientRecipeModRestriction> modRestrictions = List.of();
    private static Set<String> unlockedStages = Set.of();

    private AStagesEmiVisibility() {
    }

    public static void refreshRestrictions() {
        Map<ResourceLocation, List<AClientRecipeRestriction>> byRecipe = new HashMap<>();
        for (AClientRecipeRestriction restriction :
                AClientRestrictionManager.RECIPE_INSTANCE.getRegistry().getRecipeRestrictions()) {
            for (ResourceLocation recipeId : restriction.getRecipes()) {
                byRecipe.computeIfAbsent(recipeId, ignored -> new ArrayList<>()).add(restriction);
            }
        }
        recipeRestrictions = byRecipe;
        modRestrictions = List.copyOf(AClientRestrictionManager.RECIPE_INSTANCE.getRegistry().getModRestrictions());
        unlockedStages = AStagesClientUtils.getStages(AClientHolder.serverAndPlayer());
    }

    public static boolean shouldHideRecipe(EmiRecipe recipe) {
        RecipeHolder<?> backingRecipe = recipe.getBackingRecipe();
        if (backingRecipe != null && isRestricted(backingRecipe.id(), backingRecipe.value().getType())) {
            return true;
        }
        ResourceLocation originalId = tmrvOriginalId(recipe);
        if (originalId == null) {
            return false;
        }

        for (ResourceLocation recipeId : candidateRecipeIds(recipe, originalId)) {
            RecipeHolder<?> holder = findRecipe(recipeId);
            if (isRestricted(recipeId, holder == null ? null : holder.value().getType())) {
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

    private static boolean isRestricted(ResourceLocation recipeId, RecipeType<?> type) {
        for (AClientRecipeRestriction restriction : recipeRestrictions.getOrDefault(recipeId, List.of())) {
            if ((type == null || restriction.getType() == type)
                    && !unlockedStages.contains(restriction.getStage())) {
                return true;
            }
        }

        for (AClientRecipeModRestriction restriction : modRestrictions) {
            if (restriction.getModId().equals(recipeId.getNamespace())
                    && !restriction.getIgnoredRecipeIds().contains(recipeId)
                    && !unlockedStages.contains(restriction.getStage())) {
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
