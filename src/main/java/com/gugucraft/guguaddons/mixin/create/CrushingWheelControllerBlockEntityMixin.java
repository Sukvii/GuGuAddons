package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(CrushingWheelControllerBlockEntity.class)
public abstract class CrushingWheelControllerBlockEntityMixin {
    @Inject(method = "findRecipe", at = @At("RETURN"), cancellable = true)
    private void guguaddons$filterRecipe(
            CallbackInfoReturnable<Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>>> cir) {
        cir.setReturnValue(cir.getReturnValue()
                .filter(holder -> MachineRecipeStageManager.canProcess(
                        (BlockEntity) (Object) this, holder)));
    }
}
