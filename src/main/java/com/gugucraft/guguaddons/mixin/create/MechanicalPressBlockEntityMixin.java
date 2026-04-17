package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(MechanicalPressBlockEntity.class)
public abstract class MechanicalPressBlockEntityMixin {
    @Inject(method = "getRecipe", at = @At("RETURN"), cancellable = true)
    private void guguaddons$filterRecipe(ItemStack stack,
                                         CallbackInfoReturnable<Optional<RecipeHolder<PressingRecipe>>> cir) {
        Optional<RecipeHolder<PressingRecipe>> recipe = cir.getReturnValue()
                .filter(holder -> MachineRecipeStageManager.canProcessIncludingSequenced(
                        (BlockEntity) (Object) this, holder));
        cir.setReturnValue(recipe);
    }
}
