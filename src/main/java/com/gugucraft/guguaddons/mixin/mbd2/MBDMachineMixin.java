package com.gugucraft.guguaddons.mixin.mbd2;

import com.gugucraft.guguaddons.compat.mbd2.RedstonePorts;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MBDMachine.class, remap = false)
public abstract class MBDMachineMixin {
    @Inject(method = "setAnalogOutputSignal", at = @At(value = "INVOKE",
            target = "Lcom/lowdragmc/mbd2/common/machine/MBDMachine;updateSignal()V", shift = At.Shift.AFTER))
    private void guguaddons$updateComparators(int signal, CallbackInfo ci) {
        var machine = (MBDMachine) (Object) this;
        if (!(machine.getLevel() instanceof ServerLevel level)) return;

        level.updateNeighbourForOutputSignal(machine.getPos(), machine.getBlockState().getBlock());
        if (machine instanceof RedstonePorts ports) {
            for (BlockPos pos : ports.guguaddons$redstonePorts()) {
                if (level.isLoaded(pos)) level.updateNeighbourForOutputSignal(pos, ProxyPartBlock.BLOCK);
            }
        }
    }
}
