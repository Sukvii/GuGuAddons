package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class QuestInputBlockEntity extends KineticBlockEntity {
    public static final float STRESS_APPLIED = 512.0f;

    public QuestInputBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUEST_INPUT.get(), pos, state);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level != null && !level.isClientSide && Math.abs(previousSpeed - getSpeed()) > 0.01f) {
            QuestInterfaceBlockEntity.notifyInputSpeedChangedAround(level, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) {
            QuestInterfaceBlockEntity.requestStructureRefreshAround(level, worldPosition);
        }
        super.onChunkUnloaded();
    }

    @Override
    public float calculateStressApplied() {
        return STRESS_APPLIED;
    }
}
