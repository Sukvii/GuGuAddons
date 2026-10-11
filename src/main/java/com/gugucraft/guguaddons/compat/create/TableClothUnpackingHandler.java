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
        IItemHandler targetInv = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, targetBE, side);
        if (!(targetInv instanceof TableClothItemHandler clothHandler))
            return false;

        boolean inserted = clothHandler.insertAll(items, simulate);
        if (inserted && !simulate)
            items.clear();
        return inserted;
    }
}
