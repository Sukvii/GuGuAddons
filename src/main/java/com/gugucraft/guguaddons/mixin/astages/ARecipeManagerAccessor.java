package com.gugucraft.guguaddons.mixin.astages;

import com.alessandro.astages.api.cache.server.ResourceLocationCache;
import com.alessandro.astages.engine.server.manager.ARecipeManager;
import com.alessandro.astages.engine.server.restriction.recipe.ARecipeRestriction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ARecipeManager.class, remap = false)
public interface ARecipeManagerAccessor {
    @Accessor("recipeCache")
    ResourceLocationCache<ARecipeRestriction> guguaddons$getRecipeCache();
}
