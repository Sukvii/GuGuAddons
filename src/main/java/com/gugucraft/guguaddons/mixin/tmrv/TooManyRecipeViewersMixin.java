package com.gugucraft.guguaddons.mixin.tmrv;

import com.gugucraft.guguaddons.GuGuAddons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.nolij.toomanyrecipeviewers.TooManyRecipeViewers", remap = false)
public abstract class TooManyRecipeViewersMixin {
    @Shadow
    private volatile boolean recipesBaked;

    @Unique
    private boolean guguaddons$loggedDuplicateRecipesBaked;

    @Inject(method = "recipesBaked()V", at = @At("HEAD"), cancellable = true)
    private void guguaddons$skipDuplicateRecipesBaked(CallbackInfo ci) {
        if (!recipesBaked) {
            return;
        }

        if (!guguaddons$loggedDuplicateRecipesBaked) {
            guguaddons$loggedDuplicateRecipesBaked = true;
            GuGuAddons.LOGGER.debug("Skipped duplicate TMRV recipesBaked() call after an EMI recipe rebake");
        }
        ci.cancel();
    }
}
