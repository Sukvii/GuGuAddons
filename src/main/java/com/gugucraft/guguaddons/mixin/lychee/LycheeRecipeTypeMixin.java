package com.gugucraft.guguaddons.mixin.lychee;

import com.gugucraft.guguaddons.compat.lychee.LycheeRecipeStageHooks;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.lychee.util.context.LycheeContext;
import snownee.lychee.util.recipe.ILycheeRecipe;
import snownee.lychee.util.recipe.LycheeRecipeType;

import java.util.Optional;

@Mixin(LycheeRecipeType.class)
public abstract class LycheeRecipeTypeMixin<T extends ILycheeRecipe<LycheeContext>> {
    @Inject(method = "tryMatch", at = @At("HEAD"), cancellable = true)
    private void guguaddons$skipLockedRecipe(RecipeHolder<T> holder, Level level, LycheeContext context,
                                             CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        if (!LycheeRecipeStageHooks.canProcess(context, holder)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
