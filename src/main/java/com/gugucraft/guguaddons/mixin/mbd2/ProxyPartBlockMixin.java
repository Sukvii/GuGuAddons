package com.gugucraft.guguaddons.mixin.mbd2;

import com.gugucraft.guguaddons.compat.mbd2.RedstonePorts;
import com.lowdragmc.mbd2.api.block.ProxyPartBlock;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ProxyPartBlock.class, remap = false)
public abstract class ProxyPartBlockMixin extends Block {
    protected ProxyPartBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        var port = RedstonePorts.portAt(level, pos);
        return port != null && IMachine.ofMachine(level, port.getControllerPos()).orElse(null) instanceof MBDMachine controller
                ? controller.getAnalogOutputSignal() : 0;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction side) {
        return side != null && RedstonePorts.portAt(level, pos) != null;
    }
}
