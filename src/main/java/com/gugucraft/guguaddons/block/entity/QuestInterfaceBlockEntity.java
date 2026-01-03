package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.registry.ModBlockEntities;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.block.neoforge.NeoForgeTaskScreenBlockEntity;
import dev.ftb.mods.ftbquests.quest.task.Task; // Changed from dev.ftb.mods.ftbquests.book.Task
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.TeamData; // Changed from dev.ftb.mods.ftbquests.book.team.TeamData
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class QuestInterfaceBlockEntity extends NeoForgeTaskScreenBlockEntity {
    public QuestInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<?> getType() {
        return ModBlockEntities.QUEST_INTERFACE.get();
    }

    // Fields and accessors are inherited from TaskScreenBlockEntity.
    // Overriding getTask to ensure it loads on Server side if needed, but super might handle it.
    // However, super.getTask uses clientSide check. We might need server side access.
    
    @Override
    public Task getTask() {
        Task t = super.getTask();
        if (t == null && level != null && !level.isClientSide) {
             return t;
        }
        return t;
    }
    
    // We do NOT need to override saveAdditional/loadAdditional as TaskScreenBlockEntity handles it.
    // We do NOT need setTeamId/getTaskId etc as they are inherited.


    public void tick(Level level, BlockPos pos, BlockState state) {
        // Logic if needed, e.g. periodically checking valid task
    }

    // save/load handled by super

    // Capability implementation would go here (IItemHandler, etc.)
    // Since direct capability implementation in the BE class is deprecated/changed in NeoForge,
    // we should register capabilities in the Mod or use the new registerBlockEntityCapabilities.
    // However, for simplicity in 1.21, we can implement the handling logic here and register the provider.

    public IItemHandler getItemHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                 UUID teamId = getTeamId();
                 if (teamId == null) return stack;
                 // taskId is internal in super, use getTask().id (but task might be null).
                 
                 Task t = getTask();
                 if (t == null || !(t instanceof ItemTask itemTask)) return stack;

                 TeamData data = FTBQuestsAPI.api().getQuestFile(level.isClientSide).getNullableTeamData(teamId);
                 if (data == null || !data.canStartTasks(t.getQuest())) {
                     return stack;
                 }

                 return itemTask.insert(data, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return true;
            }
        };
    }

    public boolean isStructureFormed() {
        if (level == null) return false;
        BlockState state = level.getBlockState(this.getBlockPos());
        if (!state.hasProperty(com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.FACING)) return false;
        
        net.minecraft.core.Direction facing = state.getValue(com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.FACING);
        net.minecraft.core.Direction backwards = facing.getOpposite();
        net.minecraft.core.Direction left = facing.getClockWise();
        net.minecraft.core.Direction up = net.minecraft.core.Direction.UP;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int d = 0; d < 3; d++) {
            for (int h = -1; h <= 1; h++) {
                for (int v = -1; v <= 1; v++) {
                    mutablePos.set(this.getBlockPos());
                    mutablePos.move(backwards, d);
                    mutablePos.move(left, h);
                    mutablePos.move(up, v);

                    if (d == 0 && h == 0 && v == 0) {
                        if (!mutablePos.equals(this.getBlockPos())) return false;
                    } else {
                        if (!level.getBlockState(mutablePos).is(net.minecraft.world.level.block.Blocks.STONE)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean isBlockInStructure(BlockPos pos) {
        if (level == null) return false;
        BlockState state = level.getBlockState(this.getBlockPos());
        if (!state.hasProperty(com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.FACING)) return false;

        net.minecraft.core.Direction facing = state.getValue(com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.FACING);
        net.minecraft.core.Direction backwards = facing.getOpposite();
        net.minecraft.core.Direction left = facing.getClockWise();
        net.minecraft.core.Direction up = net.minecraft.core.Direction.UP;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int d = 0; d < 3; d++) {
            for (int h = -1; h <= 1; h++) {
                for (int v = -1; v <= 1; v++) {
                    mutablePos.set(this.getBlockPos());
                    mutablePos.move(backwards, d);
                    mutablePos.move(left, h);
                    mutablePos.move(up, v);
                    if (mutablePos.equals(pos)) return true;
                }
            }
        }
        return false;
    }
}
