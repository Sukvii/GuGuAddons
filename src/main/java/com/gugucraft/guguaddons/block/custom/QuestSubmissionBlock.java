package com.gugucraft.guguaddons.block.custom;

import com.gugucraft.guguaddons.block.entity.QuestSubmissionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

public class QuestSubmissionBlock extends BaseEntityBlock {
    public static final MapCodec<QuestSubmissionBlock> CODEC = simpleCodec(QuestSubmissionBlock::new);

    public QuestSubmissionBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new QuestSubmissionBlockEntity(pPos, pState);
    }
}
