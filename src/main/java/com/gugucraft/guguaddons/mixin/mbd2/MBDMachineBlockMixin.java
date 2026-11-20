package com.gugucraft.guguaddons.mixin.mbd2;

import com.lowdragmc.mbd2.common.block.MBDMachineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = MBDMachineBlock.class, remap = false)
public abstract class MBDMachineBlockMixin extends Block {
    protected MBDMachineBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
}
