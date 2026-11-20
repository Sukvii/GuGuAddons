package com.gugucraft.guguaddons.mixin.mbd2;

import com.gugucraft.guguaddons.compat.mbd2.RedstonePorts;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = MBDMultiblockMachine.class, remap = false)
public abstract class MBDMultiblockMachineMixin implements RedstonePorts {
    @Unique private List<BlockPos> guguaddons$ports = List.of();

    @Override
    public List<BlockPos> guguaddons$redstonePorts() {
        return guguaddons$ports;
    }

    @Inject(method = "onStructureFormed", at = @At("TAIL"))
    private void guguaddons$connectPorts(CallbackInfo ci) {
        var machine = (MBDMultiblockMachine) (Object) this;
        if (!(machine.getLevel() instanceof ServerLevel level)) return;

        guguaddons$ports = machine.getProxyWhileFormedPositions().stream()
                .filter(pos -> RedstonePorts.portAt(level, pos) != null).toList();
        for (BlockPos pos : guguaddons$ports) {
            // MBD2 sets the proxy's capabilities after placing it, so reconnect existing dust too.
            level.getBlockState(pos).updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(pos, ProxyPartBlock.BLOCK);
        }
    }

    @Inject(method = "onStructureInvalid(Z)V", at = @At("HEAD"))
    private void guguaddons$disconnectPorts(boolean controllerRemoved, CallbackInfo ci) {
        guguaddons$ports = List.of();
    }
}
