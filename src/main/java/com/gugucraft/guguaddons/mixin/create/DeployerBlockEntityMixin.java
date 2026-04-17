package com.gugucraft.guguaddons.mixin.create;

import com.gugucraft.guguaddons.stage.MachineOwnerHelper;
import com.gugucraft.guguaddons.stage.MachineRecipeStageManager;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(DeployerBlockEntity.class)
public abstract class DeployerBlockEntityMixin {
    @Inject(method = "getRecipe", at = @At("RETURN"), cancellable = true)
    private void guguaddons$filterRecipe(ItemStack stack,
            CallbackInfoReturnable<RecipeHolder<? extends Recipe<? extends RecipeInput>>> cir) {
        RecipeHolder<? extends Recipe<? extends RecipeInput>> recipe = cir.getReturnValue();
        if (recipe != null
                && !MachineRecipeStageManager.canProcessIncludingSequenced((BlockEntity) (Object) this, recipe)) {
            cir.setReturnValue(null);
        }
    }

    @Redirect(method = "initHandler", at = @At(value = "NEW",
            target = "(Lnet/minecraft/server/level/ServerLevel;Ljava/util/UUID;)Lcom/simibubi/create/content/kinetics/deployer/DeployerFakePlayer;"))
    private DeployerFakePlayer guguaddons$usePlacedOwner(ServerLevel level, UUID owner) {
        UUID placedOwner = MachineOwnerHelper.getOwner((BlockEntity) (Object) this);
        return new DeployerFakePlayer(level, placedOwner == null ? owner : placedOwner);
    }
}
