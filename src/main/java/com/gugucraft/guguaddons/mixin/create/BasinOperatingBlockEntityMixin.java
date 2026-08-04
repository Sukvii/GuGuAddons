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

import java.util.ArrayList;
import java.util.List;

@Mixin(BasinOperatingBlockEntity.class)
public abstract class BasinOperatingBlockEntityMixin {
    @Shadow
    protected Recipe<?> currentRecipe;

    /**
     * The returned list must stay mutable and independent of Create's original list:
     * subclasses and addons keep appending dynamic recipes after this method returns
     * (e.g. MechanicalMixerBlockEntity#getMatchingRecipes calls add(..) on the super
     * result to inject potion mixing), so an immutable view would crash them.
     */
    @Inject(method = "getMatchingRecipes", at = @At("RETURN"), cancellable = true)
    private void guguaddons$filterMatchingRecipes(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        BlockEntity machine = (BlockEntity) (Object) this;
        List<Recipe<?>> originalRecipes = cir.getReturnValue();

        if (originalRecipes == null || originalRecipes.isEmpty()) {
            cir.setReturnValue(new ArrayList<>());
            return;
        }

        List<Recipe<?>> filteredRecipes = new ArrayList<>(originalRecipes);
        filteredRecipes.removeIf(recipe -> !MachineRecipeStageManager.canProcess(machine, recipe));
        cir.setReturnValue(filteredRecipes);
    }

    @Inject(method = "applyBasinRecipe", at = @At("HEAD"), cancellable = true)
    private void guguaddons$cancelLockedRecipe(CallbackInfo ci) {
        if (!MachineRecipeStageManager.canProcess((BlockEntity) (Object) this, currentRecipe)) {
            ci.cancel();
        }
    }
}
