package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class QuestInputBlockEntity extends KineticBlockEntity {

    public QuestInputBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUEST_INPUT.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public float calculateStressApplied() {
        return 32.0f; // Stress Impact: 32x RPM
    }
}
