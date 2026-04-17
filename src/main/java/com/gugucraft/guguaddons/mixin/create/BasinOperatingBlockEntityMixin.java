package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BasinOperatingBlockEntity.class)
public abstract class BasinOperatingBlockEntityMixin {
    @Shadow
    protected Recipe<?> currentRecipe;

    @Inject(method = "getMatchingRecipes", at = @At("RETURN"), cancellable = true)
    private void guguaddons$filterMatchingRecipes(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        BlockEntity machine = (BlockEntity) (Object) this;
        cir.setReturnValue(cir.getReturnValue().stream()
                .filter(recipe -> MachineRecipeStageManager.canProcess(machine, recipe))
                .toList());
    }

    @Inject(method = "applyBasinRecipe", at = @At("HEAD"), cancellable = true)
    private void guguaddons$cancelLockedRecipe(CallbackInfo ci) {
        if (!MachineRecipeStageManager.canProcess((BlockEntity) (Object) this, currentRecipe)) {
            ci.cancel();
        }
    }
}
