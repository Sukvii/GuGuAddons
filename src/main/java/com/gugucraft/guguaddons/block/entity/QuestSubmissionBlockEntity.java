package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.registry.ModBlockEntities;
import com.gugucraft.guguaddons.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuestSubmissionBlockEntity extends BlockEntity {

    private long lastSubmissionTime = 0;
    private int processingSpeed = 1;
    private BlockPos cachedControllerPos = null;

    private int getCooldown() {
        return Math.max(1, 20 / processingSpeed);
    }

    private final IItemHandler itemHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (level == null || stack.isEmpty())
                return stack;

            // Check cooldown
            if (level.getGameTime() - lastSubmissionTime < getCooldown()) {
                return stack;
            }

            BlockPos myPos = getBlockPos();

            // 1. Try Cached Controller
            if (cachedControllerPos != null) {
                BlockEntity be = level.getBlockEntity(cachedControllerPos);
                if (be instanceof QuestInterfaceBlockEntity interfaceBe) {
                    if (interfaceBe.isStructureFormed() && interfaceBe.isBlockInStructure(myPos)) {
                        return trySubmitTo(interfaceBe, stack, simulate);
                    }
                }
                // Cache invalid
                cachedControllerPos = null;
            }

            // 2. Scan for the controller
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        BlockPos target = myPos.offset(x, y, z);
                        BlockEntity be = level.getBlockEntity(target);
                        if (be instanceof QuestInterfaceBlockEntity interfaceBe) {
                            if (interfaceBe.isStructureFormed() && interfaceBe.isBlockInStructure(myPos)) {
                                cachedControllerPos = target;
                                return trySubmitTo(interfaceBe, stack, simulate);
                            }
                        }
                    }
                }
            }
            return stack;
        }

        private ItemStack trySubmitTo(QuestInterfaceBlockEntity interfaceBe, ItemStack stack, boolean simulate) {
            // To enforce strict rate, we try to insert EXACTLY ONE item
            ItemStack singleItem = stack.copy();
            singleItem.setCount(1);

            ItemStack remainderSingle = interfaceBe.submitItem(singleItem, simulate);

            // If the single item was accepted (remainder is empty)
            if (remainderSingle.isEmpty()) {
                if (!simulate) {
                    lastSubmissionTime = level.getGameTime();
                    ItemStack result = stack.copy();
                    result.shrink(1);
                    return result;
                }
                // For simulation, we just indicate 1 item fewer
                ItemStack result = stack.copy();
                result.shrink(1);
                return result;
            } else {
                // The item was rejected
                return stack;
            }
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    };

    public QuestSubmissionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUEST_SUBMISSION.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ProcessingSpeed", processingSpeed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ProcessingSpeed")) {
            processingSpeed = tag.getInt("ProcessingSpeed");
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public int getProcessingSpeed() {
        return processingSpeed;
    }

    public boolean tryUpgrade(ItemStack stack, Player player) {
        Item item = stack.getItem();
        int newSpeed = -1;

        if (item == ModItems.INTERFACE_UPGRADE_1.get()) {
            if (processingSpeed == 1)
                newSpeed = 4;
            else if (processingSpeed >= 4) {
                player.displayClientMessage(
                        Component.translatable("message.guguaddons.submission_upgrade_already_applied"), true);
                return false;
            }
        } else if (item == ModItems.INTERFACE_UPGRADE_2.get()) {
            if (processingSpeed == 4)
                newSpeed = 8;
            else if (processingSpeed < 4) {
                player.displayClientMessage(
                        Component.translatable("message.guguaddons.submission_upgrade_previous_tier_required"), true);
                return false;
            } else if (processingSpeed >= 8) {
                player.displayClientMessage(
                        Component.translatable("message.guguaddons.submission_upgrade_already_applied"), true);
                return false;
            }
        } else if (item == ModItems.INTERFACE_UPGRADE_3.get()) {
            if (processingSpeed == 8)
                newSpeed = 16;
            else if (processingSpeed < 8) {
                player.displayClientMessage(
                        Component.translatable("message.guguaddons.submission_upgrade_previous_tier_required"), true);
                return false;
            } else if (processingSpeed >= 16) {
                player.displayClientMessage(
                        Component.translatable("message.guguaddons.submission_upgrade_already_applied"), true);
                return false;
            }
        }

        if (newSpeed != -1) {
            processingSpeed = newSpeed;
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            player.displayClientMessage(
                    Component.translatable("message.guguaddons.submission_upgrade_success", newSpeed), true);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            return true;
        }

        return false;
    }

    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }
}
