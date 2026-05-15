package com.gugucraft.guguaddons.block.custom;

import com.gugucraft.guguaddons.block.entity.AbyssCatalyticChamberTopBlockEntity;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AbyssCatalyticChamberTopBlock extends Block implements IBE<AbyssCatalyticChamberTopBlockEntity>, IWrenchable {
    public AbyssCatalyticChamberTopBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<AbyssCatalyticChamberTopBlockEntity> getBlockEntityClass() {
        return AbyssCatalyticChamberTopBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AbyssCatalyticChamberTopBlockEntity> getBlockEntityType() {
        return ModBlockEntities.ABYSS_CATALYTIC_CHAMBER_TOP.get();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return AbyssCatalyticChamberBlock.isTopBoundToBottom(level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()
                && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }

        AbyssCatalyticChamberBlock.destroyChamber(level, pos.below(2), true);
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            AbyssCatalyticChamberBlock.destroyChamber(level, pos.below(2), false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
            BlockPos currentPos, BlockPos facingPos) {
        if (level instanceof Level actualLevel && !actualLevel.isClientSide()
                && !actualLevel.getBlockTicks().hasScheduledTick(currentPos, this)) {
            actualLevel.scheduleTick(currentPos, this, 1);
        }
        return state;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos bottomPos = context.getClickedPos().below(2);
        BlockState bottomState = level.getBlockState(bottomPos);
        if (!(bottomState.getBlock() instanceof IWrenchable wrenchable)) {
            return InteractionResult.PASS;
        }

        UseOnContext redirectedContext = new UseOnContext(level, context.getPlayer(), context.getHand(),
                context.getItemInHand(), new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                        bottomPos, context.isInside()));
        return wrenchable.onSneakWrenched(bottomState, redirectedContext);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
            Player player) {
        return new ItemStack(ModBlocks.ABYSS_CATALYTIC_CHAMBER.get());
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
}
