package com.gugucraft.guguaddons.mixin.lychee;

import com.gugucraft.guguaddons.compat.lychee.LycheeRecipeStageHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import snownee.lychee.recipes.EntityTickingRecipe;
import snownee.lychee.recipes.EntityTickingRecipeType;

import java.util.Iterator;
import java.util.List;

@Mixin(EntityTickingRecipeType.class)
public abstract class EntityTickingRecipeTypeMixin {
    @Redirect(method = "process", at = @At(value = "INVOKE",
            target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<RecipeHolder<EntityTickingRecipe>> guguaddons$filterTickingRecipes(
            List<RecipeHolder<EntityTickingRecipe>> recipes, Entity entity) {
        return recipes.stream()
                .filter(holder -> LycheeRecipeStageHooks.canProcess(entity, holder))
                .iterator();
    }
}
