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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.UUID;

public class QuestInterfaceBlockEntity extends NeoForgeTaskScreenBlockEntity {

    // [优化] 1. 将 ItemHandler 实例化为成员变量，实现缓存
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
            // 获取团队 ID
            UUID teamId = QuestInterfaceBlockEntity.this.getTeamId();
            if (teamId == null) return stack;

            // 获取绑定的任务
            Task t = QuestInterfaceBlockEntity.this.getTask();
            // 确保任务有效且是物品提交任务
            if (t == null || !(t instanceof ItemTask itemTask)) return stack;

            // [逻辑] 检查结构转速，如果绝对值小于 16 RPM 则不工作
            // 使用 QuestInterfaceBlockEntity.this 访问外部类方法
            if (Math.abs(QuestInterfaceBlockEntity.this.getStructureSpeed()) < 256.0f) return stack;

            // 检查团队数据
            if (level == null) return stack;
            TeamData data = FTBQuestsAPI.api().getQuestFile(level.isClientSide).getNullableTeamData(teamId);
            if (data == null || !data.canStartTasks(t.getQuest())) {
                return stack;
            }

            // 尝试提交物品到任务
            return itemTask.insert(data, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // 这是一个只进不出的接口
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
        // 允许在服务端获取任务，修复可能的同步问题
        if (t == null && level != null && !level.isClientSide) {
            return t;
        }
        return t;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        // 如果没有特殊逻辑，建议在 Block 类的 getTicker 中移除注册，以提升 TPS
    }

    // [优化] 2. 直接返回缓存的 itemHandler 对象
    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }

    // --- 结构检测逻辑 ---

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

                    int v = 1 - row;
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

    // --- GUI 配置相关 ---

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

    // --- 速度检测逻辑 (获取结构中最大的输入速度) ---

    public float getStructureSpeed() {
        if (level == null) return 0;
        BlockState state = level.getBlockState(this.getBlockPos());
        if (!state.hasProperty(com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.FACING)) return 0;

        net.minecraft.core.Direction facing = state.getValue(com.gugucraft.guguaddons.block.custom.QuestInterfaceBlock.FACING);
        net.minecraft.core.Direction backwards = facing.getOpposite();
        net.minecraft.core.Direction left = facing.getClockWise();
        net.minecraft.core.Direction up = net.minecraft.core.Direction.UP;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        float maxSpeed = 0.0f; // 用于存储最大速度

        for (int d = 0; d < PATTERN.length; d++) {
            String[] layer = PATTERN[d];
            for (int row = 0; row < layer.length; row++) {
                String rowStr = layer[row];
                for (int col = 0; col < rowStr.length(); col++) {
                    char key = rowStr.charAt(col);
                    if (key != 'S') continue; // 只检查 'S' 位置

                    int v = 1 - row;
                    int h = 1 - col;

                    mutablePos.set(this.getBlockPos());
                    mutablePos.move(backwards, d);
                    mutablePos.move(left, h);
                    mutablePos.move(up, v);

                    BlockEntity be = level.getBlockEntity(mutablePos);

                    if (be instanceof com.gugucraft.guguaddons.block.entity.QuestInputBlockEntity inputBE) {
                        float speed = inputBE.getSpeed();
                        // 记录绝对值最大的速度
                        if (Math.abs(speed) > Math.abs(maxSpeed)) {
                            maxSpeed = speed;
                        }
                    }
                }
            }
        }
        return maxSpeed;
    }
}