package com.gugucraft.guguaddons.compat.create;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Custom unpacking handler for Create's table cloth blocks.
 *
 * <p>Table cloths have a strict 4-slot, 1-item-per-slot capacity. This handler ensures
 * that packages are only unpacked if all items fit, preventing item voiding and promise
 * inflation that occurs when the default handler tries to force oversized packages into
 * the cloth.
 */
public enum TableClothUnpackingHandler implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos pos, BlockState state, Direction side,
                         List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext,
                         boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(pos);
        if (targetBE == null)
            return false;

        IItemHandler targetInv = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, targetBE, side);
        if (targetInv == null)
            return false;

        // Cloth constraint: max 4 items, 1 per slot
        if (!(targetInv instanceof TableClothItemHandler)) {
            // Fallback to default if somehow not our handler
            return false;
        }

        // Count non-empty items in the package
        int itemCount = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                itemCount += stack.getCount();
            }
        }

        // Count current filled slots
        int currentFilled = 0;
        for (int slot = 0; slot < targetInv.getSlots(); slot++) {
            if (!targetInv.getStackInSlot(slot).isEmpty())
                currentFilled++;
        }

        int availableSlots = targetInv.getSlots() - currentFilled;

        // Reject if package has more items than available slots
        if (itemCount > availableSlots) {
            return false;
        }

        // Try to insert each item
        if (simulate) {
            // Simulate: check if all items can fit
            int slotIndex = currentFilled;
            for (ItemStack stack : items) {
                if (stack.isEmpty())
                    continue;

                for (int i = 0; i < stack.getCount(); i++) {
                    if (slotIndex >= targetInv.getSlots())
                        return false;

                    ItemStack remainder = targetInv.insertItem(slotIndex, stack.copyWithCount(1), true);
                    if (!remainder.isEmpty())
                        return false;

                    slotIndex++;
                }
            }
            return true;
        } else {
            // Commit: actually insert
            int slotIndex = currentFilled;
            for (ItemStack stack : items) {
                if (stack.isEmpty())
                    continue;

                for (int i = 0; i < stack.getCount(); i++) {
                    if (slotIndex >= targetInv.getSlots())
                        return false;

                    ItemStack remainder = targetInv.insertItem(slotIndex, stack.copyWithCount(1), false);
                    if (!remainder.isEmpty())
                        return false;

                    slotIndex++;
                }
            }

            // Clear the items list to signal everything was consumed
            items.clear();
            return true;
        }
    }
}
