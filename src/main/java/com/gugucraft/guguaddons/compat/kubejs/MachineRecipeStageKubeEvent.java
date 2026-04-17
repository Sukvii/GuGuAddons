package com.gugucraft.guguaddons.compat.kubejs;

import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import dev.latvian.mods.kubejs.event.KubeEvent;

import java.util.List;

public class MachineRecipeStageKubeEvent implements KubeEvent {
    public void addRecipe(String recipeType, String recipeId, String stage) {
        MachineRecipeStageManager.addRecipe(recipeType, recipeId, stage);
    }

    public void addRecipes(String recipeType, String[] recipeIds, String stage) {
        MachineRecipeStageManager.addRecipes(recipeType, recipeIds, stage);
    }

    public void addRecipes(String recipeType, List<String> recipeIds, String stage) {
        MachineRecipeStageManager.addRecipes(recipeType, recipeIds, stage);
    }

    public void addRecipeByMod(String recipeType, String modId, String stage) {
        MachineRecipeStageManager.addRecipeByMod(recipeType, modId, stage);
    }

    public void addRecipeByMods(String recipeType, String[] modIds, String stage) {
        MachineRecipeStageManager.addRecipeByMods(recipeType, modIds, stage);
    }

    public void addRecipeByMods(String recipeType, List<String> modIds, String stage) {
        MachineRecipeStageManager.addRecipesByMods(recipeType, modIds, stage);
    }

    public void addRecipeByMachine(String recipeType, String stage) {
        MachineRecipeStageManager.addRecipeByMachine(recipeType, stage);
    }
}
