package com.gugucraft.guguaddons.mixin.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Pseudo
@Mixin(targets = "dev.emi.emi.registry.EmiRecipes$Manager", remap = false)
public abstract class EmiRecipeManagerMixin {
    @Redirect(
            method = {"lambda$new$3", "lambda$new$6", "lambda$new$8"},
            at = @At(value = "INVOKE", target = "Ldev/emi/emi/api/stack/EmiStack;copy()Ldev/emi/emi/api/stack/EmiStack;")
    )
    private static EmiStack guguaddons$deferIndexKeyCopy(EmiStack stack) {
        // The lookup below copies only new keys; existing entries need no temporary copy.
        return stack;
    }

    @Redirect(
            method = {"lambda$new$3", "lambda$new$6", "lambda$new$8"},
            at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;")
    )
    private static Object guguaddons$reuseIndexedStack(
            Map<EmiStack, Set<EmiRecipe>> index, Object key,
            Function<? super EmiStack, ? extends Set<EmiRecipe>> factory) {
        EmiStack stack = (EmiStack) key;
        Set<EmiRecipe> recipes = index.get(stack);
        if (recipes != null) {
            return recipes;
        }
        return index.computeIfAbsent(stack.copy(), factory);
    }
}
