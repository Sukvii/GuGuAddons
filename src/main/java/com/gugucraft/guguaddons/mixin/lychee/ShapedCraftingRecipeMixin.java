package com.gugucraft.guguaddons.mixin.lychee;

import com.gugucraft.guguaddons.compat.lychee.LycheeRecipeStageHooks;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import snownee.lychee.recipes.ShapedCraftingRecipe;
import snownee.lychee.util.context.LycheeContext;

@Mixin(ShapedCraftingRecipe.class)
public abstract class ShapedCraftingRecipeMixin {
    @ModifyExpressionValue(method = "updateContextAndGet", at = @At(value = "INVOKE",
            target = "Lsnownee/lychee/recipes/ShapedCraftingRecipe$ResolvableContext;resolve(Lnet/minecraft/world/level/Level;)Lsnownee/lychee/util/context/LycheeContext;"))
    private LycheeContext guguaddons$rejectLockedCraftingContext(LycheeContext context) {
        // Lychee 6.7 resolves a container-specific context before matching or assembling.
        // Preserve its cache so rejecting one recipe cannot erase the player's context
        // for another candidate. The caller handles a null context as an unavailable recipe.
        if (context != null && !LycheeRecipeStageHooks.canCraft(context, context.level(), (Recipe<?>) (Object) this)) {
            return null;
        }
        return context;
    }
}
