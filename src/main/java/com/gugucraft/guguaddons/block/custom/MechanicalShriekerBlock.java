package com.gugucraft.guguaddons.block.custom;

import com.gugucraft.guguaddons.block.entity.MechanicalShriekerBlockEntity;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class MechanicalShriekerBlock extends DirectionalKineticBlock
        implements IBE<MechanicalShriekerBlockEntity>, ICogWheel {
    public static final BooleanProperty SHRIEKING = BlockStateProperties.SHRIEKING;

    public MechanicalShriekerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SHRIEKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(SHRIEKING));
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.NORMAL;
    }

    @Override
    public Class<MechanicalShriekerBlockEntity> getBlockEntityClass() {
        return MechanicalShriekerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MechanicalShriekerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.MECHANICAL_SHRIEKER.get();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
}
