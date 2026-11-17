package com.gugucraft.guguaddons.mixin.emi;

import com.gugucraft.guguaddons.client.emi.EmiClientBakeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.emi.emi.registry.EmiRecipes", remap = false)
public abstract class EmiRecipesMixin {
    @Inject(method = "bake()V", at = @At("HEAD"))
    private static void guguaddons$consumePendingBake(CallbackInfo ci) {
        EmiClientBakeHelper.onRecipeBakeStarted();
    }
}
