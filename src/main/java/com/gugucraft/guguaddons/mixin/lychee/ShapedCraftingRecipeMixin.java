package com.gugucraft.guguaddons.mixin.lychee;

import com.google.common.cache.Cache;
import com.gugucraft.guguaddons.compat.lychee.LycheeRecipeStageHooks;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.lychee.recipes.ShapedCraftingRecipe;
import snownee.lychee.util.context.LycheeContext;

@Mixin(ShapedCraftingRecipe.class)
public abstract class ShapedCraftingRecipeMixin {
    @Shadow
    @Final
    private static Cache<CraftingInput, LycheeContext> CONTEXT_CACHE;

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"), cancellable = true)
    private void guguaddons$hideLockedCraftingRecipe(CraftingInput input, Level level,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }

        LycheeContext context = CONTEXT_CACHE.getIfPresent(input);
        if (!LycheeRecipeStageHooks.canCraft(context, level, (Recipe<?>) (Object) this)) {
            CONTEXT_CACHE.invalidate(input);
            cir.setReturnValue(false);
        }
    }
}
