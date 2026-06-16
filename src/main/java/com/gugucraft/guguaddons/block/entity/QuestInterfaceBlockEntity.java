package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.block.custom.QuestInputBlock;
import com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock;
import com.gugucraft.guguaddons.block.custom.QuestSubmissionBlock;
import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.gugucraft.guguaddons.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.block.neoforge.NeoForgeTaskScreenBlockEntity;
import dev.ftb.mods.ftbquests.net.BlockConfigResponseMessage;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.neoforge.ForgeEnergyTask;
import dev.ftb.mods.ftbquests.util.ConfigQuestObject;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

public class QuestInterfaceBlockEntity extends NeoForgeTaskScreenBlockEntity implements IHaveGoggleInformation {
    private static final String[][] PATTERN = {
            {
                    "DDDDD",
                    "GGGGG",
                    "GGIGG",
                    "DDDDD"
            },
            {
                    "DDDDD",
                    "DSCSD",
                    "DSCSD",
                    "DDDDD"
            },
            {
                    "DDDDD",
                    "GGGGG",
                    "GGGGG",
                    "DDDDD"
            }
    };
    private static final int STRUCTURE_RADIUS = 2;
    private static final Comparator<BlockPos> BLOCK_POS_COMPARATOR = Comparator.<BlockPos>comparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getZ);
    private static final Map<Level, LoadedInterfaceState> LOADED_INTERFACE_STATES = Collections
            .synchronizedMap(new WeakHashMap<>());

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
            return stack;
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

    private boolean isFormed;
    private boolean structureCandidate;
    private boolean structureDirty = true;
    private boolean unloadingChunk;
    @Nullable
    private BlockPos cachedInputPos;
    private float candidateSpeed;
    private float syncedSpeed;

    public QuestInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<?> getType() {
        return ModBlockEntities.QUEST_INTERFACE.get();
    }

    @Override
    public void setTeamId(UUID teamId) {
        UUID previousTeamId = getTeamId();
        super.setTeamId(teamId);

        if (level == null || level.isClientSide || Objects.equals(previousTeamId, teamId)) {
            return;
        }

        invalidateTeamSelection(level, previousTeamId);
        refreshTeamInterfaces(level, previousTeamId);
        requestStructureRefresh();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) {
            return;
        }

        structureDirty = true;
        registerLoadedInterface(this);
        refreshCurrentState();
    }

    @Override
    public void setRemoved() {
        Level currentLevel = level;
        UUID currentTeamId = getTeamId();
        super.setRemoved();
        // A genuine block removal can refresh nearby interfaces (world is live).
        // During a chunk unload, super.onChunkUnloaded() also routes here; the
        // unloadingChunk guard keeps that path non-loading.
        unregisterLoadedInterface(this, currentLevel, currentTeamId, !unloadingChunk);
    }

    @Override
    public void onChunkUnloaded() {
        Level currentLevel = level;
        UUID currentTeamId = getTeamId();
        unloadingChunk = true;
        try {
            // super.onChunkUnloaded() may indirectly call setRemoved(); the guard
            // ensures neither path triggers a synchronous structure refresh.
            super.onChunkUnloaded();
        } finally {
            unloadingChunk = false;
        }
        // Unload path: detach from the registry only, never recompute.
        unregisterLoadedInterface(this, currentLevel, currentTeamId, false);
    }

    public void requestStructureRefresh() {
        if (level == null || level.isClientSide) {
            return;
        }

        structureDirty = true;
        invalidateTeamSelection(level, getTeamId());
        refreshCurrentState();
    }

    public void notifyInputSpeedChanged(BlockPos inputPos) {
        if (level == null || level.isClientSide) {
            return;
        }

        if (cachedInputPos != null && cachedInputPos.equals(inputPos) && !structureDirty) {
            refreshSyncedSpeedIfNeeded();
            return;
        }

        if (isBlockInStructure(inputPos)) {
            requestStructureRefresh();
        }
    }

    public static void requestStructureRefreshAround(Level level, BlockPos changedPos) {
        if (level == null || level.isClientSide) {
            return;
        }

        for (QuestInterfaceBlockEntity questInterface : getLoadedInterfaces(level)) {
            if (questInterface.isRemoved() || questInterface.level != level) {
                continue;
            }
            if (questInterface.isBlockInStructure(changedPos)) {
                questInterface.requestStructureRefresh();
            }
        }
    }

    /**
     * Non-loading variant for unload/shutdown paths. Only flags already-loaded
     * interfaces whose structure covers {@code changedPos} as dirty, without
     * recomputing state or touching the world. The next on-demand read (or a
     * later normal refresh) will reconcile. Uses only the interface's own cached
     * block state (isBlockInStructureCached) and never calls level.getBlockState
     * / level.getBlockEntity / level.getChunk, so it cannot trigger synchronous
     * chunk loading during "Saving worlds".
     */
    public static void markStructureDirtyAround(Level level, BlockPos changedPos) {
        if (level == null || level.isClientSide) {
            return;
        }

        for (QuestInterfaceBlockEntity questInterface : getLoadedInterfaces(level)) {
            if (questInterface.isRemoved() || questInterface.level != level) {
                continue;
            }
            if (questInterface.isBlockInStructureCached(changedPos)) {
                questInterface.structureDirty = true;
                invalidateTeamSelection(level, questInterface.getTeamId());
            }
        }
    }

    public static void notifyInputSpeedChangedAround(Level level, BlockPos inputPos) {
        if (level == null || level.isClientSide) {
            return;
        }

        for (QuestInterfaceBlockEntity questInterface : getLoadedInterfaces(level)) {
            if (questInterface.isRemoved() || questInterface.level != level) {
                continue;
            }
            if (questInterface.isBlockInStructure(inputPos)) {
                questInterface.notifyInputSpeedChanged(inputPos);
            }
        }
    }

    public static void requestStructureRefreshForChunk(Level level, ChunkPos chunkPos) {
        if (level == null || level.isClientSide) {
            return;
        }

        for (QuestInterfaceBlockEntity questInterface : getLoadedInterfaces(level)) {
            if (questInterface.intersectsChunk(chunkPos)) {
                questInterface.requestStructureRefresh();
            }
        }
    }

    /**
     * Non-loading variant for the chunk unload / shutdown path. Flags loaded
     * interfaces intersecting {@code chunkPos} as dirty without recomputing
     * state or sending block updates, so it never waits on chunk access.
     */
    public static void markStructureDirtyForChunk(Level level, ChunkPos chunkPos) {
        if (level == null || level.isClientSide) {
            return;
        }

        for (QuestInterfaceBlockEntity questInterface : getLoadedInterfaces(level)) {
            if (questInterface.isRemoved() || questInterface.level != level) {
                continue;
            }
            if (questInterface.intersectsChunk(chunkPos)) {
                questInterface.structureDirty = true;
                invalidateTeamSelection(level, questInterface.getTeamId());
            }
        }
    }

    public ItemStack submitItem(ItemStack stack, boolean simulate) {
        UUID teamId = getTeamId();
        if (teamId == null || teamId.equals(Util.NIL_UUID)) {
            return stack;
        }

        Task task = getTask();
        if (!(task instanceof ItemTask itemTask)) {
            return stack;
        }

        if (Math.abs(getStructureSpeed()) < 256.0f || level == null) {
            return stack;
        }

        TeamData data = FTBQuestsAPI.api().getQuestFile(level.isClientSide).getNullableTeamData(teamId);
        if (data == null || !isSubmittableTask(data, task)) {
            return stack;
        }

        return itemTask.insert(data, stack, simulate);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public boolean isStructureFormed() {
        if (level != null && !level.isClientSide) {
            refreshCurrentState();
        }
        return isFormed;
    }

    public boolean isBlockInStructure(BlockPos pos) {
        if (level == null) {
            return false;
        }

        return isBlockInStructure(pos, level.getBlockState(getBlockPos()));
    }

    /**
     * Non-loading geometry check. Uses the block entity's own cached state
     * ({@link #getBlockState()}) instead of querying the level, so it can be
     * called on unload/shutdown paths without touching level.getBlockState /
     * level.getBlockEntity / level.getChunk.
     */
    public boolean isBlockInStructureCached(BlockPos pos) {
        return isBlockInStructure(pos, getBlockState());
    }

    /**
     * Pure geometry: tests whether {@code pos} is one of the pattern cells for
     * an interface centered here and oriented per {@code centerState}. Reads no
     * world state of its own.
     */
    private boolean isBlockInStructure(BlockPos pos, BlockState centerState) {
        if (!centerState.hasProperty(QuestInterfaceBlock.FACING)) {
            return false;
        }

        Direction facing = centerState.getValue(QuestInterfaceBlock.FACING);
        Direction left = facing.getClockWise();
        Direction backwards = facing.getOpposite();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int depth = 0; depth < PATTERN.length; depth++) {
            String[] layer = PATTERN[depth];
            for (int row = 0; row < layer.length; row++) {
                String rowPattern = layer[row];
                for (int col = 0; col < rowPattern.length(); col++) {
                    if (rowPattern.charAt(col) == ' ') {
                        continue;
                    }

                    mutablePos.set(getBlockPos());
                    if (depth != 0) {
                        mutablePos.move(backwards, depth);
                    }

                    int horizontalOffset = 2 - col;
                    if (horizontalOffset != 0) {
                        mutablePos.move(left, horizontalOffset);
                    }

                    int verticalOffset = 2 - row;
                    if (verticalOffset != 0) {
                        mutablePos.move(Direction.UP, verticalOffset);
                    }

                    if (mutablePos.equals(pos)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public ConfigGroup fillConfigGroup(TeamData data) {
        ConfigGroup root = new ConfigGroup("task_screen", accepted -> {
            if (accepted && getLevel() != null) {
                NetworkManager.sendToServer(new BlockConfigResponseMessage(getBlockPos(),
                        saveWithoutMetadata(getLevel().registryAccess())));
            }
        });

        root.setNameKey(getBlockState().getBlock().getDescriptionId());
        ConfigGroup screen = root.getOrCreateSubgroup("screen");
        screen.add("task", new ConfigQuestObject<>(o -> isSuitableTask(data, o), this::formatLine), getTask(),
                this::setTask, null).setNameKey("ftbquests.task");

        return root;
    }

    public float getStructureSpeed() {
        if (level != null && !level.isClientSide) {
            refreshCurrentState();
            refreshSyncedSpeedIfNeeded();
        }
        return syncedSpeed;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!isStructureFormed()) {
            return false;
        }

        CreateLang.translate("gui.goggles.kinetic_stats")
                .forGoggles(tooltip);

        float speed = Math.abs(getStructureSpeed());
        float stressTotal = QuestInputBlockEntity.STRESS_APPLIED * speed;

        CreateLang.translate("tooltip.stressImpact")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        CreateLang.number(stressTotal)
                .translate("generic.unit.stress")
                .style(ChatFormatting.AQUA)
                .space()
                .add(CreateLang.translate("gui.goggles.at_current_speed")
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        if (speed < 256.0f) {
            tooltip.add(Component.empty());

            CreateLang.translate("tooltip.speedRequirement")
                    .style(ChatFormatting.GOLD)
                    .forGoggles(tooltip);
            Component hint = CreateLang.translate("gui.contraptions.not_fast_enough",
                    Component.translatable(getBlockState().getBlock().getDescriptionId())).component();
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CreateLang.builder().add(component.copy()).forGoggles(tooltip);
            }
        }

        return true;
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsFormed", isFormed);
        tag.putFloat("StructureSpeed", syncedSpeed);
    }

    @Override
    public void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isFormed = tag.getBoolean("IsFormed");
        syncedSpeed = tag.getFloat("StructureSpeed");
        structureCandidate = isFormed;
        candidateSpeed = syncedSpeed;
        cachedInputPos = null;
        structureDirty = true;
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(net.minecraft.nbt.CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        loadAdditional(tag, registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
            net.minecraft.core.HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    private void refreshCurrentState() {
        if (level == null || level.isClientSide) {
            return;
        }

        refreshCandidateStateIfDirty();

        UUID teamId = getTeamId();
        if (hasAssignedTeam(teamId)) {
            refreshTeamSelection(level, teamId);
        } else {
            applyResolvedState(null);
        }
    }

    private void refreshCandidateStateIfDirty() {
        if (!structureDirty || level == null || level.isClientSide) {
            return;
        }

        boolean structureValid = true;
        int inputCount = 0;
        int submissionCount = 0;
        BlockPos foundInputPos = null;

        BlockState centerState = level.getBlockState(getBlockPos());
        if (!centerState.hasProperty(QuestInterfaceBlock.FACING)) {
            structureValid = false;
        } else {
            Direction facing = centerState.getValue(QuestInterfaceBlock.FACING);
            Direction left = facing.getClockWise();
            Direction backwards = facing.getOpposite();
            Direction.Axis horizontalAxis = left.getAxis();

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            for (int depth = 0; depth < PATTERN.length && structureValid; depth++) {
                String[] layer = PATTERN[depth];
                for (int row = 0; row < layer.length && structureValid; row++) {
                    String rowPattern = layer[row];
                    for (int col = 0; col < rowPattern.length(); col++) {
                        char key = rowPattern.charAt(col);
                        if (key == ' ') {
                            continue;
                        }

                        mutablePos.set(getBlockPos());
                        if (depth != 0) {
                            mutablePos.move(backwards, depth);
                        }

                        int horizontalOffset = 2 - col;
                        if (horizontalOffset != 0) {
                            mutablePos.move(left, horizontalOffset);
                        }

                        int verticalOffset = 2 - row;
                        if (verticalOffset != 0) {
                            mutablePos.move(Direction.UP, verticalOffset);
                        }

                        if (!level.isLoaded(mutablePos)) {
                            structureValid = false;
                            break;
                        }

                        BlockState state = level.getBlockState(mutablePos);
                        Block block = state.getBlock();
                        switch (key) {
                            case 'D' -> {
                                if (state.is(ModBlocks.DEDUCTION_CASING.get())) {
                                    continue;
                                }
                                if (block instanceof QuestInputBlock) {
                                    inputCount++;
                                    foundInputPos = mutablePos.immutable();
                                    continue;
                                }
                                if (block instanceof QuestSubmissionBlock) {
                                    submissionCount++;
                                    continue;
                                }
                                structureValid = false;
                            }
                            case 'G' -> {
                                if (!AllPaletteBlocks.FRAMED_GLASS_PANE.has(state)) {
                                    structureValid = false;
                                }
                            }
                            case 'S' -> {
                                if (!AllBlocks.SHAFT.has(state) || !state.hasProperty(BlockStateProperties.AXIS)
                                        || state.getValue(BlockStateProperties.AXIS) != horizontalAxis) {
                                    structureValid = false;
                                }
                            }
                            case 'C' -> {
                                if (!AllBlocks.COGWHEEL.has(state) || !state.hasProperty(BlockStateProperties.AXIS)
                                        || state.getValue(BlockStateProperties.AXIS) != horizontalAxis) {
                                    structureValid = false;
                                }
                            }
                            case 'I' -> {
                                if (!state.is(ModBlocks.QUEST_INTERFACE_BLOCK.get())) {
                                    structureValid = false;
                                }
                            }
                            default -> structureValid = false;
                        }

                        if (!structureValid) {
                            break;
                        }
                    }
                }
            }
        }

        structureCandidate = structureValid && inputCount <= 1 && submissionCount <= 1;
        cachedInputPos = structureCandidate ? foundInputPos : null;
        candidateSpeed = structureCandidate ? resolveInputSpeed(foundInputPos) : 0.0f;
        structureDirty = false;
    }

    private void refreshSyncedSpeedIfNeeded() {
        if (level == null || level.isClientSide || !isFormed || cachedInputPos == null) {
            return;
        }

        float currentInputSpeed = resolveInputSpeed(cachedInputPos);
        if (Math.abs(currentInputSpeed - candidateSpeed) <= 0.01f) {
            return;
        }

        candidateSpeed = currentInputSpeed;
        UUID teamId = getTeamId();
        if (hasAssignedTeam(teamId)) {
            refreshTeamSelection(level, teamId);
        } else {
            applyResolvedState(null);
        }
    }

    private void applyResolvedState(@Nullable BlockPos activePos) {
        UUID teamId = getTeamId();
        boolean formed = structureCandidate && (!hasAssignedTeam(teamId) || getBlockPos().equals(activePos));
        float resolvedSpeed = formed ? candidateSpeed : 0.0f;

        if (formed == isFormed && Math.abs(resolvedSpeed - syncedSpeed) <= 0.01f) {
            return;
        }

        isFormed = formed;
        syncedSpeed = resolvedSpeed;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private float resolveInputSpeed(@Nullable BlockPos inputPos) {
        if (level == null || inputPos == null || !level.isLoaded(inputPos)) {
            return 0.0f;
        }

        BlockEntity blockEntity = level.getBlockEntity(inputPos);
        return blockEntity instanceof QuestInputBlockEntity inputBlockEntity ? inputBlockEntity.getSpeed() : 0.0f;
    }

    private boolean intersectsChunk(ChunkPos chunkPos) {
        int minBlockX = chunkPos.getMinBlockX();
        int maxBlockX = chunkPos.getMaxBlockX();
        int minBlockZ = chunkPos.getMinBlockZ();
        int maxBlockZ = chunkPos.getMaxBlockZ();

        int structureMinX = getBlockPos().getX() - STRUCTURE_RADIUS;
        int structureMaxX = getBlockPos().getX() + STRUCTURE_RADIUS;
        int structureMinZ = getBlockPos().getZ() - STRUCTURE_RADIUS;
        int structureMaxZ = getBlockPos().getZ() + STRUCTURE_RADIUS;

        return structureMaxX >= minBlockX && structureMinX <= maxBlockX
                && structureMaxZ >= minBlockZ && structureMinZ <= maxBlockZ;
    }

    private boolean isSuitableTask(TeamData data, QuestObjectBase object) {
        return object instanceof Task task && isSubmittableTask(data, task);
    }

    private boolean isSubmittableTask(TeamData data, Task task) {
        return isTaskScreenSubmittableTask(task)
                && data.areDependenciesComplete(task.getQuest())
                && data.canStartTasks(task.getQuest())
                && !data.isCompleted(task);
    }

    private boolean isTaskScreenSubmittableTask(Task task) {
        return (task instanceof ItemTask itemTask && itemTask.canInsertItem())
                || task instanceof FluidTask
                || task instanceof ForgeEnergyTask;
    }

    private Component formatLine(Task task) {
        if (task == null) {
            return Component.empty();
        }

        Component questText = Component.literal(" [").append(task.getQuest().getTitle()).append("]")
                .withStyle(ChatFormatting.GREEN);
        return ConfigQuestObject.formatEntry(task).copy().append(questText);
    }

    private static void registerLoadedInterface(QuestInterfaceBlockEntity blockEntity) {
        LoadedInterfaceState state = getLoadedInterfaceState(blockEntity.level);
        state.loadedInterfaces.remove(blockEntity);
        state.loadedInterfaces.add(blockEntity);
        invalidateTeamSelection(blockEntity.level, blockEntity.getTeamId());
    }

    private static void unregisterLoadedInterface(QuestInterfaceBlockEntity blockEntity, @Nullable Level level,
            @Nullable UUID teamId, boolean refreshNow) {
        if (level == null || level.isClientSide) {
            return;
        }

        LoadedInterfaceState state = getLoadedInterfaceState(level);
        state.loadedInterfaces.remove(blockEntity);
        state.loadedInterfaces.removeIf(loadedInterface -> loadedInterface.isRemoved() || loadedInterface.level != level);
        invalidateTeamSelection(level, teamId);
        // refreshNow == false on the unload/shutdown path: only detach + mark the
        // team selection dirty. Recomputing here (refreshTeamInterfaces) would
        // reach level.getBlockState / level.getBlockEntity and can stall the
        // server thread during "Saving worlds".
        if (refreshNow) {
            refreshTeamInterfaces(level, teamId);
        }
    }

    private static void invalidateTeamSelection(@Nullable Level level, @Nullable UUID teamId) {
        if (level == null || level.isClientSide || !hasAssignedTeam(teamId)) {
            return;
        }

        getLoadedInterfaceState(level).teamSelections
                .computeIfAbsent(teamId, ignored -> new TeamSelection())
                .dirty = true;
    }

    private static void refreshTeamInterfaces(@Nullable Level level, @Nullable UUID teamId) {
        if (level == null || level.isClientSide || !hasAssignedTeam(teamId)) {
            return;
        }

        refreshTeamSelection(level, teamId);
    }

    private static void refreshTeamSelection(Level level, UUID teamId) {
        LoadedInterfaceState state = getLoadedInterfaceState(level);
        TeamSelection selection = state.teamSelections.computeIfAbsent(teamId, ignored -> new TeamSelection());
        if (!selection.dirty) {
            return;
        }

        List<QuestInterfaceBlockEntity> teamInterfaces = new ArrayList<>();
        for (QuestInterfaceBlockEntity blockEntity : getLoadedInterfaces(level)) {
            if (!blockEntity.isRemoved() && teamId.equals(blockEntity.getTeamId())) {
                blockEntity.refreshCandidateStateIfDirty();
                teamInterfaces.add(blockEntity);
            }
        }

        BlockPos activePos = null;
        if (selection.activePos != null) {
            for (QuestInterfaceBlockEntity blockEntity : teamInterfaces) {
                if (blockEntity.structureCandidate && blockEntity.getBlockPos().equals(selection.activePos)) {
                    activePos = selection.activePos;
                    break;
                }
            }
        }

        if (activePos == null) {
            activePos = teamInterfaces.stream()
                    .filter(blockEntity -> blockEntity.structureCandidate)
                    .map(QuestInterfaceBlockEntity::getBlockPos)
                    .min(BLOCK_POS_COMPARATOR)
                    .orElse(null);
        }

        selection.activePos = activePos;
        selection.dirty = false;

        for (QuestInterfaceBlockEntity blockEntity : teamInterfaces) {
            blockEntity.applyResolvedState(activePos);
        }
    }

    private static LoadedInterfaceState getLoadedInterfaceState(Level level) {
        synchronized (LOADED_INTERFACE_STATES) {
            return LOADED_INTERFACE_STATES.computeIfAbsent(level, ignored -> new LoadedInterfaceState());
        }
    }

    private static List<QuestInterfaceBlockEntity> getLoadedInterfaces(Level level) {
        return new ArrayList<>(getLoadedInterfaceState(level).loadedInterfaces);
    }

    private static boolean hasAssignedTeam(@Nullable UUID teamId) {
        return teamId != null && !teamId.equals(Util.NIL_UUID);
    }

    private static final class LoadedInterfaceState {
        private final List<QuestInterfaceBlockEntity> loadedInterfaces = new ArrayList<>();
        private final Map<UUID, TeamSelection> teamSelections = new java.util.HashMap<>();
    }

    private static final class TeamSelection {
        @Nullable
        private BlockPos activePos;
        private boolean dirty = true;
    }
}
