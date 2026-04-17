package com.gugucraft.guguaddons.stage;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public record MachineRecipeStageRestriction(String stageId, RecipeType<?> recipeType,
                                            List<ResourceLocation> recipeIds) {
    public MachineRecipeStageRestriction {
        recipeIds = List.copyOf(recipeIds);
    }
}
