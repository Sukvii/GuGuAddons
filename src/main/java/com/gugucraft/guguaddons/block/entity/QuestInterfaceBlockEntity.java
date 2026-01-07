package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.registry.ModBlockEntities;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.block.neoforge.NeoForgeTaskScreenBlockEntity;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.net.BlockConfigResponseMessage;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.ConfigQuestObject;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.UUID;

import com.gugucraft.guguaddons.block.custom.QuestInputBlock;
import com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock;
import com.gugucraft.guguaddons.block.custom.QuestSubmissionBlock;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class QuestInterfaceBlockEntity extends NeoForgeTaskScreenBlockEntity {

    // Instantiate ItemHandler as a member variable to implement caching
    private final IItemHandler itemHandler = new IItemHandler() {
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
            // The main block no longer accepts direct input, forcing the use of a dedicated submission interface
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // This is an input-only interface
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

    /**
     * Dedicated item submission logic, called by QuestSubmissionBlock
     */
    public ItemStack submitItem(ItemStack stack, boolean simulate) {
        // Get Team ID
        UUID teamId = this.getTeamId();
        if (teamId == null) return stack;

        // Get bound task
        Task t = this.getTask();
        // Ensure the task is valid and is an item submission task
        if (t == null || !(t instanceof ItemTask itemTask)) return stack;

        // [Logic] Check structure speed, if absolute value is less than 256 RPM, it does not work
        if (Math.abs(this.getStructureSpeed()) < 256.0f) return stack;

        // Check team data
        if (level == null) return stack;
        TeamData data = FTBQuestsAPI.api().getQuestFile(level.isClientSide).getNullableTeamData(teamId);
        if (data == null || !data.canStartTasks(t.getQuest())) {
            return stack;
        }

        // Attempt to submit item to task
        return itemTask.insert(data, stack, simulate);
    }

    public QuestInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<?> getType() {
        return ModBlockEntities.QUEST_INTERFACE.get();
    }

    @Override
    public Task getTask() {
        Task t = super.getTask();
        // Allow retrieving task on server side to fix potential synchronization issues
        if (t == null && level != null && !level.isClientSide) {
            return t;
        }
        return t;
    }

    public static final java.util.Set<QuestInterfaceBlockEntity> TRACKED_INTERFACES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            TRACKED_INTERFACES.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            TRACKED_INTERFACES.remove(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) {
            TRACKED_INTERFACES.remove(this);
        }
    }
    
    public void tick(Level level, BlockPos pos, BlockState state) {
        // If there is no special logic, it is recommended to remove registration in the Block class's getTicker to improve TPS
    }

    // [Optimization] 2. Directly return the cached itemHandler object
    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }

    // --- Structure Detection Logic ---

    private static final String[][] PATTERN = {
            // Depth 0 (Front)
            {
                    "DDDDD",
                    "GGGGG",
                    "GGIGG",
                    "DDDDD"
            },
            // Depth 1 (Middle)
            {
                    "DDDDD",
                    "DSCSD",
                    "DSCSD",
                    "DDDDD"
            },
            // Depth 2 (Back)
            {
                    "DDDDD",
                    "GGGGG",
                    "GGGGG",
                    "DDDDD"
            }
    };

    private boolean isFormed = false;
    private boolean structureDirty = true;
    private QuestInputBlockEntity cachedInput = null;
    private float syncedSpeed = 0.0f;

    public void setStructureDirty() {
        this.structureDirty = true;
    }

    private void checkStructure() {
        if (level == null || level.isClientSide) {
             structureDirty = false;
             return;
        }

        boolean wasFormed = this.isFormed;
        float oldSpeed = this.syncedSpeed;

        BlockPos centerPos = this.getBlockPos();
        BlockState centerState = level.getBlockState(centerPos);

        if (!centerState.hasProperty(QuestInterfaceBlock.FACING)) {
            isFormed = false;
            cachedInput = null;
            syncedSpeed = 0.0f;
            structureDirty = false;
            updateStateIfChanged(wasFormed, oldSpeed);
            return;
        }

        Direction facing = centerState.getValue(QuestInterfaceBlock.FACING);
        Direction left = facing.getClockWise();
        Direction up = Direction.UP;
        Direction backwards = facing.getOpposite();
        Direction.Axis horizontalAxis = left.getAxis();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int inputCount = 0;
        int submissionCount = 0;
        QuestInputBlockEntity foundInput = null;
        boolean structureValid = true;

        for (int d = 0; d < PATTERN.length; d++) {
            String[] layer = PATTERN[d];
            for (int row = 0; row < layer.length; row++) {
                String rowStr = layer[row];
                for (int col = 0; col < rowStr.length(); col++) {
                    char key = rowStr.charAt(col);

                    int v = 2 - row;
                    int h = 2 - col;

                    mutablePos.set(centerPos);
                    if (d != 0) mutablePos.move(backwards, d);
                    if (h != 0) mutablePos.move(left, h);
                    if (v != 0) mutablePos.move(up, v);

                    BlockState state = level.getBlockState(mutablePos);
                    Block block = state.getBlock();

                    if (key == 'D') {
                        if (state.is(ModBlocks.DEDUCTION_CASING.get())) continue;
                        if (block instanceof QuestInputBlock) {
                            inputCount++;
                            BlockEntity be = level.getBlockEntity(mutablePos);
                            if (be instanceof QuestInputBlockEntity inputBE) {
                                foundInput = inputBE;
                            }
                            continue;
                        }
                        if (block instanceof QuestSubmissionBlock) {
                            submissionCount++;
                            continue;
                        }
                        structureValid = false;
                    } else if (key == 'G') {
                        if (!AllPaletteBlocks.FRAMED_GLASS_PANE.has(state)) structureValid = false;
                    } else if (key == 'S') {
                        if (!AllBlocks.SHAFT.has(state)) structureValid = false;
                        else if (!state.hasProperty(BlockStateProperties.AXIS) || state.getValue(BlockStateProperties.AXIS) != horizontalAxis) structureValid = false;
                    } else if (key == 'C') {
                        if (!AllBlocks.COGWHEEL.has(state)) structureValid = false;
                        else if (!state.hasProperty(BlockStateProperties.AXIS) || state.getValue(BlockStateProperties.AXIS) != horizontalAxis) structureValid = false;
                    } else if (key == 'I') {
                        if (!state.is(ModBlocks.QUEST_INTERFACE_BLOCK.get())) structureValid = false;
                    } else if (key == ' ') {
                        // ignore
                    } else {
                        structureValid = false;
                    }

                    if (!structureValid) break;
                }
                if (!structureValid) break;
            }
            if (!structureValid) break;
        }

        if (structureValid && inputCount <= 1 && submissionCount <= 1) {
            isFormed = true;
            cachedInput = foundInput;
            syncedSpeed = (cachedInput != null && !cachedInput.isRemoved()) ? cachedInput.getSpeed() : 0.0f;
        } else {
            isFormed = false;
            cachedInput = null;
            syncedSpeed = 0.0f;
        }
        structureDirty = false;
        
        updateStateIfChanged(wasFormed, oldSpeed);
    }

    private void updateStateIfChanged(boolean wasFormed, float oldSpeed) {
        // If speed or formation state changed, sync
        if (wasFormed != this.isFormed || Math.abs(oldSpeed - this.syncedSpeed) > 0.01f) {
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsFormed", isFormed);
        tag.putFloat("StructureSpeed", syncedSpeed);
    }

    @Override
    public void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isFormed = tag.getBoolean("IsFormed");
        syncedSpeed = tag.getFloat("StructureSpeed");
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        loadAdditional(tag, registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, net.minecraft.core.HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }


    public boolean isStructureFormed() {
        if (level != null && !level.isClientSide) {
            if (structureDirty) {
                checkStructure();
            }
        }
        return isFormed;
    }

    public boolean isBlockInStructure(BlockPos pos) {
        if (level == null) return false;
        BlockPos centerPos = this.getBlockPos();
        BlockState centerState = level.getBlockState(centerPos);
        if (!centerState.hasProperty(QuestInterfaceBlock.FACING)) return false;

        Direction facing = centerState.getValue(QuestInterfaceBlock.FACING);
        Direction left = facing.getClockWise();
        Direction up = Direction.UP;
        Direction backwards = facing.getOpposite();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int d = 0; d < PATTERN.length; d++) {
            String[] layer = PATTERN[d];
            for (int row = 0; row < layer.length; row++) {
                String rowStr = layer[row];
                for (int col = 0; col < rowStr.length(); col++) {
                    if (rowStr.charAt(col) == ' ') continue;

                    int v = 2 - row;
                    int h = 2 - col;

                    mutablePos.set(centerPos);
                    if (d != 0) mutablePos.move(backwards, d);
                    if (h != 0) mutablePos.move(left, h);
                    if (v != 0) mutablePos.move(up, v);

                    if (mutablePos.equals(pos)) return true;
                }
            }
        }
        return false;
    }

    // --- GUI Configuration Related ---

    @Override
    public ConfigGroup fillConfigGroup(TeamData data) {
        ConfigGroup cg0 = new ConfigGroup("task_screen", accepted -> {
            if (accepted) {
                if (getLevel() != null) {
                    NetworkManager.sendToServer(new BlockConfigResponseMessage(getBlockPos(), saveWithoutMetadata(getLevel().registryAccess())));
                }
            }
        });

        cg0.setNameKey(getBlockState().getBlock().getDescriptionId());
        ConfigGroup cg = cg0.getOrCreateSubgroup("screen");
        cg.add("task", new ConfigQuestObject<>(o -> isSuitableTask(data, o), this::formatLine), getTask(), this::setTask, null).setNameKey("ftbquests.task");

        return cg0;
    }

    private boolean isSuitableTask(TeamData data, QuestObjectBase o) {
        return o instanceof dev.ftb.mods.ftbquests.quest.task.Task t &&
                (data.getCanEdit(FTBQuestsClient.getClientPlayer()) || data.canStartTasks(t.getQuest())) &&
                t.consumesResources();
    }

    private Component formatLine(dev.ftb.mods.ftbquests.quest.task.Task task) {
        if (task == null) return Component.empty();

        Component questTxt = Component.literal(" [").append(task.getQuest().getTitle()).append("]").withStyle(ChatFormatting.GREEN);
        return ConfigQuestObject.formatEntry(task).copy().append(questTxt);
    }

    // --- Speed Detection Logic (Get the maximum input speed in the structure) ---

    public float getStructureSpeed() {
        if (level != null && !level.isClientSide) {
            if (structureDirty) {
                checkStructure();
            }
            // Update speed if input is present (in case speed changed without structure change)
            if (isFormed && cachedInput != null && !cachedInput.isRemoved()) {
                float currentInputSpeed = cachedInput.getSpeed();
                if (Math.abs(currentInputSpeed - syncedSpeed) > 0.01f) {
                   syncedSpeed = currentInputSpeed;
                   setChanged();
                   level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                }
            }
        }
        return syncedSpeed;
    }
}