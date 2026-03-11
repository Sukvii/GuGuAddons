package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.block.custom.CentrifugeStructuralBlock;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CentrifugeStructuralBlockEntity extends SmartBlockEntity {
    public CentrifugeStructuralBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CENTRIFUGE_STRUCTURE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Nullable
    public CentrifugeBlockEntity getMaster() {
        if (level == null) {
            return null;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof CentrifugeStructuralBlock structuralBlock)
                || !structuralBlock.stillValid(level, worldPosition, state, false)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(CentrifugeStructuralBlock.getMaster(level, worldPosition, state));
        return blockEntity instanceof CentrifugeBlockEntity centrifuge ? centrifuge : null;
    }

    @Nullable
    public IItemHandlerModifiable getItemCapability() {
        CentrifugeBlockEntity master = getMaster();
        return master == null ? null : master.getItemCapability();
    }

    @Nullable
    public IFluidHandler getFluidCapability() {
        CentrifugeBlockEntity master = getMaster();
        return master == null ? null : master.getFluidCapability();
    }
}
