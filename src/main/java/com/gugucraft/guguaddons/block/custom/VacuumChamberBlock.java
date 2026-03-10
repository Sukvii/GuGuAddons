package com.gugucraft.guguaddons.block.custom;

import com.gugucraft.guguaddons.block.entity.VacuumChamberBlockEntity;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class VacuumChamberBlock extends KineticBlock
        implements IBE<VacuumChamberBlockEntity>, ICogWheel, IWrenchable {

    public VacuumChamberBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !BasinBlock.isBasin(level, pos.below());
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    public Class<VacuumChamberBlockEntity> getBlockEntityClass() {
        return VacuumChamberBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VacuumChamberBlockEntity> getBlockEntityType() {
        return ModBlockEntities.VACUUM_CHAMBER.get();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        VacuumChamberBlockEntity blockEntity = getBlockEntity(context.getLevel(), context.getClickedPos());
        if (blockEntity == null || !blockEntity.canChangeMode()) {
            return InteractionResult.PASS;
        }

        blockEntity.changeMode();
        if (context.getLevel().isClientSide()) {
            AllSoundEvents.WRENCH_ROTATE.playAt(context.getLevel(), context.getClickedPos(), 1, 1, true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
}
