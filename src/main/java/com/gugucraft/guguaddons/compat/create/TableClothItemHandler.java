package com.gugucraft.guguaddons.compat.create;

import com.simibubi.create.content.logistics.tableCloth.TableClothBlock;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Item-handler capability for Create's table cloth (create:*_table_cloth).
 *
 * <p>Create itself never registers an {@code IItemHandler} on the table cloth, so
 * hoppers / Create logistics cannot push items onto it -- only manual right-click
 * placement works. This handler exposes the cloth's {@code manuallyAddedItems}
 * display list (max 4, one item per slot) as a 4-slot inventory so automation can
 * stock and drain it, matching the behaviour the 1.20.1 example pack achieved via
 * PowerfulJS capability attachment (which is incompatible with current Create).
 *
 * <p>The handler is stateless: it re-resolves the block entity on every call, so it
 * stays correct across NeoForge capability-cache invalidations and block-entity
 * recreation. It is registered at the BLOCK level (see
 * {@code ModCapabilities#registerCapabilities}) so that inserting into an EMPTY
 * cloth -- which has no block entity yet ({@code HAS_BE == false}) -- can force the
 * block entity into existence on the first (non-simulated) insert.
 */
public class TableClothItemHandler implements IItemHandler {

    public static final int SLOTS = 4;

    private final Level level;
    private final BlockPos pos;

    public TableClothItemHandler(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    /** Current block entity, or null if the cloth is empty (no BE yet). */
    @Nullable
    private TableClothBlockEntity be() {
        if (!(level.getBlockEntity(pos) instanceof TableClothBlockEntity cloth))
            return null;
        return cloth;
    }

    @Override
    public int getSlots() {
        return SLOTS;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= SLOTS)
            return ItemStack.EMPTY;
        TableClothBlockEntity be = be();
        if (be == null || be.isShop())
            return ItemStack.EMPTY;
        if (slot >= be.manuallyAddedItems.size())
            return ItemStack.EMPTY;
        return be.manuallyAddedItems.get(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= SLOTS)
            return stack;

        TableClothBlockEntity be = be();

        // Shop cloths are Create stock-keeper displays -- never let automation touch them.
        if (be != null && be.isShop())
            return stack;

        // Each cloth slot holds exactly one item. Only allow sequential filling:
        // slot must equal the current number of filled slots to ensure simulate
        // and commit phases see the same "available slot".
        int filled = be == null ? 0 : be.manuallyAddedItems.size();

        // Only accept insertion into the next sequential empty slot
        if (slot != filled)
            return stack;

        if (filled >= SLOTS)
            return stack;

        if (simulate) {
            // Simulate: this specific slot can accept exactly 1 item
            return stack.copyWithCount(stack.getCount() - 1);
        }

        // Mutating the world (and force-creating the BE) must only happen server-side;
        // capability-driven logistics run there, mirroring Create's own guards.
        if (level.isClientSide())
            return stack;

        // Non-simulated: materialise the block entity if the cloth is still empty,
        // then append one item to the display list and sync.
        if (be == null) {
            be = createBlockEntity();
            if (be == null)
                return stack;
        }

        be.manuallyAddedItems.add(stack.copyWithCount(1));
        be.notifyUpdate();
        return stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot < 0 || slot >= SLOTS)
            return ItemStack.EMPTY;

        TableClothBlockEntity be = be();
        if (be == null || be.isShop())
            return ItemStack.EMPTY;
        if (slot >= be.manuallyAddedItems.size())
            return ItemStack.EMPTY;

        ItemStack present = be.manuallyAddedItems.get(slot);
        if (present.isEmpty())
            return ItemStack.EMPTY;

        if (simulate)
            return present.copyWithCount(1);

        if (level.isClientSide())
            return ItemStack.EMPTY;

        // To maintain slot stability for arrival detection, only allow extraction
        // from the last filled slot. This prevents the list from compacting and
        // shifting indices, which would corrupt the before/after inventory diff
        // used by PackagerBlockEntity.submitNewArrivals.
        int lastFilledSlot = be.manuallyAddedItems.size() - 1;
        if (slot != lastFilledSlot) {
            // Middle slot extraction would shift subsequent items forward, breaking
            // slot position assumptions. Reject extraction from non-tail slots.
            return ItemStack.EMPTY;
        }

        ItemStack removed = be.manuallyAddedItems.remove(slot).copyWithCount(1);
        be.notifyUpdate();
        return removed;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }

    /**
     * Flip the cloth's {@code HAS_BE} state so Create creates its block entity, then
     * return it. Mirrors what {@code TableClothBlock#useItemOn} does on first manual
     * placement. Returns null if the block at {@code pos} is somehow not a cloth.
     */
    @Nullable
    private TableClothBlockEntity createBlockEntity() {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TableClothBlock))
            return null;
        if (!state.getValue(TableClothBlock.HAS_BE))
            level.setBlock(pos, state.setValue(TableClothBlock.HAS_BE, true), Block.UPDATE_ALL);
        return level.getBlockEntity(pos) instanceof TableClothBlockEntity cloth ? cloth : null;
    }
}
