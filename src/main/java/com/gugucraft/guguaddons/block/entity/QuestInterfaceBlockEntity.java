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

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.net.BlockConfigResponseMessage;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.util.ConfigQuestObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;


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

                 if (Math.abs(QuestInterfaceBlockEntity.this.getStructureSpeed()) < 16.0f) return stack;

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

    private static final java.util.Map<Character, java.util.function.Predicate<BlockState>> PALETTE = java.util.Map.of(
        'S', state -> state.is(net.minecraft.world.level.block.Blocks.STONE) || state.getBlock() instanceof com.gugucraft.guguaddons.block.custom.QuestInputBlock,
        'I', state -> state.getBlock() instanceof com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock,
        ' ', state -> true
    );

    private static final String[][] PATTERN = {
        // Depth 0 (Front)
        {
            "SSS", // y=1 (Top)
            "SIS", // y=0 (Center)
            "SSS"  // y=-1 (Bottom)
        },
        // Depth 1
        {
            "SSS",
            "SSS",
            "SSS"
        },
        // Depth 2
        {
            "SSS",
            "SSS",
            "SSS"
        }
    };

    public boolean isStructureFormed() {
        if (level == null) return false;
        BlockState state = level.getBlockState(this.getBlockPos());
        if (!state.hasProperty(com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.FACING)) return false;
        
        net.minecraft.core.Direction facing = state.getValue(com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.FACING);
        net.minecraft.core.Direction backwards = facing.getOpposite();
        net.minecraft.core.Direction left = facing.getClockWise();
        net.minecraft.core.Direction up = net.minecraft.core.Direction.UP;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int d = 0; d < PATTERN.length; d++) {
            String[] layer = PATTERN[d];
            for (int row = 0; row < layer.length; row++) {
                String rowStr = layer[row];
                for (int col = 0; col < rowStr.length(); col++) {
                    char key = rowStr.charAt(col);
                    if (key == ' ') continue;

                    // Row 0 is Top (v=1), Row 2 is Bottom (v=-1)
                    int v = 1 - row;
                    // Col 0 is Left (h=1), Col 2 is Right (h=-1)
                    int h = 1 - col;

                    mutablePos.set(this.getBlockPos());
                    mutablePos.move(backwards, d);
                    mutablePos.move(left, h);
                    mutablePos.move(up, v);

                    BlockState targetState = level.getBlockState(mutablePos);
                    java.util.function.Predicate<BlockState> predicate = PALETTE.get(key);
                    
                    if (predicate == null || !predicate.test(targetState)) {
                        return false;
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

        for (int d = 0; d < PATTERN.length; d++) {
            String[] layer = PATTERN[d];
            for (int row = 0; row < layer.length; row++) {
                String rowStr = layer[row];
                for (int col = 0; col < rowStr.length(); col++) {
                    int v = 1 - row;
                    int h = 1 - col;

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

    @Override
    public ConfigGroup fillConfigGroup(TeamData data) {
        ConfigGroup cg0 = new ConfigGroup("task_screen", accepted -> {
            if (accepted) {
                NetworkManager.sendToServer(new BlockConfigResponseMessage(getBlockPos(), saveWithoutMetadata(getLevel().registryAccess())));
            }
        });

        cg0.setNameKey(getBlockState().getBlock().getDescriptionId());
        ConfigGroup cg = cg0.getOrCreateSubgroup("screen");
        cg.add("task", new ConfigQuestObject<>(o -> isSuitableTask(data, o), this::formatLine), getTask(), this::setTask, null).setNameKey("ftbquests.task");

        return cg0;
    }

    private boolean isSuitableTask(TeamData data, QuestObjectBase o) {
        return o instanceof dev.ftb.mods.ftbquests.quest.task.Task t && (data.getCanEdit(FTBQuestsClient.getClientPlayer()) || data.canStartTasks(t.getQuest())) && t.consumesResources();
    }

    private Component formatLine(dev.ftb.mods.ftbquests.quest.task.Task task) {
        if (task == null) return Component.empty();

        Component questTxt = Component.literal(" [").append(task.getQuest().getTitle()).append("]").withStyle(ChatFormatting.GREEN);
        return ConfigQuestObject.formatEntry(task).copy().append(questTxt);
    }

    public float getStructureSpeed() {
        if (level == null) return 0;
        BlockState state = level.getBlockState(this.getBlockPos());
        if (!state.hasProperty(com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.FACING)) return 0;

        net.minecraft.core.Direction facing = state.getValue(com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.FACING);
        net.minecraft.core.Direction backwards = facing.getOpposite();
        net.minecraft.core.Direction left = facing.getClockWise();
        net.minecraft.core.Direction up = net.minecraft.core.Direction.UP;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int d = 0; d < PATTERN.length; d++) {
            String[] layer = PATTERN[d];
            for (int row = 0; row < layer.length; row++) {
                String rowStr = layer[row];
                for (int col = 0; col < rowStr.length(); col++) {
                    char key = rowStr.charAt(col);
                    if (key != 'S') continue;

                    int v = 1 - row;
                    int h = 1 - col;

                    mutablePos.set(this.getBlockPos());
                    mutablePos.move(backwards, d);
                    mutablePos.move(left, h);
                    mutablePos.move(up, v);

                    BlockEntity be = level.getBlockEntity(mutablePos);
                    // System.out.println("Scanning pos: " + mutablePos + " found BE: " + (be == null ? "null" : be.getClass().getSimpleName()));
                    
                    if (be instanceof com.gugucraft.guguaddons.block.entity.QuestInputBlockEntity inputBE) {
                        float speed = inputBE.getSpeed();
                        // System.out.println("Found QuestInputBlockEntity at " + mutablePos + " with speed: " + speed);
                        if (Math.abs(speed) >= 16.0f) return speed;
                    }
                }
            }
        }
        return 0;
    }
}
