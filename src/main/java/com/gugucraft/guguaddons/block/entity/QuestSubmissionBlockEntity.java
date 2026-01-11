package com.gugucraft.guguaddons.block.entity;

import com.gugucraft.guguaddons.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public class QuestSubmissionBlockEntity extends BlockEntity {

    private long lastSubmissionTime = 0;
    private static final int ITEMS_PER_SECOND = 1;
    private static final int COOLDOWN_TICKS = 20 / ITEMS_PER_SECOND;

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
            if (level.getGameTime() - lastSubmissionTime < COOLDOWN_TICKS) {
                return stack;
            }

            BlockPos myPos = getBlockPos();

            // Scan for the controller
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        BlockPos target = myPos.offset(x, y, z);
                        BlockEntity be = level.getBlockEntity(target);
                        if (be instanceof QuestInterfaceBlockEntity interfaceBe) {
                            if (interfaceBe.isStructureFormed() && interfaceBe.isBlockInStructure(myPos)) {

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
                        }
                    }
                }
            }
            return stack;
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

    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }
}
