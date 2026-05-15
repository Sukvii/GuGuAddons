package com.gugucraft.guguaddons.block.custom;

import com.gugucraft.guguaddons.block.entity.CentrifugeBlockEntity;
import com.gugucraft.guguaddons.block.entity.CentrifugeStructuralBlockEntity;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class CentrifugeStructuralBlock extends DirectionalBlock
        implements IBE<CentrifugeStructuralBlockEntity>, IWrenchable, IProxyHoveringInformation {

    public static final MapCodec<CentrifugeStructuralBlock> CODEC = simpleCodec(CentrifugeStructuralBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 2, 0, 16, 14, 16);

    public CentrifugeStructuralBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(ModBlocks.CENTRIFUGE.get());
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();

        if (stillValid(level, clickedPos, state, false)) {
            BlockPos masterPos = getMaster(level, clickedPos, state);
            context = new UseOnContext(level, context.getPlayer(), context.getHand(), context.getItemInHand(),
                    new BlockHitResult(context.getClickLocation(), context.getClickedFace(), masterPos, context.isInside()));
            state = level.getBlockState(masterPos);
        }

        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (!stillValid(level, pos, state, false)) {
            return ItemInteractionResult.FAIL;
        }
        CentrifugeBlockEntity master = getMasterBlockEntity(level, pos, state);
        if (master == null) {
            return ItemInteractionResult.FAIL;
        }
        return master.handleItemUse(player, hand, stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!stillValid(level, pos, state, false)) {
            return InteractionResult.FAIL;
        }
        CentrifugeBlockEntity master = getMasterBlockEntity(level, pos, state);
        return master == null ? InteractionResult.FAIL : master.handleEmptyHandUse(player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }
        if (stillValid(level, pos, state, false)) {
            level.destroyBlock(getMaster(level, pos, state), true);
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (stillValid(level, pos, state, false)) {
            BlockPos masterPos = getMaster(level, pos, state);
            level.destroyBlockProgress(masterPos.hashCode(), masterPos, -1);
            if (!level.isClientSide() && player.isCreative()) {
                level.destroyBlock(masterPos, false);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
            BlockPos currentPos, BlockPos facingPos) {
        if (stillValid(level, currentPos, state, false)) {
            BlockPos masterPos = getMaster(level, currentPos, state);
            if (!level.getBlockTicks().hasScheduledTick(masterPos, ModBlocks.CENTRIFUGE.get())) {
                level.scheduleTick(masterPos, ModBlocks.CENTRIFUGE.get(), 1);
            }
            return state;
        }
        if (!(level instanceof Level actualLevel) || actualLevel.isClientSide()) {
            return state;
        }
        if (!actualLevel.getBlockTicks().hasScheduledTick(currentPos, this)) {
            actualLevel.scheduleTick(currentPos, this, 1);
        }
        return state;
    }

    public static BlockPos getMaster(BlockGetter level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos targetedPos = pos.relative(direction);
        BlockState targetedState = level.getBlockState(targetedPos);
        if (targetedState.is(ModBlocks.CENTRIFUGE_STRUCTURE.get())) {
            return getMaster(level, targetedPos, targetedState);
        }
        return targetedPos;
    }

    public boolean stillValid(BlockGetter level, BlockPos pos, BlockState state, boolean directlyAdjacent) {
        if (!state.is(this)) {
            return false;
        }

        Direction direction = state.getValue(FACING);
        BlockPos targetedPos = pos.relative(direction);
        BlockState targetedState = level.getBlockState(targetedPos);

        if (!directlyAdjacent && stillValid(level, targetedPos, targetedState, true)) {
            return true;
        }
        return targetedState.is(ModBlocks.CENTRIFUGE.get());
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!stillValid(level, pos, state, false)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    @Override
    public boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2,
            LivingEntity entity, int numberOfParticles) {
        BlockState particleState = resolveParticleState(level, pos, state1);
        if (particleState.isAir()) {
            return super.addLandingEffects(state1, level, pos, state2, entity, numberOfParticles);
        }

        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, particleState).setPos(pos),
                entity.getX(), entity.getY(), entity.getZ(), numberOfParticles, 0.0D, 0.0D, 0.0D, 0.15D);
        return true;
    }

    @Override
    public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) {
        BlockState particleState = resolveParticleState(level, pos, state);
        if (particleState.isAir()) {
            return super.addRunningEffects(state, level, pos, entity);
        }

        var movement = entity.getDeltaMovement();
        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, particleState).setPos(pos),
                entity.getX() + (level.random.nextDouble() - 0.5D) * entity.getBbWidth(),
                entity.getY() + 0.1D,
                entity.getZ() + (level.random.nextDouble() - 0.5D) * entity.getBbWidth(),
                movement.x * -4.0D, 1.5D, movement.z * -4.0D);
        return true;
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        super.updateEntityAfterFallOn(level, entity);
        if (!(entity instanceof ItemEntity itemEntity) || entity.level().isClientSide()) {
            return;
        }

        BlockPos pos = itemEntity.blockPosition();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CentrifugeStructuralBlock structuralBlock)
                || !structuralBlock.stillValid(level, pos, state, false)) {
            return;
        }

        BlockPos masterPos = getMaster(level, pos, state);
        if (!(entity.level().getBlockEntity(masterPos) instanceof CentrifugeBlockEntity master)) {
            return;
        }
        master.absorbItemEntity(itemEntity);
    }

    @Override
    public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        return stillValid(level, pos, state, false) ? getMaster(level, pos, state) : pos;
    }

    @Override
    public Class<CentrifugeStructuralBlockEntity> getBlockEntityClass() {
        return CentrifugeStructuralBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CentrifugeStructuralBlockEntity> getBlockEntityType() {
        return ModBlockEntities.CENTRIFUGE_STRUCTURE.get();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private CentrifugeBlockEntity getMasterBlockEntity(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(getMaster(level, pos, state)) instanceof CentrifugeBlockEntity centrifuge ? centrifuge : null;
    }

    @Override
    protected @NotNull MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    public BlockState resolveParticleState(Level level, BlockPos pos, BlockState state) {
        if (stillValid(level, pos, state, false)) {
            return level.getBlockState(getMaster(level, pos, state));
        }
        return ModBlocks.CENTRIFUGE.get().defaultBlockState();
    }
}
