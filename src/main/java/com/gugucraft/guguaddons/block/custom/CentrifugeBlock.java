package com.gugucraft.guguaddons.block.custom;

import com.gugucraft.guguaddons.block.entity.CentrifugeBlockEntity;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CentrifugeBlock extends KineticBlock implements IBE<CentrifugeBlockEntity> {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public CentrifugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        return onBlockEntityUseItemOn(level, pos, be -> be.handleItemUse(player, hand, stack));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        return onBlockEntityUse(level, pos, be -> be.handleEmptyHandUse(player));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState stateForPlacement = super.getStateForPlacement(context);
        if (stateForPlacement == null) {
            return null;
        }

        BlockPos pos = context.getClickedPos();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                BlockState occupiedState = context.getLevel().getBlockState(pos.offset(x, 0, z));
                if (!occupiedState.canBeReplaced()) {
                    return null;
                }
            }
        }

        return stateForPlacement;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (Direction side : Iterate.horizontalDirections) {
            for (boolean secondary : Iterate.falseAndTrue) {
                Direction targetSide = secondary ? side.getClockWise(Axis.Y) : side;
                BlockPos structurePos = (secondary ? pos.relative(side) : pos).relative(targetSide);
                BlockState occupiedState = level.getBlockState(structurePos);
                BlockState requiredStructure = ModBlocks.CENTRIFUGE_STRUCTURE.get().defaultBlockState()
                        .setValue(CentrifugeStructuralBlock.FACING, targetSide.getOpposite());
                if (occupiedState == requiredStructure) {
                    continue;
                }
                if (!occupiedState.canBeReplaced()) {
                    level.destroyBlock(pos, false);
                    return;
                }
                level.setBlockAndUpdate(structurePos, requiredStructure);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }

        forEachStructurePos(pos, structurePos -> {
            BlockState structureState = level.getBlockState(structurePos);
            if (structureState.is(ModBlocks.CENTRIFUGE_STRUCTURE.get())) {
                level.removeBlock(structurePos, false);
            }
        });

        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        super.updateEntityAfterFallOn(level, entity);
        if (!(entity instanceof ItemEntity itemEntity) || entity.level().isClientSide()) {
            return;
        }
        CentrifugeBlockEntity blockEntity = getBlockEntity(level, entity.blockPosition());
        if (blockEntity != null) {
            blockEntity.absorbItemEntity(itemEntity);
        }
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == Axis.Y;
    }

    @Override
    public Class<CentrifugeBlockEntity> getBlockEntityClass() {
        return CentrifugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CentrifugeBlockEntity> getBlockEntityType() {
        return ModBlockEntities.CENTRIFUGE.get();
    }

    @Override
    public boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static void forEachStructurePos(BlockPos masterPos, java.util.function.Consumer<BlockPos> consumer) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                consumer.accept(masterPos.offset(x, 0, z));
            }
        }
    }
}
