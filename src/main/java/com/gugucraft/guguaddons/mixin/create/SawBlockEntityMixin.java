package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SawBlockEntity.class)
public abstract class SawBlockEntityMixin {
    @Inject(method = "getRecipes", at = @At("RETURN"), cancellable = true)
    private void guguaddons$filterRecipes(
            CallbackInfoReturnable<List<RecipeHolder<? extends Recipe<?>>>> cir) {
        BlockEntity machine = (BlockEntity) (Object) this;
        cir.setReturnValue(cir.getReturnValue().stream()
                .filter(holder -> MachineRecipeStageManager.canProcessIncludingSequenced(machine, holder))
                .toList());
    }
}
