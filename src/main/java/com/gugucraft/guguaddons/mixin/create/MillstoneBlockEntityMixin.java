package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.CreateRecipeStageHooks;
import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(MillstoneBlockEntity.class)
public abstract class MillstoneBlockEntityMixin {
    @Shadow
    private MillingRecipe lastRecipe;

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/AllRecipeTypes;find(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
    private <I extends RecipeInput, R extends Recipe<I>> Optional<RecipeHolder<R>> guguaddons$filterTickFind(
            AllRecipeTypes recipeType, I input, Level level) {
        return CreateRecipeStageHooks.findUnlocked((BlockEntity) (Object) this, recipeType, input, level);
    }

    @Redirect(method = "process", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/AllRecipeTypes;find(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
    private <I extends RecipeInput, R extends Recipe<I>> Optional<RecipeHolder<R>> guguaddons$filterProcessFind(
            AllRecipeTypes recipeType, I input, Level level) {
        return CreateRecipeStageHooks.findUnlocked((BlockEntity) (Object) this, recipeType, input, level);
    }

    @Inject(method = "process", at = @At("HEAD"), cancellable = true)
    private void guguaddons$cancelLockedCachedRecipe(CallbackInfo ci) {
        if (lastRecipe != null && !MachineRecipeStageManager.canProcess((BlockEntity) (Object) this, lastRecipe)) {
            ci.cancel();
        }
    }
}
