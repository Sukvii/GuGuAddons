package com.gugucraft.guguaddons.block.custom;

import com.gugucraft.guguaddons.block.entity.AbyssCatalyticChamberBlockEntity;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AbyssCatalyticChamberBlock extends Block implements IBE<AbyssCatalyticChamberBlockEntity>, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public AbyssCatalyticChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public Class<AbyssCatalyticChamberBlockEntity> getBlockEntityClass() {
        return AbyssCatalyticChamberBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AbyssCatalyticChamberBlockEntity> getBlockEntityType() {
        return ModBlockEntities.ABYSS_CATALYTIC_CHAMBER.get();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState stateForPlacement = super.getStateForPlacement(context);
        if (stateForPlacement == null || !canPlaceCompleteChamber(context.getLevel(), context.getClickedPos())) {
            return null;
        }
        return stateForPlacement;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (state.is(oldState.getBlock()) || level.isClientSide()) {
            return;
        }

        BlockPos middlePos = pos.above();
        BlockPos topPos = pos.above(2);
        if (!canReplaceForStructure(level, middlePos) || !canReplaceForStructure(level, topPos)) {
            level.destroyBlock(pos, false);
            return;
        }

        level.setBlockAndUpdate(middlePos, ModBlocks.ABYSS_CATALYTIC_CHAMBER_MIDDLE.get().defaultBlockState());
        level.setBlockAndUpdate(topPos, ModBlocks.ABYSS_CATALYTIC_CHAMBER_TOP.get().defaultBlockState());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }

        removeStructureParts(level, pos);
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
            BlockPos currentPos, BlockPos facingPos) {
        if (facing == Direction.UP && level instanceof Level actualLevel && !actualLevel.isClientSide()
                && !actualLevel.getBlockTicks().hasScheduledTick(currentPos, this)) {
            actualLevel.scheduleTick(currentPos, this, 1);
        }
        return state;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isValidChamber(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
            Player player) {
        return new ItemStack(ModBlocks.ABYSS_CATALYTIC_CHAMBER.get());
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!context.getLevel().isClientSide()) {
            withBlockEntityDo(context.getLevel(), context.getClickedPos(),
                    be -> be.onWrenched(context.getClickedFace()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        return onBlockEntityUseItemOn(level, pos, be -> be.handleItemUse(player, hand, stack));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        return onBlockEntityUse(level, pos, be -> be.handleEmptyHandUse(player));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    public static BlockPos getBottomPos(BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof AbyssCatalyticChamberMiddleBlock) {
            return pos.below();
        }
        if (block instanceof AbyssCatalyticChamberTopBlock) {
            return pos.below(2);
        }
        return pos;
    }

    public static boolean isValidChamber(LevelReader level, BlockPos bottomPos) {
        return level.getBlockState(bottomPos).getBlock() instanceof AbyssCatalyticChamberBlock
                && level.getBlockState(bottomPos.above()).getBlock() instanceof AbyssCatalyticChamberMiddleBlock
                && level.getBlockState(bottomPos.above(2)).getBlock() instanceof AbyssCatalyticChamberTopBlock;
    }

    public static boolean canPlaceCompleteChamber(LevelReader level, BlockPos bottomPos) {
        return canReplaceForStructure(level, bottomPos.above()) && canReplaceForStructure(level, bottomPos.above(2));
    }

    public static void destroyChamber(Level level, BlockPos bottomPos, boolean dropBottom) {
        if (level.isClientSide()) {
            return;
        }

        BlockState bottomState = level.getBlockState(bottomPos);
        if (bottomState.is(ModBlocks.ABYSS_CATALYTIC_CHAMBER.get())) {
            level.destroyBlock(bottomPos, dropBottom);
            return;
        }

        removeStructureParts(level, bottomPos);
    }

    public static boolean isMiddleBoundToBottom(BlockGetter level, BlockPos middlePos) {
        return level.getBlockState(middlePos.below()).is(ModBlocks.ABYSS_CATALYTIC_CHAMBER.get());
    }

    public static boolean isTopBoundToBottom(BlockGetter level, BlockPos topPos) {
        return level.getBlockState(topPos.below()).is(ModBlocks.ABYSS_CATALYTIC_CHAMBER_MIDDLE.get())
                && level.getBlockState(topPos.below(2)).is(ModBlocks.ABYSS_CATALYTIC_CHAMBER.get());
    }

    private static boolean canReplaceForStructure(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced()
                || state.is(ModBlocks.ABYSS_CATALYTIC_CHAMBER_MIDDLE.get())
                || state.is(ModBlocks.ABYSS_CATALYTIC_CHAMBER_TOP.get());
    }

    private static void removeStructureParts(Level level, BlockPos bottomPos) {
        removeStructurePart(level, bottomPos.above(2), ModBlocks.ABYSS_CATALYTIC_CHAMBER_TOP.get());
        removeStructurePart(level, bottomPos.above(), ModBlocks.ABYSS_CATALYTIC_CHAMBER_MIDDLE.get());
    }

    private static void removeStructurePart(Level level, BlockPos pos, Block expectedBlock) {
        if (level.getBlockState(pos).is(expectedBlock)) {
            level.removeBlock(pos, false);
        }
    }
}
