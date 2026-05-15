package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AbyssCatalyticChamberMiddleBlockEntity extends AbyssCatalyticChamberBlockEntity {
    public AbyssCatalyticChamberMiddleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ABYSS_CATALYTIC_CHAMBER_MIDDLE.get(), pos, state);
    }
}
