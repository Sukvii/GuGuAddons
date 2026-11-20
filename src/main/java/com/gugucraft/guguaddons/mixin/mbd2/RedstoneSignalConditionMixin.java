package com.gugucraft.guguaddons.mixin.mbd2;

import com.gugucraft.guguaddons.compat.mbd2.RedstonePorts;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.recipe.RedstoneSignalCondition;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RedstoneSignalCondition.class, remap = false)
public abstract class RedstoneSignalConditionMixin {
    @ModifyExpressionValue(method = "test", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBestNeighborSignal(Lnet/minecraft/core/BlockPos;)I"))
    private int guguaddons$includePortSignals(int signal, MBDRecipe recipe, RecipeLogic logic) {
        if (signal == 15 || !(logic.getMachine() instanceof RedstonePorts ports)) return signal;

        var level = logic.getMachine().getLevel();
        for (BlockPos pos : ports.guguaddons$redstonePorts()) {
            if (!level.isLoaded(pos)) continue;
            signal = Math.max(signal, level.getBestNeighborSignal(pos));
            if (signal == 15) break;
        }
        return signal;
    }
}
