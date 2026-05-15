package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AbyssCatalyticChamberTopBlockEntity extends AbyssCatalyticChamberBlockEntity {
    public AbyssCatalyticChamberTopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ABYSS_CATALYTIC_CHAMBER_TOP.get(), pos, state);
    }
}
