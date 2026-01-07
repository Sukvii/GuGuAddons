package com.gugucraft.guguaddons.block.custom;

import com.gugucraft.guguaddons.block.entity.QuestInterfaceBlockEntity;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import com.mojang.serialization.MapCodec;

public class QuestInterfaceBlock extends BaseEntityBlock {
    public static final MapCodec<QuestInterfaceBlock> CODEC = simpleCodec(QuestInterfaceBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

    public QuestInterfaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(
            net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuestInterfaceBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof ServerPlayer player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof QuestInterfaceBlockEntity questInterface) {
                // Auto-bind to player's team
                FTBTeamsAPI.api().getManager().getTeamForPlayer(player).ifPresent(team -> {
                    UUID teamId = team.getId();
                    questInterface.setTeamId(teamId);
                });
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof QuestInterfaceBlockEntity questInterface && player instanceof ServerPlayer sp) {
                if (questInterface.isStructureFormed()) {
                    float currentSpeed = questInterface.getStructureSpeed();
                    if (Math.abs(currentSpeed) < 256.0f) {
                        player.displayClientMessage(
                                Component.translatable("message.guguaddons.interface_speed_fail", currentSpeed), true);
                        return InteractionResult.SUCCESS;
                    }

                    dev.architectury.networking.NetworkManager.sendToPlayer(sp,
                            new dev.ftb.mods.ftbquests.net.BlockConfigRequestMessage(pos,
                                    dev.ftb.mods.ftbquests.net.BlockConfigRequestMessage.BlockType.TASK_SCREEN));
                    return InteractionResult.SUCCESS;
                } else {
                    player.displayClientMessage(Component.translatable("message.guguaddons.structure_not_formed"),
                            true);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof QuestInterfaceBlockEntity questInterface) {
                questInterface.setStructureDirty();
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.QUEST_INTERFACE.get(),
                (level1, pos, state1, blockEntity) -> blockEntity.tick(level1, pos, state1));

    }
}
